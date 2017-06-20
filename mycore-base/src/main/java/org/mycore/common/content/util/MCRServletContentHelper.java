/*
 * $Id$
 * $Revision: 5697 $ $Date: Feb 28, 2014 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */
package org.mycore.common.content.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRSeekableChannelContent;
import org.mycore.common.content.MCRVFSContent;

/**
 * @author Thomas Scheffler (yagee)
 * @author Matthias Eichner
 */
public abstract class MCRServletContentHelper {

    private static Logger LOGGER = LogManager.getLogger(MCRServletContentHelper.class);

    private static final ArrayList<Range> FULL = new ArrayList<>();

    private static final String MIME_BOUNDARY = "MYCORE_MIME_BOUNDARY";

    public static final String ATT_SERVE_CONTENT = MCRServletContentHelper.class.getName() + ".serveContent";

    public static final int DEFAULT_BUFFER_SIZE = 65536;

    public static final int MIN_BUFFER_SIZE = 512;

    public static class Config {

        public boolean useAcceptRanges = true;

        public int inputBufferSize = DEFAULT_BUFFER_SIZE;

        public int outputBufferSize = DEFAULT_BUFFER_SIZE;

    }

    protected static class Range {

        public long start;

        public long end;

        public long length;

        public boolean validate() {
            if (end >= length) {
                //set 'end' to content size
                end = length - 1;
            }
            return start >= 0 && end >= 0 && start <= end && length > 0;
        }
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

        if (config.inputBufferSize < MIN_BUFFER_SIZE) {
            config.inputBufferSize = MIN_BUFFER_SIZE;
        }
        if (config.outputBufferSize < MIN_BUFFER_SIZE) {
            config.outputBufferSize = MIN_BUFFER_SIZE;
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
                LOGGER.debug("Serving '" + path + "' headers and data");
            } else {
                LOGGER.debug("Serving '" + path + "' headers only");
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
        ArrayList<Range> ranges = null;
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
                ranges = FULL;
            }

