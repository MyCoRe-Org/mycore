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
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRContent;

import com.google.common.collect.Iterables;

/**
 * @author Thomas Scheffler (yagee)
 * @author Matthias Eichner
 */
public abstract class MCRRestContentHelper {

    private static Logger LOGGER = LogManager.getLogger(MCRRestContentHelper.class);

    public static final int DEFAULT_BUFFER_SIZE = ContentUtils.DEFAULT_BUFFER_SIZE;

    public static final String ATT_SERVE_CONTENT = MCRRestContentHelper.class.getName() + ".serveContent";

    public enum ContentDispositionType {
        inline, attachment;
    }

    public static class Config {

        public ContentDispositionType dispositionType = ContentDispositionType.attachment;

        public boolean useAcceptRanges = true;

        public int inputBufferSize = ContentUtils.DEFAULT_BUFFER_SIZE;

        public int outputBufferSize = ContentUtils.DEFAULT_BUFFER_SIZE;

    }

    public static Response serveContent(final MCRContent content, final UriInfo uriInfo,
        final HttpHeaders requestHeader)
        throws IOException {
        return serveContent(content, uriInfo, requestHeader, new Config());
    }

    public static Response serveContent(final MCRContent content, final UriInfo uriInfo,
        final HttpHeaders requestHeader, final Config config) throws IOException {

        final String path = uriInfo.getPath();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Serving '{}' headers and data", path);
        }

        if (content == null) {
            throw new NotFoundException();
        }

        // Find content type.
        String mimeType = content.getMimeType();
        if (mimeType == null) {
            mimeType = MediaType.APPLICATION_OCTET_STREAM;
        }
        MediaType contentType = MediaType.valueOf(mimeType);
        String enc = content.getEncoding();
        if (enc != null) {
            HashMap<String, String> param = new HashMap<>(contentType.getParameters());
            param.put(MediaType.CHARSET_PARAMETER, enc);
            contentType = new MediaType(contentType.getType(), contentType.getSubtype(), param);
        }

        Response.ResponseBuilder response = Response.ok();

        String eTag = content.getETag();
        List<Range> ranges = null;
        if (config.useAcceptRanges) {
            response.header("Accept-Ranges", "bytes");
        }

        final long contentLength = content.length();
        long lastModified = content.lastModified();
        ranges = parseRange(requestHeader, lastModified, eTag, contentLength);

        response.header(HttpHeaders.ETAG, eTag);

        if (lastModified >= 0) {
            response.lastModified(new Date(lastModified));
        }
        String dispositionType = config.dispositionType.name();
        String filename = Optional.of(content.getName())
            .orElseGet(() -> Iterables.getLast(uriInfo.getPathSegments()).getPath());
        response.header(HttpHeaders.CONTENT_DISPOSITION, dispositionType + ";filename=\"" + filename + "\"");

        boolean serveContent = true;
        //No Content to serve?
        if (contentLength == 0) {
            serveContent = false;
        }

        if (ranges == null || ranges == ContentUtils.FULL) {
            //No ranges
            if (contentType != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("contentType='{}'", contentType);
                }
                response.type(contentType);
            }
            if (contentLength >= 0) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("contentLength={}", contentLength);
                }
                response.header(HttpHeaders.CONTENT_LENGTH, contentLength);
            }

            if (serveContent) {
                response.entity(
                    (StreamingOutput) out -> ContentUtils.copy(content, out, config.inputBufferSize,
                        config.outputBufferSize));
            }

        } else {

            if (ranges.isEmpty()) {
                return response.status(Response.Status.NO_CONTENT).build();
            }

            // Partial content response.

            response.status(Response.Status.PARTIAL_CONTENT);

            if (ranges.size() == 1) {

                final Range range = ranges.get(0);
                response.header("Content-Range", "bytes " + range.start + "-" + range.end + "/" + range.length);
                final long length = range.end - range.start + 1;
                response.header(HttpHeaders.CONTENT_LENGTH, length);

                if (contentType != null) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("contentType='{}'", contentType);
                    }
                    response.type(contentType);
                }

                if (serveContent) {
                    response.entity(
                        (StreamingOutput) out -> ContentUtils.copy(content, out, range, config.inputBufferSize,
                            config.outputBufferSize));
                }

            } else {

                response.type("multipart/byteranges; boundary=" + ContentUtils.MIME_BOUNDARY);

                if (serveContent) {
                    Iterator<Range> rangeIterator = ranges.iterator();
                    String ct = contentType.toString();
                    response.entity(
                        (StreamingOutput) out -> ContentUtils.copy(content, out, rangeIterator, ct,
                            config.inputBufferSize,
                            config.outputBufferSize));
                }
            }
        }
        return response.build();
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
     * Parses and validates the range header.
     * This method ensures that all ranges are in ascending order and non-overlapping, so we can use a single
     * InputStream.
     */
    private static List<Range> parseRange(HttpHeaders headers, long lastModified, @NotNull String eTag,
        long contentLength) {

        // Checking if range is still valid (lastModified, ETag)
        String ifRangeHeader = headers.getHeaderString("If-Range");
        if (ifRangeHeader != null) {
            long headerValueTime = -1L;
            try {
                RuntimeDelegate.HeaderDelegate<Date> dateHeaderDelegate = RuntimeDelegate.getInstance()
                    .createHeaderDelegate(Date.class);
                headerValueTime = dateHeaderDelegate.fromString(ifRangeHeader).getTime();
            } catch (final IllegalArgumentException e) {
                // Ignore
            }

            if (headerValueTime == -1L) {
                // If the content changed, the complete content is served.
                if (!eTag.equals(ifRangeHeader.trim())) {
                    return ContentUtils.FULL;
                }
            } else {
                //add one second buffer to check if the content was modified.
                if (lastModified > headerValueTime + 1000) {
                    return ContentUtils.FULL;
                }
            }

        }

        if (contentLength <= 0) {
            return null;
        }

        String rangeHeader = headers.getHeaderString("Range");
        try {
            return Range.parseRanges(rangeHeader, contentLength);
        } catch (IllegalArgumentException e) {
            Response errResponse = Response.status(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE)
                .header("Content-Range", "bytes */" + contentLength).build();
            throw new WebApplicationException(errResponse);
        }
    }

}
