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

package org.mycore.restapi.v1;

import static org.mycore.frontend.jersey.MCRJerseyUtil.APPLICATION_JSON_UTF_8;
import static org.mycore.frontend.jersey.MCRJerseyUtil.APPLICATION_XML_UTF_8;
import static org.mycore.frontend.jersey.MCRJerseyUtil.TEXT_PLAIN_ISO_8859_1;
import static org.mycore.frontend.jersey.MCRJerseyUtil.TEXT_PLAIN_UTF_8;
import static org.mycore.frontend.jersey.MCRJerseyUtil.TEXT_XML_UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.solr.MCRSolrIndex;
import org.mycore.solr.MCRSolrIndexManager;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;
import org.mycore.solr.search.MCRSolrSearchUtils;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

/**
 * Rest API methods that cover SOLR searches.
 *
 * @author Robert Stephan
 */
@Path("/search")
public class MCRRestAPISearch {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String FORMAT_JSON = "json";

    public static final String FORMAT_XML = "xml";

    public static final String FORMAT_CSV = "csv";

    /**
     * See <a href="ttp://wiki.apache.org/solr/CommonQueryParameters">CommonQueryParameters</a> for syntax of
     * parameters.
     *
     * @param query       the Query in SOLR Query syntax
     * @param sort        the sort parameter - syntax as defined by SOLR
     * @param wt          the format parameter - syntax as defined by SOLR
     * @param start       the start parameter (number) - syntax as defined by SOLR
     * @param rows        the rows parameter (number) - syntax as defined by SOLR
     * @param fq          the filter query parameter - syntax as defined by SOLR
     * @param fl          the list of fields to be returned - syntax as defined by SOLR
     * @param facet       the facet parameter (true to return facets)  - syntax as defined by SOLR
     * @param facetFields the list of facetFields to be returned - syntax as defined by SOLR
     * @param jsonWrf     the name of the JSONP callback function - syntax as defined by SOLR
     * @return a Jersey Response Object
     */
    @SuppressWarnings("PMD.ExcessiveParameterList")
    @GET
    @Produces({ TEXT_XML_UTF_8, APPLICATION_JSON_UTF_8, TEXT_PLAIN_ISO_8859_1, TEXT_PLAIN_UTF_8 })
    public Response search(@QueryParam(CommonParams.Q) String query,
        @QueryParam(CommonParams.SORT) String sort, @QueryParam(CommonParams.WT) @DefaultValue("xml") String wt,
        @QueryParam(CommonParams.START) String start, @QueryParam(CommonParams.ROWS) String rows,
        @QueryParam(CommonParams.FQ) List<String> fq, @QueryParam(CommonParams.FL) List<String> fl,
        @QueryParam("facet") String facet, @QueryParam("facet.sort") String facetSort,
        @QueryParam("facet.limit") String facetLimit, @QueryParam("facet.field") List<String> facetFields,
        @QueryParam("facet.mincount") String facetMinCount,
        @QueryParam("json.wrf") String jsonWrf) {
        ModifiableSolrParams params = new ModifiableSolrParams();


        // Append query parameters using helper methods
        appendQueryParam(params, CommonParams.Q, query);
        appendQueryParam(params, CommonParams.SORT, sort);
        appendQueryParam(params, CommonParams.WT, wt);
        appendQueryParam(params, CommonParams.START, start);
        appendQueryParam(params, CommonParams.ROWS, rows);

        appendListQueryParam(params, CommonParams.FQ, fq);
        appendListQueryParam(params, CommonParams.FL, fl);

        appendQueryParam(params, "facet", facet);
        appendQueryParam(params, "facet.sort", facetSort);
        appendQueryParam(params, "facet.limit", facetLimit);
        appendQueryParam(params, "facet.mincount", facetMinCount);
        appendListQueryParam(params, "facet.field", facetFields);
        appendQueryParam(params, "json.wrf", jsonWrf);

        return executeSolrQuery(params, wt);
    }

    // Helper method to append single query parameters
    private void appendQueryParam(ModifiableSolrParams params, String param, String value) {
        if (value != null) {
            params.set(param, value);
        }
    }

    // Helper method to append list-based query parameters
    private void appendListQueryParam(ModifiableSolrParams params, String param, List<String> values) {
        if (values != null && !values.isEmpty()) {
            params.add(param, values.toArray(new String[0]));
        }
    }

    // Method to execute the Solr query and handle response
    private Response executeSolrQuery(ModifiableSolrParams params, String wt) {
        MCRSolrIndex solrIndex = MCRSolrIndexManager.obtainInstance().requireMainIndex();
        SolrClient client = solrIndex.getClient();
        QueryRequest queryRequest = new QueryRequest(params);
        MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(queryRequest,
            MCRSolrAuthenticationLevel.SEARCH);

        try (InputStream is = MCRSolrSearchUtils.streamRequest(client, queryRequest, wt)) {
            return Response.ok(is.readAllBytes())
                .type(getContentType(wt))
                .build();
        } catch (IOException | SolrServerException e) {
            LOGGER.error("Error while executing Solr query", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    // Helper method to determine content type based on 'wt'
    private String getContentType(String wt) {
        return switch (wt) {
            case FORMAT_XML -> APPLICATION_XML_UTF_8;
            case FORMAT_JSON -> APPLICATION_JSON_UTF_8;
            case FORMAT_CSV -> "text/comma-separated-values; charset=UTF-8";
            default -> "text";
        };
    }

}
