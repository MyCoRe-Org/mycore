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

import java.util.List;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Variant;

/**
 * Delivers an {@link MCRErrorResponse} as JSON if the client accepts neither JSON nor XML.
 * <p>
 * An error response carries an {@link MCRErrorResponse} entity without an explicit media type, so Jersey picks
 * the media type from the {@code Accept} header. If the client only accepts e.g. {@code application/zip} or
 * {@code application/pdf}, there is no writer for the error entity in that type, and Jersey answers with an
 * empty {@code 500 Internal Server Error} instead of the original status (e.g. 404). This filter sets JSON as
 * media type in that case, so status, {@code X-Error-*} headers and error body reach the client. Clients that
 * accept JSON or XML keep the negotiated format.
 */
public class MCRErrorResponseMediaTypeFilter implements ContainerResponseFilter {

    private static final List<Variant> SUPPORTED_VARIANTS = Variant
        .mediaTypes(MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE).build();

    private static final MediaType FALLBACK_MEDIA_TYPE = MediaType.APPLICATION_JSON_TYPE.withCharset("UTF-8");

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        if (responseContext.getEntity() instanceof MCRErrorResponse
            && requestContext.getRequest().selectVariant(SUPPORTED_VARIANTS) == null) {
            responseContext.setEntity(responseContext.getEntity(), responseContext.getEntityAnnotations(),
                FALLBACK_MEDIA_TYPE);
        }
    }
}
