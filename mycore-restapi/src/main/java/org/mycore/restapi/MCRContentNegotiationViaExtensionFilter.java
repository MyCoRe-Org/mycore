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

package org.mycore.restapi;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import org.glassfish.jersey.server.filter.UriConnegFilter;
import org.mycore.common.config.MCRConfiguration2;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

/**
 * This filter uses Jersey's default implementation of UriConnectFilter to set
 * the content headers by file extension.
 * 
 * It is preconfigured for the extensions (*.json, *.xml) and ignores ressources
 * behind REST-API path /contents/ since there is the possibility that a valid
 * file extension (*.xml) will be removed through this filter by mistake
 * 
 * @author Robert Stephan
 * 
 */

@Provider
@PreMatching
@Priority(Priorities.HEADER_DECORATOR - 10)
public class MCRContentNegotiationViaExtensionFilter implements ContainerRequestFilter {

    private static Pattern P_URI = Pattern.compile(MCRConfiguration2.getStringOrThrow(
        "MCR.RestAPI.V2.ContentNegotiationViaExtensionFilter.RegEx"));

    private static final Map<String, MediaType> MEDIA_TYPE_MAPPINGS = Map.ofEntries(
        Map.entry("json", MediaType.APPLICATION_JSON_TYPE),
        Map.entry("xml", MediaType.APPLICATION_XML_TYPE));

    private UriConnegFilter uriConnegFilter = new UriConnegFilter(MEDIA_TYPE_MAPPINGS, Map.of());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (P_URI.matcher(requestContext.getUriInfo().getPath()).matches()) {
            uriConnegFilter.filter(requestContext);
        }
    }
}
