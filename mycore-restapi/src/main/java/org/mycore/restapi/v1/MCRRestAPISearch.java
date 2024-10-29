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

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.services.http.MCRHttpUtils;
import org.mycore.solr.MCRSolrCoreManager;
import org.mycore.solr.MCRSolrUtils;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Rest API methods that cover SOLR searches.
 *
 * @author Robert Stephan
 *
 */
@Path("/search")
public class MCRRestAPISearch {
    public static final String FORMAT_JSON = "json";
    public static final String FORMAT_XML = "xml";
    public static final String FORMAT_CSV = "csv";
    private static Logger LOGGER = LogManager.getLogger(MCRRestAPISearch.class);

    /**
     * see http://wiki.apache.org/solr/CommonQueryParameters for syntax of parameters
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
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8",
        MediaType.TEXT_PLAIN + ";charset=ISO-8859-1", MediaType.TEXT_PLAIN + ";charset=UTF-8" })
    public Response search(@QueryParam("q") String query,
        @QueryParam("sort") String sort, @QueryParam("wt") @DefaultValue("xml") String wt,
        @QueryParam("start") String start, @QueryParam("rows") String rows,
        @QueryParam("fq") List<String> fq, @QueryParam("fl") List<String> fl,
        @QueryParam("facet") String facet, @QueryParam("facet.sort") String facetSort,
        @QueryParam("facet.limit") String facetLimit, @QueryParam("facet.field") List<String> facetFields,
        @QueryParam("facet.mincount") String facetMinCount,
        @QueryParam("json.wrf") String jsonWrf) {
        StringBuilder url = new StringBuilder(MCRSolrCoreManager.getMainSolrCore().getV1CoreURL());
        url.append("/select?");

        // Append query parameters using helper methods
        appendQueryParam(url, "q", query);
        appendQueryParam(url, "sort", sort);
        appendQueryParam(url, "wt", wt);
        appendQueryParam(url, "start", start);
        appendQueryParam(url, "rows", rows);

        appendListQueryParam(url, "fq", fq);
        appendListQueryParam(url, "fl", fl);

        appendQueryParam(url, "facet", facet);
        appendQueryParam(url, "facet.sort", facetSort);
        appendQueryParam(url, "facet.limit", facetLimit);
        appendQueryParam(url, "facet.mincount", facetMinCount);
        appendListQueryParam(url, "facet.field", facetFields);
        appendQueryParam(url, "json.wrf", jsonWrf);

        return executeSolrQuery(url.toString(), wt);
    }

    // Helper method to append single query parameters
    private void appendQueryParam(StringBuilder url, String param, String value) {
        if (value != null) {
            url.append('&').append(param).append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
    }

    // Helper method to append list-based query parameters
    private void appendListQueryParam(StringBuilder url, String param, List<String> values) {
        if (values != null) {
            for (String value : values) {
                appendQueryParam(url, param, value);
            }
        }
    }

    // Method to execute the Solr query and handle response
    private Response executeSolrQuery(String url, String wt) {
        HttpRequest.Builder reqBuilder = MCRSolrUtils.getRequestBuilder().uri(URI.create(url));
        MCRSolrAuthenticationManager.getInstance().applyAuthentication(reqBuilder, MCRSolrAuthenticationLevel.SEARCH);
        HttpRequest request = reqBuilder.build();

        try(HttpClient client = MCRHttpUtils.getHttpClient()) {
            HttpResponse<String> resp
                = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return Response.ok(resp.body())
                .type(getContentType(wt))
                .build();
        } catch (InterruptedException | IOException e) {
            LOGGER.error("Error while executing Solr query", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    // Helper method to determine content type based on 'wt'
    private String getContentType(String wt) {
        return switch (wt) {
            case FORMAT_XML -> "application/xml; charset=UTF-8";
            case FORMAT_JSON -> "application/json; charset=UTF-8";
            case FORMAT_CSV -> "text/comma-separated-values; charset=UTF-8";
            default -> "text";
        };
    }

}
