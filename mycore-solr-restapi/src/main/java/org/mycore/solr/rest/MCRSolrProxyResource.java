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

package org.mycore.solr.rest;

import static org.mycore.access.MCRAccessManager.PERMISSION_READ;
import static org.mycore.solr.MCRSolrConstants.SOLR_CONFIG_PREFIX;
import static org.mycore.solr.MCRSolrConstants.SOLR_QUERY_XML_PROTOCOL_VERSION;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.InputStreamResponseParser;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.solr.MCRSolrIndex;
import org.mycore.solr.MCRSolrIndexRegistryManager;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/")
public class MCRSolrProxyResource {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRSolrAuthenticationManager SOLR_AUTHENTICATION_MANAGER =
        MCRSolrAuthenticationManager.obtainInstance();

    @GET
    @Path("{core}/{queryHandler: .+}")
    public Response query(@PathParam("core") String core,
        @PathParam("queryHandler") String queryHandler,
        @Context UriInfo uriInfo) {

        String queryHandlerPath = "/" + queryHandler;

        Set<String> whitelist = getQueryHandlerWhitelist();
        if (!whitelist.contains(queryHandlerPath)) {
            return Response.status(Response.Status.FORBIDDEN)
                .entity("No access to " + queryHandlerPath)
                .build();
        }

        String ruleID = "solr:" + queryHandlerPath;
        if (MCRAccessManager.hasRule(ruleID, PERMISSION_READ)
            && !MCRAccessManager.checkPermission(ruleID, PERMISSION_READ)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        return handleQuery(queryHandlerPath, core, uriInfo);
    }

    private Response handleQuery(String queryHandlerPath, String core, UriInfo uriInfo) {
        ModifiableSolrParams solrParams = buildSolrParams(uriInfo.getQueryParameters());
        filterParams(solrParams);

        QueryRequest queryRequest = new QueryRequest(solrParams);
        queryRequest.setPath(queryHandlerPath);
        SOLR_AUTHENTICATION_MANAGER.applyAuthentication(queryRequest, MCRSolrAuthenticationLevel.SEARCH);

        Optional<MCRSolrIndex> optionalIndex = MCRSolrIndexRegistryManager.obtainRegistry().getIndex(core);
        if (optionalIndex.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("No such core: " + core)
                .build();
        }

        MCRSolrIndex solrIndex = optionalIndex.get();
        String writerType = solrParams.get("wt", "json");
        queryRequest.setResponseParser(new InputStreamResponseParser(writerType));

        try {
            NamedList<Object> solrResponse = solrIndex.getClient().request(queryRequest);
            try (InputStream is = (InputStream) solrResponse.get("stream")) {
                byte[] responseBytes = is.readAllBytes();

                Response.ResponseBuilder responseBuilder;
                if (solrResponse.get("responseStatus") != null) {
                    Integer responseStatus = (Integer) solrResponse.get("responseStatus");
                    responseBuilder = Response.status(responseStatus);
                } else {
                    responseBuilder = Response.ok();
                }

                String contentType = getContentType(writerType);
                return responseBuilder
                    .entity(responseBytes)
                    .type(contentType)
                    .build();
            }
        } catch (SolrServerException | IOException e) {
            LOGGER.error("Error while processing Solr query request", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error while processing query request: " + e.getMessage())
                .build();
        }
    }

    private ModifiableSolrParams buildSolrParams(MultivaluedMap<String, String> queryParameters) {
        Map<String, String[]> parameterMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
            parameterMap.put(entry.getKey(), entry.getValue().toArray(new String[0]));
        }

        ModifiableSolrParams solrParams = new ModifiableSolrParams(parameterMap);
        if (!parameterMap.containsKey("version") && !parameterMap.containsKey("wt")) {
            solrParams.set("version", SOLR_QUERY_XML_PROTOCOL_VERSION);
        }
        return solrParams;
    }

    private void filterParams(ModifiableSolrParams solrParameter) {
        MCRConfiguration2.getString("MCR.Solr.Disallowed.Facets")
            .ifPresent(disallowedFacets -> MCRConfiguration2.splitValue(disallowedFacets)
                .forEach(disallowedFacet -> solrParameter.remove("facet.field", disallowedFacet)));

        MCRConfiguration2.getString("MCR.Solr.Proxy.Disallowed.Parameter")
            .ifPresent(disallowedParameter -> MCRConfiguration2.splitValue(disallowedParameter)
                .forEach(solrParameter::remove));
    }

    private Set<String> getQueryHandlerWhitelist() {
        List<String> whitelistPropertyList = MCRConfiguration2.getString(SOLR_CONFIG_PREFIX + "Proxy.WhiteList")
            .map(MCRConfiguration2::splitValue)
            .map(s -> s.collect(Collectors.toList()))
            .orElseGet(() -> Collections.singletonList("/select"));
        return new HashSet<>(whitelistPropertyList);
    }

    private String getContentType(String writerType) {
        return switch (writerType) {
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "csv" -> "text/csv";
            case "javabin" -> "application/octet-stream";
            default -> "application/octet-stream";
        };
    }
}
