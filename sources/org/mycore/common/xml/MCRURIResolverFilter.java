/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.common.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.log4j.Logger;

import org.mycore.common.MCRSession;
import org.mycore.frontend.servlets.MCRServlet;

/**
 * Servlet Filter for adding debug information to servlet output.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRURIResolverFilter implements Filter {
    private static final Logger LOGGER = Logger.getLogger(MCRURIResolver.class);

    /**
     * adds debug information from MCRURIResolver to Servlet output.
     * 
     * The information includes all URIs resolved by MCRURIResolver by the
     * current request. The filter is triggered by the log4j statement of the
     * MCRURIResolver. To switch it on set the logger to DEBUG level.
     * 
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        MCRSession currentSession=null;
        List list = null;
        /*
         * isDebugEnabled() may return a different value
         * when called a second time. Since we initialize things in the
         * first block, we need to make sure to visit the second block only if we
         * visited the first block, too.
         */
        final boolean debugEnabled = LOGGER.isDebugEnabled();
        if (debugEnabled) {
            /*
             * we cannot call MCRSessionMgr.getCurrentSession() the current
             * Session is gathered by MCRServlet that at this point was not
             * called
             */
            currentSession = MCRServlet.getSession((HttpServletRequest) request);
            LOGGER.debug("start filter in session " + currentSession.getID());
            // prepare UriResolver debug list
            list = new MyLinkedList();
            currentSession.put(MCRURIResolver.SESSION_OBJECT_NAME, list);
        }
        ServletOutputStream out = response.getOutputStream();
        MyResponseWrapper wrapper = new MyResponseWrapper((HttpServletResponse) response);
        // process request
        filterChain.doFilter(request, wrapper);
        final String origOutput = wrapper.toString();
        final String characterEncoding = wrapper.getCharacterEncoding();
        /**
         * NOTE: jetty doesn't correctly implement
         * ServletOutputStream.print(String). So we must encode it ourself to
         * byte arrays.
         */
        if (debugEnabled && !list.isEmpty() && (origOutput.length() > 0)
                && (-1 != response.getContentType().indexOf("text/html") || -1 != response.getContentType().indexOf("text/xml"))) {
            int pos = getInsertPosition(origOutput);
            out.write(origOutput.substring(0, pos).getBytes(characterEncoding));
            final String insertString = "\n<!-- \n" + list.toString() + "\n-->";
            final byte[] insertBytes = insertString.getBytes(characterEncoding);
            out.write(insertBytes);
            out.write(origOutput.substring(pos, origOutput.length()).getBytes(characterEncoding));
            response.setContentLength(origOutput.getBytes(characterEncoding).length+insertBytes.length);
            // delete debuglist
            currentSession.deleteObject(MCRURIResolver.SESSION_OBJECT_NAME);
            LOGGER.debug("end filter");
        } else {
            out.write(origOutput.getBytes(characterEncoding));
        }
        out.close();
    }

    private int getInsertPosition(final String origOutput) {
        // if html document, before head-tag
        int pos = origOutput.indexOf("<head>");
        if (pos < 0) {
            // for xml output, after <?xml *?>
            pos = origOutput.indexOf("?>") + 2;
        }
        if (pos < 2) {
            // for the rest
            pos = 0;
        }
        return pos;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy() {
        // nothing to be done so far

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig arg0) throws ServletException {
        // no inititalization parameters required so far
    }

    /**
     * LinkedList with overwritten toString() method.
     * 
     * @author Thomas Scheffler (yagee)
     */
    private static class MyLinkedList extends LinkedList {

        private static final long serialVersionUID = -2602420461572432380L;

        public String toString() {
            StringBuffer buf = new StringBuffer("The following includes where resolved by MCRURIResolver:\n\n");
            for (Iterator it = this.iterator(); it.hasNext();) {
                Object obj = it.next();
                buf.append(obj.toString());
                buf.append('\n');
            }
            buf.deleteCharAt(buf.length() - 1);
            return buf.toString();
        }

    }

    /**
     * wrapper arround the servlet response to change the output afterwards.
     * 
     * @author Thomas Scheffler (yagee)
     */
    private static class MyResponseWrapper extends HttpServletResponseWrapper {
        private ByteArrayOutputStream output;

        public String toString() {
            try {
                return output.toString(getCharacterEncoding());
            } catch (UnsupportedEncodingException e) {
                /**
                 * Definitely this Exception should not occur because we get the
                 * character encoding from the HttpResponse object where it was
                 * previously set or it defaults to the system or ISO-8859-1.
                 * Either way for this rare circumstance we return the String in
                 * system default encoding here.
                 */
                LOGGER.error("Fall back to DEFAULT encoding.");
                return output.toString();
            }
        }

        public MyResponseWrapper(HttpServletResponse response) {
            super(response);
            output = new ByteArrayOutputStream(16*1024);
        }

        public PrintWriter getWriter() {
            return new PrintWriter(output);
        }

        public ServletOutputStream getOutputStream() throws IOException {
            return new MyServletOutputStream();
        }
        
        public String getCharacterEncoding() {
            final String encoding=super.getCharacterEncoding();
            LOGGER.debug("Character Encoding: "+encoding);
            return encoding;
        }

        private class MyServletOutputStream extends ServletOutputStream{

            public void print(String arg0) throws IOException {
                output.write(arg0.getBytes(getResponse().getCharacterEncoding()));
            }

            public void write(int b) throws IOException {
                output.write(b);
            }
            
        }
    }
}
