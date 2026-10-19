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

package org.mycore.restapi;

import static org.mycore.frontend.jersey.MCRJerseyUtil.APPLICATION_RDF_XML;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.glassfish.jersey.server.filter.UriConnegFilter;
import org.mycore.common.config.MCRConfiguration2;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

/**
 * This filter uses Jersey's default implementation of UriConnectFilter to set
 * the content headers by file extension.
 * <p>
 * It is preconfigured for the extensions (*.json, *.xml, *.rdf) and ignores resources
 * behind REST-API path /contents/ since there is the possibility that a valid
 * file extension (*.xml) will be removed through this filter by mistake.
 * <p>
 * The only exception is the derivate root itself: {@code contents.zip} and {@code contents.tar}
 * are mapped to the media types {@code application/zip} and {@code application/x-tar} to request
 * an archive of all files of a derivate.
 * 
 * @author Robert Stephan
 */
@Provider
@PreMatching
@Priority(Priorities.HEADER_DECORATOR - 10)
public class MCRContentNegotiationViaExtensionFilter implements ContainerRequestFilter {

    private static final Pattern P_URI = Pattern.compile(MCRConfiguration2.getStringOrThrow(
        "MCR.RestAPI.V2.ContentNegotiationViaExtensionFilter.RegEx"));

    private static final MediaType MEDIA_TYPE_ZIP = MediaType.valueOf("application/zip");

    private static final MediaType MEDIA_TYPE_TAR = MediaType.valueOf("application/x-tar");

    private static final List<MediaType> ARCHIVE_MEDIA_TYPES = List.of(MEDIA_TYPE_ZIP, MEDIA_TYPE_TAR);

    private static final Map<String, MediaType> MEDIA_TYPE_MAPPINGS = Map.ofEntries(
        Map.entry("json", MediaType.APPLICATION_JSON_TYPE),
        Map.entry("xml", MediaType.APPLICATION_XML_TYPE),
        Map.entry("rdf", MediaType.valueOf(APPLICATION_RDF_XML)),
        Map.entry("zip", MEDIA_TYPE_ZIP),
        Map.entry("tar", MEDIA_TYPE_TAR));

    private final UriConnegFilter uriConnegFilter = new UriConnegFilter(MEDIA_TYPE_MAPPINGS, Map.of());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (P_URI.matcher(requestContext.getUriInfo().getPath()).matches()) {
            uriConnegFilter.filter(requestContext);
        }
        acceptErrorResponsesForArchives(requestContext);
    }

    /**
     * Error responses cannot be written as archive. If a client accepts nothing but archives, JSON is added as
     * fallback with a low quality, so that errors are delivered as JSON instead of failing with a server error.
     */
    private static void acceptErrorResponsesForArchives(ContainerRequestContext requestContext) {
        List<MediaType> acceptable = requestContext.getAcceptableMediaTypes();
        boolean onlyArchives = !acceptable.isEmpty() && acceptable.stream()
            .allMatch(m -> ARCHIVE_MEDIA_TYPES.stream().anyMatch(a -> a.isCompatible(m) && !m.isWildcardType()
                && !m.isWildcardSubtype()));
        if (onlyArchives) {
            requestContext.getHeaders().putSingle(HttpHeaders.ACCEPT,
                requestContext.getHeaderString(HttpHeaders.ACCEPT) + ", " + MediaType.APPLICATION_JSON + ";q=0.1");
        }
    }

}
