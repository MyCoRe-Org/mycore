/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.common.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Servlet Filter for adding debug information to servlet output.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRURIResolverFilter implements Filter {
    private static final Logger LOGGER = LogManager.getLogger(MCRURIResolver.class);

    static ThreadLocal<List<String>> uriList = ThreadLocal.withInitial(MyLinkedList::new);

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
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException,
        ServletException {
        /*
         * isDebugEnabled() may return a different value when called a second
         * time. Since we initialize things in the first block, we need to make
         * sure to visit the second block only if we visited the first block,
         * too.
         */
        final boolean debugEnabled = LOGGER.isDebugEnabled();
        if (!debugEnabled) {
            //do not filter...
            filterChain.doFilter(request, response);
        } else {
            MyResponseWrapper wrapper = new MyResponseWrapper((HttpServletResponse) response);
            // process request
            filterChain.doFilter(request, wrapper);
            final String origOutput = wrapper.toString();
            final String characterEncoding = wrapper.getCharacterEncoding();
            if (!uriList.get().isEmpty() && origOutput.length() > 0
                && (response.getContentType().contains("text/html")
                    || response.getContentType().contains("text/xml"))) {
                final String insertString = "\n<!-- \n" + uriList.get() + "\n-->";
                final byte[] insertBytes = insertString.getBytes(characterEncoding);
                response.setContentLength(origOutput.getBytes(characterEncoding).length + insertBytes.length);
                int pos = getInsertPosition(origOutput);
                try (ServletOutputStream out = response.getOutputStream()) {
                    out.write(origOutput.substring(0, pos).getBytes(characterEncoding));
                    out.write(insertBytes);
                    out.write(origOutput.substring(pos, origOutput.length()).getBytes(characterEncoding));
                    // delete debuglist
                    uriList.remove();
                    LOGGER.debug("end filter: {}", origOutput.substring(origOutput.length() - 10, origOutput.length()));
                }
            } else {
                LOGGER.debug("Sending original response");
                byte[] byteArray = wrapper.output.toByteArray();
                if (byteArray.length > 0) {
                    try (ServletOutputStream out = response.getOutputStream()) {
                        out.write(byteArray);
                    }
                }
            }
        }
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
    private static class MyLinkedList extends LinkedList<String> {

        private static final long serialVersionUID = -2602420461572432380L;

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder("The following includes where resolved by MCRURIResolver:\n\n");
            for (String obj : this) {
                buf.append(obj);
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

        @Override
        public String toString() {
            try {
                return output.toString(getCharacterEncoding());
            } catch (UnsupportedEncodingException e) {
                LOGGER.error("Fall back to DEFAULT encoding.");
                try {
                    return output.toString(Charset.defaultCharset().name());
                } catch (UnsupportedEncodingException neverThrown) {
                    throw new RuntimeException(neverThrown);
                }
            }
        }

        public MyResponseWrapper(HttpServletResponse response) {
            super(response);
            output = new ByteArrayOutputStream(16 * 1024);
        }

        @Override
        public PrintWriter getWriter() {
            return new PrintWriter(new OutputStreamWriter(output, Charset.forName(getCharacterEncoding())));
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return new MyServletOutputStream();
        }

        @Override
        public String getCharacterEncoding() {
            final String encoding = super.getCharacterEncoding();
            LOGGER.debug("Character Encoding: {}", encoding);
            return encoding;
        }

        private class MyServletOutputStream extends ServletOutputStream {

            @Override
            public void print(String arg0) throws IOException {
                output.write(arg0.getBytes(getResponse().getCharacterEncoding()));
            }

            @Override
            public void write(int b) throws IOException {
                output.write(b);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                //isReady() is always ready
            }

        }
    }
}
