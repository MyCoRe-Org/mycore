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
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

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
        final MCRSession currentSession = MCRSessionMgr.getCurrentSession();
        List list = null;
        if (LOGGER.isDebugEnabled()) {
            // prepare UriResolver debug list
            LOGGER.debug("start filter");
            list = new MyLinkedList();
            currentSession.put(MCRURIResolver.SESSION_OBJECT_NAME, list);
        }
        ServletOutputStream out = response.getOutputStream();
        MyResponseWrapper wrapper = new MyResponseWrapper((HttpServletResponse) response);
        // process request
        filterChain.doFilter(request, wrapper);
        final String origOutput = wrapper.toString();
        if (LOGGER.isDebugEnabled() && !list.isEmpty() && (-1 != response.getContentType().indexOf("text/html") || -1 != response.getContentType().indexOf("text/xml"))) {
            int pos = getInsertPosition(origOutput);
            out.print(origOutput.substring(0, pos));
            final String insertString = "\n<!-- \n" + list.toString() + "\n-->";
            out.print(insertString);
            out.print(origOutput.substring(pos, origOutput.length()));
            response.setContentLength(origOutput.length()+insertString.length());
            // delete debuglist
            currentSession.deleteObject(MCRURIResolver.SESSION_OBJECT_NAME);
            LOGGER.debug("end filter");
        } else {
            out.print(origOutput);
        }
        out.close();
    }

    private int getInsertPosition(final String origOutput) {
        // if html document, before head-tag
        int pos = origOutput.indexOf("<head>") - 1;
        if (pos < 0) {
            // for xml output, after <?xml *?>
            pos = origOutput.indexOf("?>") + 2;
        }
        if (pos < 0) {
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
    private class MyLinkedList extends LinkedList {

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
    private class MyResponseWrapper extends HttpServletResponseWrapper {
        private ByteArrayOutputStream output;

        public String toString() {
            return output.toString();
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
