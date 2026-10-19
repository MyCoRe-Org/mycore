/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.restapi.v2;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;

import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

public final class MCRRestUtils {

    public static final String JSON_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static final String TAG_MYCORE_CLASSIFICATION = "mcr_class";

    public static final String TAG_MYCORE_OBJECT = "mcr_object";

    public static final String TAG_MYCORE_DERIVATE = "mcr_derivate";

    public static final String TAG_MYCORE_FILE = "mcr_file";

    public static final String TAG_MYCORE_ABOUT = "mcr_about";

    public static final String TAG_MYCORE_EVENTS = "mcr_events";

    private static final String ATTR_CHAR_SPECIALS = "!#$&+-.^_`|~";

    private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();

    private MCRRestUtils() {
    }

    public static Response getCachedResponseIgnoreExceptions(Request request, MCRObjectID id) {
        try {
            long lastModified = MCRXMLMetadataManager.obtainInstance().getLastModified(id);
            if (lastModified >= 0) {
                Date lmDate = new Date(lastModified);
                Optional<Response> cachedResponse = getCachedResponse(request, lmDate);
                if (cachedResponse.isPresent()) {
                    return cachedResponse.get();
                }
            }
        } catch (Exception e) {
            //ignore errors as PUT is idempotent
        }
        return null;
    }

    public static Optional<Response> getCachedResponse(Request request, Date lastModified) {
        return Optional.ofNullable(request)
            .map(r -> r.evaluatePreconditions(lastModified))
            .map(Response.ResponseBuilder::build);
    }

    public static Optional<Response> getCachedResponse(Request request, Date lastModified, EntityTag eTag) {
        return Optional.ofNullable(request)
            .map(r -> r.evaluatePreconditions(lastModified, eTag))
            .map(Response.ResponseBuilder::build);
    }

    public static void checkExists(MCRObjectID id) {
        if (!MCRMetadataManager.exists(id)) {
            throw MCRErrorResponse.ofStatusCode(Response.Status.NOT_FOUND.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCROBJECT_NOT_FOUND)
                .withMessage("MCRObject " + id + " not found")
                .toException();
        }
    }

    /**
     * Returns the value of a {@code Content-Disposition} header for the given file name (RFC 6266).
     * <p>
     * The parameter {@code filename} carries an ASCII fallback for old clients, in which every character that is not
     * printable ASCII, a quote or a backslash is replaced by {@code _}. The parameter {@code filename*} carries the
     * exact file name, percent encoded as UTF-8 (RFC 8187).
     *
     * @param dispositionType the disposition type, e.g. {@code attachment} or {@code inline}
     * @param fileName the file name
     * @return the header value
     */
    public static String getContentDisposition(String dispositionType, String fileName) {
        String asciiFileName = fileName.replaceAll("[^\\x20-\\x7E]|[\"\\\\]", "_");
        StringBuilder encodedFileName = new StringBuilder();
        for (byte b : fileName.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if (isAttrChar(c)) {
                encodedFileName.append((char) c);
            } else {
                encodedFileName.append('%').append(HEX_FORMAT.toHexDigits(b));
            }
        }
        return dispositionType + "; filename=\"" + asciiFileName + "\"; filename*=UTF-8''" + encodedFileName;
    }

    private static boolean isAttrChar(int c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
            || ATTR_CHAR_SPECIALS.indexOf(c) >= 0;
    }

}
