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
import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.MCRSolrClientFactory;

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

    private static final Logger LOGGER = LogManager.getLogger();

    private static Set<String> SEARCHKEYS_FOR_OBJECTS = MCRConfiguration2
        .getString("MCR.RestAPI.V2.AlternativeIdentifier.Objects.Keys").stream()
        .flatMap(MCRConfiguration2::splitValue).collect(Collectors.toSet());

    private static Set<String> SEARCHKEYS_FOR_DERIVATES = MCRConfiguration2
        .getString("MCR.RestAPI.V2.AlternativeIdentifier.Derivates.Keys").stream()
        .flatMap(MCRConfiguration2::splitValue).collect(Collectors.toSet());

    @Context
    ResourceInfo resourceInfo;

    @Context
    HttpServletResponse response;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        UriInfo uriInfo = requestContext.getUriInfo();
        String path = uriInfo.getPath().toString();
        String[] pathParts = path.split("/");
        if (pathParts.length >= 2 && "objects".equals(pathParts[0])) {
            String mcrid = pathParts[1];

            String mcridExtension = "";
            if (mcrid.endsWith(".xml")) {
                mcridExtension = ".xml";
                mcrid = mcrid.substring(0, mcrid.length() - 4);
            }
            if (mcrid.endsWith(".json")) {
                mcridExtension = ".json";
                mcrid = mcrid.substring(0, mcrid.length() - 5);
            }
            try {
                if (!SEARCHKEYS_FOR_OBJECTS.isEmpty() && mcrid.contains(":")) {
                    pathParts[1] = retrieveMCRObjIDfromSOLR(mcrid) + mcridExtension;
                } else {
                    MCRObjectID mcrObjID = MCRObjectID.getInstance(mcrid);
                    // set the properly formated mcrObjID back to URL
                    pathParts[1] = mcrObjID.toString() + mcridExtension;
                }
            } catch (MCRException ex) {
                // ignore

            }

            if (pathParts.length >= 4 && pathParts[2].equals("derivates")) {
                String derid = pathParts[3];

                String deridExtension = "";
                if (derid.endsWith(".xml")) {
                    deridExtension = ".xml";
                    derid = derid.substring(0, mcrid.length() - 4);
                }
                if (derid.endsWith(".json")) {
                    deridExtension = ".json";
                    derid = derid.substring(0, mcrid.length() - 5);
                }
                try {
                    if (!SEARCHKEYS_FOR_DERIVATES.isEmpty() && derid.contains(":")) {
                        pathParts[3] = retrieveMCRDerIDfromSOLR(mcrid, derid) + deridExtension;
                    } else {
                        MCRObjectID mcrDerID = MCRObjectID.getInstance(derid);
                        // set the properly formated mcrObjID back to URL
                        pathParts[3] = mcrDerID.toString() + deridExtension;
                    }
                } catch (MCRException ex) {
                    // ignore
                }
            }
            String newPath = StringUtils.join(pathParts, "/");
            if (!newPath.equals(path)) {
                String queryString = uriInfo.getRequestUri().getQuery();
                URI uri = uriInfo.getBaseUri().resolve(queryString == null ? newPath : newPath + "?" + queryString);
                requestContext.abortWith(Response.temporaryRedirect(uri).build());
            }
        }
    }

    private String retrieveMCRDerIDfromSOLR(String mcrid, String derid) {
        String key = derid.substring(0, derid.indexOf(":"));
        String value = derid.substring(derid.indexOf(":") + 1);
        if (SEARCHKEYS_FOR_DERIVATES.contains(key)) {
            ModifiableSolrParams params = new ModifiableSolrParams();
            params.set("start", 0);
            params.set("rows", 1);
            params.set("fl", "id");
            params.set("fq", "objectKind:mycorederivate");
            params.set("fq", "returnId:" + mcrid);
            params.set("q", key + ":" + ClientUtils.escapeQueryChars(value));
            params.set("sort", "derivateOrder asc");
            QueryResponse solrResponse = null;
            try {
                solrResponse = MCRSolrClientFactory.getMainSolrClient().query(params);
            } catch (Exception e) {
                LOGGER.error("Error retrieving derivate id from SOLR", e);
            }
            if (solrResponse != null) {
                SolrDocumentList solrResults = solrResponse.getResults();
                if (solrResults.getNumFound() == 1) {
                    return String.valueOf(solrResults.get(0).getFieldValue("id"));
                }
                if (solrResults.getNumFound() == 0) {
                    throw new NotFoundException("No MyCoRe Derivate ID found for query " + derid);
                }
                if (solrResults.getNumFound() > 1) {
                    throw new BadRequestException(
                        "The query " + derid + " does not return a unique MyCoRe Derivate ID");
                }
            }
        }
        return derid;
    }

    private String retrieveMCRObjIDfromSOLR(String mcrid) {
        String key = mcrid.substring(0, mcrid.indexOf(":"));
        String value = mcrid.substring(mcrid.indexOf(":") + 1);
        if (SEARCHKEYS_FOR_OBJECTS.contains(key)) {
            ModifiableSolrParams params = new ModifiableSolrParams();
            params.set("start", 0);
            params.set("rows", 1);
            params.set("fl", "id");
            params.set("fq", "objectKind:mycoreobject");
            params.set("q", key + ":" + ClientUtils.escapeQueryChars(value));
            QueryResponse solrResponse = null;
            try {
                solrResponse = MCRSolrClientFactory.getMainSolrClient().query(params);
            } catch (Exception e) {
                LOGGER.error("Error retrieving object id from SOLR", e);
            }
            if (solrResponse != null) {
                SolrDocumentList solrResults = solrResponse.getResults();
                if (solrResults.getNumFound() == 1) {
                    return String.valueOf(solrResults.get(0).getFieldValue("id"));
                }
                if (solrResults.getNumFound() == 0) {
                    throw new NotFoundException("No MyCoRe ID found for query " + mcrid);
                }
                if (solrResults.getNumFound() > 1) {
                    throw new BadRequestException("The query " + mcrid + " does not return a unique MyCoRe ID");
                }
            }
        }
        return mcrid;
    }
}
