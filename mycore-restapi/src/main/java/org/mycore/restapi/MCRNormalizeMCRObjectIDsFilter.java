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

import java.net.URI;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.idmapper.MCRIDMapper;

import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

/**
 * This prematching filter checks the given MCRObjectIDs in an REST API call beginning with /objects,
 * normalizes them and sends a redirect if necessary.
 * 
 * @author Robert Stephan
 *
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHORIZATION - 10)
public class MCRNormalizeMCRObjectIDsFilter implements ContainerRequestFilter {

    @Context
    ResourceInfo resourceInfo;

    @Context
    HttpServletResponse response;

    private MCRIDMapper mcrIdMapper = MCRConfiguration2
        .<MCRIDMapper>getInstanceOf(MCRIDMapper.MCR_PROPERTY_CLASS).get();

    @Override
    public void filter(ContainerRequestContext requestContext) {
        UriInfo uriInfo = requestContext.getUriInfo();
        String path = uriInfo.getPath().toString();
        String[] pathParts = path.split("/", -1);
        final int mcrIdPos = 1;
        final int derIdPos = 3;
        if (pathParts.length <= mcrIdPos || !"objects".equals(pathParts[mcrIdPos - 1])) {
            return;
        }

        try {
            String mcrid = pathParts[mcrIdPos];
            String mcridExtension = getExtension(mcrid);
            mcrid = mcrid.substring(0, mcrid.length() - mcridExtension.length());
            Optional<MCRObjectID> optObjId = mcrIdMapper.mapMCRObjectID(mcrid);
            if (optObjId.isEmpty()) {
                throw new NotFoundException("No unique MyCoRe Object ID found for query " + mcrid);
            }
            pathParts[mcrIdPos] = optObjId.get().toString() + mcridExtension;

            if (optObjId.isPresent() && pathParts.length > derIdPos && pathParts[derIdPos - 1].equals("derivates")) {
                String derid = pathParts[derIdPos];
                String deridExtension = getExtension(derid);
                derid = derid.substring(0, derid.length() - deridExtension.length());
                Optional<MCRObjectID> optDerId = mcrIdMapper.mapMCRDerivateID(optObjId.get(), derid);
                if (optDerId.isEmpty()) {
                    throw new NotFoundException("No unique MyCoRe Derivate ID found for query " + derid);
                }
                pathParts[derIdPos] = optDerId.get().toString() + deridExtension;
            }
        } catch (MCRException ex) {
            throw new BadRequestException("Could not detect MyCoRe ID", ex);
        }

        String newPath = StringUtils.join(pathParts, "/");
        if (!newPath.equals(path)) {
            String queryString = uriInfo.getRequestUri().getQuery();
            URI uri = uriInfo.getBaseUri().resolve(queryString == null ? newPath : newPath + "?" + queryString);
            requestContext.abortWith(Response.temporaryRedirect(uri).build());
        }
    }

    private static String getExtension(String mcrid) {
        if (mcrid.endsWith(".xml")) {
            return ".xml";
        }
        if (mcrid.endsWith(".json")) {
            return ".json";
        }
        return "";
    }

}
