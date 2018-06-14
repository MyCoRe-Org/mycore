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
package org.mycore.common.content.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRContent;

/**
 * @author Thomas Scheffler (yagee)
 * @author Matthias Eichner
 */
public abstract class MCRServletContentHelper {

    private static Logger LOGGER = LogManager.getLogger(MCRServletContentHelper.class);

    public static final int DEFAULT_BUFFER_SIZE = ContentUtils.DEFAULT_BUFFER_SIZE;

    public static final String ATT_SERVE_CONTENT = MCRServletContentHelper.class.getName() + ".serveContent";

    public static class Config {

        public boolean useAcceptRanges = true;

        public int inputBufferSize = ContentUtils.DEFAULT_BUFFER_SIZE;

        public int outputBufferSize = ContentUtils.DEFAULT_BUFFER_SIZE;

    }

    public static Config buildConfig(ServletConfig servletConfig) {
        Config config = new Config();

        if (servletConfig.getInitParameter("inputBufferSize") != null) {
            config.inputBufferSize = Integer.parseInt(servletConfig.getInitParameter("inputBufferSize"));
        }

        if (servletConfig.getInitParameter("outputBufferSize") != null) {
            config.outputBufferSize = Integer.parseInt(servletConfig.getInitParameter("outputBufferSize"));
        }

        if (servletConfig.getInitParameter("useAcceptRanges") != null) {
            config.useAcceptRanges = Boolean.parseBoolean(servletConfig.getInitParameter("useAcceptRanges"));
        }

        if (config.inputBufferSize < ContentUtils.MIN_BUFFER_SIZE) {
            config.inputBufferSize = ContentUtils.MIN_BUFFER_SIZE;
        }
        if (config.outputBufferSize < ContentUtils.MIN_BUFFER_SIZE) {
            config.outputBufferSize = ContentUtils.MIN_BUFFER_SIZE;
        }
        return config;
    }

    public static boolean isServeContent(final HttpServletRequest request) {
        return request.getAttribute(ATT_SERVE_CONTENT) != Boolean.FALSE;
    }

    /**
     * Serve the specified content, optionally including the data content.
     * This method handles both GET and HEAD requests.
     */
    public static void serveContent(final MCRContent content, final HttpServletRequest request,
        final HttpServletResponse response, final ServletContext context) throws IOException {
        serveContent(content, request, response, context, new Config(), isServeContent(request));
    }

