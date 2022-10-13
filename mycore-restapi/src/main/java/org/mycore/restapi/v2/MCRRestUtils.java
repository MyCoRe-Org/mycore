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

package org.mycore.restapi.v2;

import java.util.Date;
import java.util.Optional;

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

    private MCRRestUtils() {
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
}
