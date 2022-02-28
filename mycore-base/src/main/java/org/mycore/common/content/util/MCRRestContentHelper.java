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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRContent;

import com.google.common.collect.Iterables;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.RuntimeDelegate;

/**
 * @author Thomas Scheffler (yagee)
 */
public abstract class MCRRestContentHelper {

    public static final RuntimeDelegate.HeaderDelegate<Date> DATE_HEADER_DELEGATE = RuntimeDelegate.getInstance()
        .createHeaderDelegate(Date.class);

    private static Logger LOGGER = LogManager.getLogger();

    public static Response serveContent(final MCRContent content, final UriInfo uriInfo,
        final HttpHeaders requestHeader, List<Map.Entry<String, String>> responseHeader)
        throws IOException {
        return serveContent(content, uriInfo, requestHeader, responseHeader, new Config());
    }

    public static Response serveContent(final MCRContent content, final UriInfo uriInfo,
        final HttpHeaders requestHeader, final List<Map.Entry<String, String>> responseHeader, final Config config)
        throws IOException {

        if (content == null) {
            throw new NotFoundException();
        }

        // Find content type.
        MediaType contentType = getMediaType(content);

        Response.ResponseBuilder response = Response.ok();

        String eTag = content.getETag();
        response.header(HttpHeaders.ETAG, eTag);
        responseHeader.forEach(e -> response.header(e.getKey(), e.getValue()));
        final long contentLength = content.length();
        if (contentLength == 0) {
            //No Content to serve?
            return response.status(Response.Status.NO_CONTENT).build();
        }
        long lastModified = content.lastModified();
        if (lastModified >= 0) {
            response.lastModified(new Date(lastModified));
        }

        List<Range> ranges = null;
        if (config.useAcceptRanges) {
            response.header("Accept-Ranges", "bytes");
            ranges = parseRange(requestHeader, lastModified, eTag, contentLength);
            String varyHeader = Stream.of("Range", "If-Range")
                .filter(h -> requestHeader.getHeaderString(h) != null)
                .collect(Collectors.joining(","));
            if (!varyHeader.isEmpty()) {
                response.header(HttpHeaders.VARY, varyHeader);
            }
        }

        String filename = Optional.of(content.getName())
            .orElseGet(() -> Iterables.getLast(uriInfo.getPathSegments()).getPath());
        response.header(HttpHeaders.CONTENT_DISPOSITION,
            config.dispositionType.name() + ";filename=\"" + filename + "\"");

        boolean noRangeRequest = ranges == null || ranges == ContentUtils.FULL;
        if (noRangeRequest) {
            LOGGER.debug("contentType='{}'", contentType);
            LOGGER.debug("contentLength={}", contentLength);
            response.type(contentType);
            response.header(HttpHeaders.CONTENT_LENGTH, contentLength);
            response.entity(
                (StreamingOutput) out -> ContentUtils.copy(content, out, config.inputBufferSize,
                    config.outputBufferSize));

        } else if (ranges.isEmpty()) {
            return response.status(Response.Status.NO_CONTENT).build();
        } else {
            // Partial content response.
            response.status(Response.Status.PARTIAL_CONTENT);

            if (ranges.size() == 1) {
                final Range range = ranges.get(0);
                response.header("Content-Range", "bytes " + range.start + "-" + range.end + "/" + range.length);
                final long length = range.end - range.start + 1;
                response.header(HttpHeaders.CONTENT_LENGTH, length);

                LOGGER.debug("contentType='{}'", contentType);
                response.type(contentType);

                response.entity(
                    (StreamingOutput) out -> ContentUtils.copy(content, out, range, config.inputBufferSize,
                        config.outputBufferSize));
            } else {
                response.type("multipart/byteranges; boundary=" + ContentUtils.MIME_BOUNDARY);
                Iterator<Range> rangeIterator = ranges.iterator();
                String ct = contentType.toString();
                response.entity(
                    (StreamingOutput) out -> ContentUtils.copy(content, out, rangeIterator, ct,
                        config.inputBufferSize,
                        config.outputBufferSize));
            }
        }
        return response.build();
    }

    private static MediaType getMediaType(MCRContent content) throws IOException {
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
        return contentType;
    }

    /**
     * Parses and validates the range header.
     * This method ensures that all ranges are in ascending order and non-overlapping, so we can use a single
     * InputStream.
     */
    private static List<Range> parseRange(HttpHeaders headers, long lastModified, String eTag, long contentLength) {

        // Checking if range is still valid (lastModified, ETag)
        String ifRangeHeader = headers.getHeaderString("If-Range");
        if (ifRangeHeader != null) {
            long headerValueTime = -1L;
            try {
                headerValueTime = DATE_HEADER_DELEGATE.fromString(ifRangeHeader).getTime();
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
        String rangeHeader = headers.getHeaderString("Range");
        try {
            return Range.parseRanges(rangeHeader, contentLength);
        } catch (IllegalArgumentException e) {
            Response errResponse = Response.status(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE)
                .header("Content-Range", "bytes */" + contentLength).build();
            throw new WebApplicationException(errResponse);
        }
    }

    public enum ContentDispositionType {
        inline, attachment
    }

    public static class Config {

        public ContentDispositionType dispositionType = ContentDispositionType.attachment;

        public boolean useAcceptRanges = true;

        public int inputBufferSize = ContentUtils.DEFAULT_BUFFER_SIZE;

        public int outputBufferSize = ContentUtils.DEFAULT_BUFFER_SIZE;

    }

}