    /**
     * Serve the specified content, optionally including the data content.
     * This method handles both GET and HEAD requests.
     */
    public static void serveContent(final MCRContent content, final HttpServletRequest request,
        final HttpServletResponse response, final ServletContext context, final Config config,
        final boolean withContent)
        throws IOException {

        boolean serveContent = withContent;

        final String path = getRequestPath(request);
        if (LOGGER.isDebugEnabled()) {
            if (serveContent) {
                LOGGER.debug("Serving '{}' headers and data", path);
            } else {
                LOGGER.debug("Serving '{}' headers only", path);
            }
        }

        if (response.isCommitted()) {
            //getContent has access to response
            return;
        }

        final boolean isError = response.getStatus() >= HttpServletResponse.SC_BAD_REQUEST;

        if (content == null && !isError) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, request.getRequestURI());
            return;
        }

        //Check if all conditional header validate
        if (!isError && !checkIfHeaders(request, response, content)) {
            return;
        }

        // Find content type.
        String contentType = content.getMimeType();
        final String filename = getFileName(request, content);
        if (contentType == null) {
            contentType = context.getMimeType(filename);
            content.setMimeType(contentType);
        }
        String enc = content.getEncoding();
        if (enc != null) {
            contentType = String.format(Locale.ROOT, "%s; charset=%s", contentType, enc);
        }

        String eTag = null;
        List<Range> ranges = null;
        if (!isError) {
            eTag = content.getETag();
            if (config.useAcceptRanges) {
                response.setHeader("Accept-Ranges", "bytes");
            }

            ranges = parseRange(request, response, content);

            response.setHeader("ETag", eTag);

            long lastModified = content.lastModified();
            if (lastModified >= 0) {
                response.setDateHeader("Last-Modified", lastModified);
            }
            if (serveContent) {
                String dispositionType = request.getParameter("dl") == null ? "inline" : "attachment";
                response.setHeader("Content-Disposition", dispositionType + ";filename=\"" + filename + "\"");
            }
        }

        final long contentLength = content.length();
        //No Content to serve?
        if (contentLength == 0) {
            serveContent = false;
        }

        if (content.isUsingSession()) {
            response.addHeader("Cache-Control", "private, max-age=0, must-revalidate");
            response.addHeader("Vary", "*");
        }

        try (ServletOutputStream out = serveContent ? response.getOutputStream() : null) {
            if (serveContent) {
                try {
                    response.setBufferSize(config.outputBufferSize);
                } catch (final IllegalStateException e) {
                    //does not matter if we fail
                }
            }
            if (response instanceof ServletResponseWrapper) {
                if (request.getHeader("Range") != null) {
                    LOGGER.warn("Response is wrapped by ServletResponseWrapper, no 'Range' requests supported.");
                }
                ranges = ContentUtils.FULL;
            }

            if (isError || (ranges == null || ranges.isEmpty()) && request.getHeader("Range") == null
                || ranges == ContentUtils.FULL) {
                //No ranges
                if (contentType != null) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("contentType='{}'", contentType);
                    }
                    response.setContentType(contentType);
                }
                if (contentLength >= 0) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("contentLength={}", contentLength);
                    }
                    setContentLengthLong(response, contentLength);
                }

                if (serveContent) {
                    ContentUtils.copy(content, out, config.inputBufferSize, config.outputBufferSize);
                }

            } else {

                if (ranges == null || ranges.isEmpty()) {
                    return;
                }

                // Partial content response.

                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

                if (ranges.size() == 1) {

                    final Range range = ranges.get(0);
                    response.addHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + range.length);
                    final long length = range.end - range.start + 1;
                    setContentLengthLong(response, length);

                    if (contentType != null) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("contentType='{}'", contentType);
                        }
                        response.setContentType(contentType);
                    }

                    if (serveContent) {
                        ContentUtils.copy(content, out, range, config.inputBufferSize, config.outputBufferSize);
                    }

                } else {

                    response.setContentType("multipart/byteranges; boundary=" + ContentUtils.MIME_BOUNDARY);

                    if (serveContent) {
                        ContentUtils.copy(content, out, ranges.iterator(), contentType, config.inputBufferSize,
                            config.outputBufferSize);
                    }
                }
            }
        }
    }

    /**
     * Check if all conditions specified in the If headers are
     * satisfied.
     */
    private static boolean checkIfHeaders(final HttpServletRequest request, final HttpServletResponse response,
        final MCRContent content) throws IOException {

        return checkIfMatch(request, response, content) && checkIfModifiedSince(request, response, content)
            && checkIfNoneMatch(request, response, content) && checkIfUnmodifiedSince(request, response, content);

    }

    /**
     * Check if the If-Match condition is satisfied.
     */
    private static boolean checkIfMatch(final HttpServletRequest request, final HttpServletResponse response,
        final MCRContent content) throws IOException {

        final String eTag = content.getETag();
        final String headerValue = request.getHeader("If-Match");
        if (headerValue != null) {
            if (headerValue.indexOf('*') == -1) {

                final StringTokenizer commaTokenizer = new StringTokenizer(headerValue, ",");
                boolean conditionSatisfied = false;

                while (!conditionSatisfied && commaTokenizer.hasMoreTokens()) {
                    final String currentToken = commaTokenizer.nextToken();
                    if (currentToken.trim().equals(eTag)) {
                        conditionSatisfied = true;
                    }
                }

                // none of the given ETags match
                if (!conditionSatisfied) {
                    response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
                    return false;
                }

            }
        }
        return true;
    }

    /**
     * Check if the If-Modified-Since condition is satisfied.
     *
     */
    private static boolean checkIfModifiedSince(final HttpServletRequest request, final HttpServletResponse response,
        final MCRContent content) throws IOException {
        try {
            final long headerValue = request.getDateHeader("If-Modified-Since");
            final long lastModified = content.lastModified();
            if (headerValue != -1) {

                // If an If-None-Match header has been specified, if modified since
                // is ignored.
                if (request.getHeader("If-None-Match") == null && lastModified < headerValue + 1000) {
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    response.setHeader("ETag", content.getETag());

                    return false;
                }
            }
        } catch (final IllegalArgumentException illegalArgument) {
            return true;
        }
        return true;
    }

    /**
     * Check if the if-none-match condition is satisfied.
     */
    private static boolean checkIfNoneMatch(final HttpServletRequest request, final HttpServletResponse response,
        final MCRContent content) throws IOException {

        final String eTag = content.getETag();
        final String headerValue = request.getHeader("If-None-Match");
        if (headerValue != null) {

            boolean conditionSatisfied = false;

            if ("*".equals(headerValue)) {
                conditionSatisfied = true;
            } else {
                final StringTokenizer commaTokenizer = new StringTokenizer(headerValue, ",");
                while (!conditionSatisfied && commaTokenizer.hasMoreTokens()) {
                    final String currentToken = commaTokenizer.nextToken();
                    if (currentToken.trim().equals(eTag)) {
                        conditionSatisfied = true;
                    }
                }

            }

            if (conditionSatisfied) {
                //'GET' 'HEAD' -> not modified
                if ("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod())) {
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    response.setHeader("ETag", eTag);

                    return false;
                }
                response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the if-unmodified-since condition is satisfied.
     */
    private static boolean checkIfUnmodifiedSince(final HttpServletRequest request, final HttpServletResponse response,
        final MCRContent resource) throws IOException {
        try {
            final long lastModified = resource.lastModified();
            final long headerValue = request.getDateHeader("If-Unmodified-Since");
            if (headerValue != -1) {
                if (lastModified >= headerValue + 1000) {
                    // The content has been modified.
                    response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
                    return false;
                }
            }
        } catch (final IllegalArgumentException illegalArgument) {
            return true;
        }
        return true;
    }

    public static long copyLarge(InputStream input, OutputStream output, long inputOffset, long length, byte[] buffer)
        throws IOException {
        return ContentUtils.copyLarge(input, output, inputOffset, length, buffer);
    }

    private static String extractFileName(String filename) {
        int filePosition = filename.lastIndexOf('/') + 1;
        filename = filename.substring(filePosition);
        filePosition = filename.lastIndexOf('.');
        if (filePosition > 0) {
            filename = filename.substring(0, filePosition);
        }
        return filename;
    }

    private static String getFileName(final HttpServletRequest req, final MCRContent content) {
        final String filename = content.getName();
        if (filename != null) {
            return filename;
        }
        if (req.getPathInfo() != null) {
            return extractFileName(req.getPathInfo());
        }
        return MessageFormat.format("{0}-{1}", extractFileName(req.getServletPath()), System.currentTimeMillis());
    }

    /**
     * Returns the request path for debugging purposes.
     */
    private static String getRequestPath(final HttpServletRequest request) {
        return request.getServletPath() + (request.getPathInfo() == null ? "" : request.getPathInfo());
    }

    /**
     * Parses and validates the range header.
     * This method ensures that all ranges are in ascending order and non-overlapping, so we can use a single
     * InputStream.
     */
    private static List<Range> parseRange(final HttpServletRequest request, final HttpServletResponse response,
        final MCRContent content) throws IOException {

        // Checking if range is still valid (lastModified)
        final String headerValue = request.getHeader("If-Range");

        if (headerValue != null) {
            long headerValueTime = -1L;
            try {
                headerValueTime = request.getDateHeader("If-Range");
            } catch (final IllegalArgumentException e) {
                // Ignore
            }

            final String eTag = content.getETag();
            final long lastModified = content.lastModified();

            if (headerValueTime == -1L) {
                // If the content changed, the complete content is served.
                if (!eTag.equals(headerValue.trim())) {
                    return ContentUtils.FULL;
                }
            } else {
                //add one second buffer to check if the content was modified.
                if (lastModified > headerValueTime + 1000) {
                    return ContentUtils.FULL;
                }
            }

        }

        final long fileLength = content.length();
        if (fileLength <= 0) {
            return null;
        }

        String rangeHeader = request.getHeader("Range");
        try {
            return Range.parseRanges(rangeHeader, fileLength);
        } catch (IllegalArgumentException e) {
            response.addHeader("Content-Range", "bytes */" + fileLength);
            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            return null;
        }
    }

    private static void setContentLengthLong(final HttpServletResponse response, final long length) {
        response.setHeader("Content-Length", String.valueOf(length));
    }

}
