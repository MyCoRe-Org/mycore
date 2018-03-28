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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.restapi.v1.errors.MCRRestAPIException;
import org.mycore.restapi.v1.utils.MCRJSONWebTokenUtil;
import org.mycore.restapi.v1.utils.MCRRestAPIUtil;
import org.mycore.restapi.v1.utils.MCRRestAPIUtil.MCRRestAPIACLPermission;
import org.mycore.solr.MCRSolrConstants;

/**
 * Rest API methods that cover SOLR searches.
 *  
 * @author Robert Stephan
 * 
 * @version $Revision: $ $Date: $
 */
@Path("/v1/search")
public class MCRRestAPISearch {
    private static Logger LOGGER = LogManager.getLogger(MCRRestAPISearch.class);

    private static final String HEADER_NAME_AUTHORIZATION = "Authorization";

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
     * @throws MCRRestAPIException    
     */
    @GET
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8",
        MediaType.TEXT_PLAIN + ";charset=ISO-8859-1", MediaType.TEXT_PLAIN + ";charset=UTF-8" })
    public Response search(@Context UriInfo info, @Context HttpServletRequest request, @QueryParam("q") String query,
        @QueryParam("sort") String sort, @QueryParam("wt") @DefaultValue("xml") String wt,
        @QueryParam("start") String start, @QueryParam("rows") String rows, @QueryParam("fq") String fq,
        @QueryParam("fl") String fl, @QueryParam("facet") String facet,
        @QueryParam("facet.field") List<String> facetFields, @QueryParam("json.wrf") String jsonWrf)
        throws MCRRestAPIException {
        MCRRestAPIUtil.checkRestAPIAccess(request, MCRRestAPIACLPermission.READ, "/v1/search");
        StringBuilder url = new StringBuilder(MCRSolrConstants.SERVER_URL);
        url.append("/select?");

        try {
            if (query != null) {
                url.append("&q=").append(URLEncoder.encode(query, "UTF-8"));
            }
            if (sort != null) {
                url.append("&sort=").append(URLEncoder.encode(sort, "UTF-8"));
            }
            if (wt != null) {
                url.append("&wt=").append(wt);
            }
            if (start != null) {
                url.append("&start=").append(start);
            }
            if (rows != null) {
                url.append("&rows=").append(rows);
            }
            if (fq != null) {
                url.append("&fq=").append(URLEncoder.encode(fq, "UTF-8"));
            }
            if (fl != null) {
                url.append("&fl=").append(URLEncoder.encode(fl, "UTF-8"));
            }
            if (facet != null) {
                url.append("&facet=").append(URLEncoder.encode(facet, "UTF-8"));
            }
            for (String ff : facetFields) {
                url.append("&facet.field=").append(URLEncoder.encode(ff, "UTF-8"));
            }
            if (jsonWrf != null) {
                url.append("&json.wrf=").append(jsonWrf);
            }
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e);
        }

        String authHeader = MCRJSONWebTokenUtil
            .createJWTAuthorizationHeader(MCRJSONWebTokenUtil.retrieveAuthenticationToken(request));
        try (InputStream is = new URL(url.toString()).openStream()) {
            try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                String text = scanner.useDelimiter("\\A").next();

                switch (wt) {
                    case FORMAT_XML:
                        return Response.ok(text).type("application/xml; charset=UTF-8")
                            .header(HEADER_NAME_AUTHORIZATION, authHeader).build();
                    //break;
                    case FORMAT_JSON:
                        return Response.ok(text).type("application/json; charset=UTF-8")
                            .header(HEADER_NAME_AUTHORIZATION, authHeader).build();
                    //break;
                    case FORMAT_CSV:
                        return Response.ok(text).type("text/comma-separated-values; charset=UTF-8")
                            .header(HEADER_NAME_AUTHORIZATION, authHeader).build();
                    default:
                        return Response.ok(text).type("text").header(HEADER_NAME_AUTHORIZATION, authHeader).build();
                }
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }

        return Response.status(Response.Status.BAD_REQUEST).build();
    }
}
