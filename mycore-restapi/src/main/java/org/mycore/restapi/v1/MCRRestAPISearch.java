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

package org.mycore.restapi.v1;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.solr.proxy.MCRSolrProxyHttpClient;
import org.mycore.solr.proxy.MCRSolrProxyHttpClient.McrSolrHttpResult;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Rest API methods that cover SOLR searches.
 *
 * @author Robert Stephan
 *
 */
@Path("/search")
public class MCRRestAPISearch {
    private static Logger LOGGER = LogManager.getLogger(MCRRestAPISearch.class);

    public static final String FORMAT_JSON = "json";

    public static final String FORMAT_XML = "xml";

    public static final String FORMAT_CSV = "csv";

    /**
     * see http://wiki.apache.org/solr/CommonQueryParameters for syntax of parameters
     *
     * @param info - the injected Jersey URIInfo object
     * @param request - the injected HTTPServletRequest object
     *
     * @param query
     *      the Query in SOLR Query syntax
     * @param sort
     *      the sort parameter - syntax as defined by SOLR
     * @param wt
     *      the format parameter - syntax as defined by SOLR
     * @param start
     *      the start parameter (number) - syntax as defined by SOLR      
     * @param rows
     *      the rows parameter (number) - syntax as defined by SOLR
     * @param fq
     *      the filter query parameter - syntax as defined by SOLR
     * @param fl
     *      the list of fields to be returned - syntax as defined by SOLR
     * @param facet
     *      the facet parameter (true to return facets)  - syntax as defined by SOLR
     * @param facetFields
     *      the list of facetFields to be returned - syntax as defined by SOLR
     * @param jsonWrf
     *      the name of the JSONP callback function - syntax as defined by SOLR 
     *
     * @return a Jersey Response Object
     */
    @GET
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8",
        MediaType.TEXT_PLAIN + ";charset=ISO-8859-1", MediaType.TEXT_PLAIN + ";charset=UTF-8" })
    public Response search(@Context UriInfo info, @Context HttpServletRequest request,
        @Context HttpServletResponse response, @QueryParam("wt") String wt) {

        try (MCRSolrProxyHttpClient solrHttpClient = new MCRSolrProxyHttpClient()) {
            McrSolrHttpResult result = solrHttpClient.handleQuery("/select", request, response);
            String contentType = switch (wt) {
                case FORMAT_XML -> "application/xml; charset=UTF-8";
                case FORMAT_JSON -> "application/json; charset=UTF-8";
                case FORMAT_CSV -> "text/comma-separated-values; charset=UTF-8";
                default -> "text";
            };
            return Response.ok(result.response().getEntity())
                .type(contentType)
                .build();

        } catch (IOException e) {
            LOGGER.error("Error in SOLR RestAPI", e);

        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

}