            if (isError || (ranges == null || ranges.isEmpty()) && request.getHeader("Range") == null
                || ranges == FULL) {
                //No ranges
                if (contentType != null) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("contentType='" + contentType + "'");
                    }
                    response.setContentType(contentType);
                }
                if (contentLength >= 0) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("contentLength=" + contentLength);
                    }
                    setContentLengthLong(response, contentLength);
                }

                if (serveContent) {
                    copy(content, out, config.inputBufferSize, config.outputBufferSize);
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
                            LOGGER.debug("contentType='" + contentType + "'");
                        }
                        response.setContentType(contentType);
                    }

                    if (serveContent) {
                        copy(content, out, range, config.inputBufferSize, config.outputBufferSize);
                    }

                } else {

                    response.setContentType("multipart/byteranges; boundary=" + MIME_BOUNDARY);

                    if (serveContent) {
                        copy(content, out, ranges.iterator(), contentType, config.inputBufferSize,
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

    private static long copyChannel(final ReadableByteChannel src, final WritableByteChannel dest, final int bufferSize)
        throws IOException {
        if (src instanceof FileChannel) {
            return copyFileChannel((FileChannel) src, dest, bufferSize);
        }
        long bytes = 0;
        final ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        while (src.read(buffer) != -1) {
            // prepare the buffer to be drained
            buffer.flip();

            // write to the channel, may block
            bytes += dest.write(buffer);
            // If partial transfer, shift remainder down
            // If buffer is empty, same as doing clear()
            buffer.compact();
        }
        // EOF will leave buffer in fill state
        buffer.flip();
        // make sure the buffer is fully drained.
        while (buffer.hasRemaining()) {
            bytes += dest.write(buffer);
        }
        return bytes;
    }

    private static long copyFileChannel(final FileChannel src, final WritableByteChannel dest, final int bufferSize)
        throws IOException {
        long bytes = 0L;
        long time = -System.currentTimeMillis();
        long size = src.size();
        while (bytes < size) {
            long bytesToTransfer = Math.min(bufferSize, size - bytes);
            long bytesTransfered = src.transferTo(bytes, bytesToTransfer, dest);

            bytes += bytesTransfered;

            if (LOGGER.isDebugEnabled()) {
                long percentage = Math.round(bytes / ((double) size) * 100.0);
                LOGGER.debug("overall bytes transfered: " + bytes + " progress " + percentage + "%");
            }

        }

        if (LOGGER.isDebugEnabled()) {
            time += System.currentTimeMillis();
            double kBps = (bytes / 1024.0) / (time / 1000.0);
            LOGGER.debug("Transfered: " + bytes + " bytes in: " + (time / 1000.0) + " s -> " + kBps + " kbytes/s");
        }
        return bytes;
    }

    /**
     * Consumes the content and writes it to the ServletOutputStream.
     */
    private static void copy(final MCRContent content, final ServletOutputStream out, final int inputBufferSize,
        final int outputBufferSize) throws IOException {
        final long bytesCopied;
        long length = content.length();
        if (content instanceof MCRSeekableChannelContent) {
            try (SeekableByteChannel byteChannel = ((MCRSeekableChannelContent) content).getSeekableByteChannel();
                WritableByteChannel nout = Channels.newChannel(out)) {
                endCurrentTransaction();
                bytesCopied = copyChannel(byteChannel, nout, outputBufferSize);
            }
        } else {
            try (InputStream contentIS = content.getInputStream();
                final InputStream in = isInputStreamBuffered(contentIS, content) ? contentIS : new BufferedInputStream(
                    contentIS, inputBufferSize);) {
                endCurrentTransaction();
                // Copy the inputBufferSize stream to the outputBufferSize stream
                bytesCopied = IOUtils.copyLarge(in, out, new byte[outputBufferSize]);
            }
        }
        if (length >= 0 && length != bytesCopied) {
            throw new EOFException("Bytes to send: " + length + " actual: " + bytesCopied);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Wrote " + bytesCopied + " bytes.");
        }
    }

    /**
     * Consumes the content and writes it to the ServletOutputStream.
     *
     * @param content      The MCRContent resource to serve
     * @param out       The outputBufferSize stream to write to
     * @param ranges        Enumeration of the ranges in ascending non overlapping order the client wanted to retrieve
     * @param contentType   Content type of the resource
     */
    private static void copy(final MCRContent content, final ServletOutputStream out, final Iterator<Range> ranges,
        final String contentType, final int inputBufferSize, final int outputBufferSize) throws IOException {

        IOException exception = null;

        long lastByte = 0;
        try (final InputStream resourceInputStream = content.getInputStream();
            final InputStream in = isInputStreamBuffered(resourceInputStream, content) ? resourceInputStream
                : new BufferedInputStream(resourceInputStream, inputBufferSize)) {
            endCurrentTransaction();
            while (exception == null && ranges.hasNext()) {

                final Range currentRange = ranges.next();

                // Writing MIME header.
                out.println();
                out.println("--" + MIME_BOUNDARY);
                if (contentType != null) {
                    out.println("Content-Type: " + contentType);
                }
                out.println("Content-Range: bytes " + currentRange.start + "-" + currentRange.end + "/"
                    + currentRange.length);
                out.println();

                // Printing content
                exception = copyRange(in, out, lastByte, currentRange.start, currentRange.end, outputBufferSize);
                lastByte = currentRange.end;
            }
        }
        out.println();
        out.print("--" + MIME_BOUNDARY + "--");

        // Rethrow any exception that has occurred
        if (exception != null) {
            throw exception;
        }

    }

    /**
     * Consumes the content and writes it to the ServletOutputStream.
     *
     * @param content  The source resource
     * @param out   The outputBufferSize stream to write to
     * @param range     Range the client wanted to retrieve
     */
    private static void copy(final MCRContent content, final ServletOutputStream out, final Range range,
        // TODO: beautify this
        final int inputBufferSize, final int outputBufferSize) throws IOException {
        if (content.isReusable()) {
            try (ReadableByteChannel readableByteChannel = content.getReadableByteChannel()) {
                if (readableByteChannel instanceof SeekableByteChannel) {
                    endCurrentTransaction();
                    SeekableByteChannel seekableByteChannel = (SeekableByteChannel) readableByteChannel;
                    seekableByteChannel.position(range.start);
                    long bytesToCopy = range.end - range.start + 1;
                    while (bytesToCopy > 0) {
                        ByteBuffer byteBuffer;
                        if (bytesToCopy > (long) MCRServletContentHelper.DEFAULT_BUFFER_SIZE) {
                            byteBuffer = ByteBuffer.allocate(MCRServletContentHelper.DEFAULT_BUFFER_SIZE);
                        } else {
                            byteBuffer = ByteBuffer.allocate((int) bytesToCopy);
                        }

                        int bytesRead = seekableByteChannel.read(byteBuffer);
                        bytesToCopy -= bytesRead;
                        out.write(byteBuffer.array());
                    }
                    return;
                }
            }
        }

        try (final InputStream resourceInputStream = content.getInputStream();
            final InputStream in = isInputStreamBuffered(resourceInputStream, content) ? resourceInputStream
                : new BufferedInputStream(resourceInputStream, inputBufferSize)) {
            endCurrentTransaction();
            final IOException exception = copyRange(in, out, 0, range.start, range.end, outputBufferSize);
            if (exception != null) {
                throw exception;
            }
        }
    }

    /**
     * Copy the content with the specified range of bytes from InputStream to OutputStream.
     *
     * @param in The input
     * @param out The output
     * @param inPosition Current position of the input
     * @param start Start position to be copied
     * @param end End position to be copied
     * @return Exception which occurred during processing or if less than <code>end - start + 1</code> bytes were read/written.
     */
    private static IOException copyRange(final InputStream in, final ServletOutputStream out, final long inPosition,
        final long start, final long end, final int outputBufferSize) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Serving bytes:" + start + "-" + end);
        }

        final long bytesToRead = end - start + 1;
        final long skip = start - inPosition;
        try {
            final long copied = copyLarge(in, out, skip, bytesToRead, new byte[outputBufferSize]);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Served bytes:" + copied);
            }
            if (copied != bytesToRead) {
                return new EOFException("Bytes to send: " + bytesToRead + " actual: " + copied);
            }
        } catch (final IOException e) {
            return e;
        }
        return null;
    }

    public static long copyLarge(InputStream input, OutputStream output, long inputOffset, long length, byte[] buffer)
        throws IOException {
        if (inputOffset > 0L) {
            long bytesToSkip = inputOffset;
            while (bytesToSkip > 0) {
                bytesToSkip -= input.skip(bytesToSkip);
            }
        }

        if (length == 0L) {
            return 0L;
        } else {
            int bufferLength = buffer.length;
            int bytesToRead = bufferLength;
            if (length > 0L && length < (long) bufferLength) {
                bytesToRead = (int) length;
            }

            long totalRead = 0L;

            int read;
            while (bytesToRead > 0 && -1 != (read = input.read(buffer, 0, bytesToRead))) {
                output.write(buffer, 0, read);
                totalRead += (long) read;
                if (length > 0L) {
                    bytesToRead = (int) Math.min(length - totalRead, (long) bufferLength);
                }
            }

            return totalRead;
        }
    }

    /**
     * Called before sending data to end hibernate transaction.
     */
    private static void endCurrentTransaction() {
        MCRSessionMgr.getCurrentSession().commitTransaction();
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
        final String result = request.getServletPath() + (request.getPathInfo() == null ? "" : request.getPathInfo());
        return result;
    }

    /**
     * Returns if the content InputStream is already buffered.
     * @param contentIS output of {@link MCRContent#getInputStream()}
     * @param content MCRContent instance associated with contentIS
     */
    private static boolean isInputStreamBuffered(final InputStream contentIS, final MCRContent content) {
        return contentIS instanceof BufferedInputStream || contentIS instanceof ByteArrayInputStream
            || contentIS instanceof FilterInputStream && content instanceof MCRVFSContent;
    }

    /**
     * Parses and validates the range header.
     * This method ensures that all ranges are in ascending order and non-overlapping, so we can use a single
     * InputStream.
     */
    private static ArrayList<Range> parseRange(final HttpServletRequest request, final HttpServletResponse response,
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
                    return FULL;
                }
            } else {
                //add one second buffer to check if the content was modified.
                if (lastModified > headerValueTime + 1000) {
                    return FULL;
                }
            }

        }

        final long fileLength = content.length();
        if (fileLength <= 0) {
            return null;
        }

        String rangeHeader = request.getHeader("Range");
        if (rangeHeader == null) {
            return null;
        }

        // We operate on byte level only
        String rangeUnit = "bytes";
        if (!rangeHeader.startsWith(rangeUnit)) {
            response.addHeader("Content-Range", "bytes */" + fileLength);
            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            return null;
        }

        rangeHeader = rangeHeader.substring(rangeUnit.length() + 1);

        final ArrayList<Range> result = new ArrayList<>();
        final StringTokenizer commaTokenizer = new StringTokenizer(rangeHeader, ",");

        // Parsing the range list
        long lastByte = 0;
        while (commaTokenizer.hasMoreTokens()) {
            final String rangeDefinition = commaTokenizer.nextToken().trim();

            final Range currentRange = new Range();
            currentRange.length = fileLength;

            final int dashPos = rangeDefinition.indexOf('-');

            if (dashPos == -1) {
                response.addHeader("Content-Range", "bytes */" + fileLength);
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return null;
            }

            if (dashPos == 0) {

                try {
                    //offset is negative
                    final long offset = Long.parseLong(rangeDefinition);
                    currentRange.start = fileLength + offset;
                    currentRange.end = fileLength - 1;
                } catch (final NumberFormatException e) {
                    response.addHeader("Content-Range", "bytes */" + fileLength);
                    response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    return null;
                }

            } else {

                try {
                    currentRange.start = Long.parseLong(rangeDefinition.substring(0, dashPos));
                    if (dashPos < rangeDefinition.length() - 1) {
                        currentRange.end = Long.parseLong(rangeDefinition.substring(dashPos + 1,
                            rangeDefinition.length()));
                    } else {
                        currentRange.end = fileLength - 1;
                    }
                } catch (final NumberFormatException e) {
                    response.addHeader("Content-Range", "bytes */" + fileLength);
                    response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    return null;
                }

            }

            if (!currentRange.validate() || lastByte > currentRange.start) {
                response.addHeader("Content-Range", "bytes */" + fileLength);
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return null;
            }
            lastByte = currentRange.end;
            result.add(currentRange);
        }
        return result;
    }

    private static void setContentLengthLong(final HttpServletResponse response, final long length) {
        response.setHeader("Content-Length", String.valueOf(length));
    }

}
