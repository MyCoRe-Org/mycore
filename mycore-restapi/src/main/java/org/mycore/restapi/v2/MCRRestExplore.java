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

package org.mycore.restapi.v2;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.restapi.v2.explore.model.MCRRestExploreResponse;
import org.mycore.restapi.v2.explore.model.MCRRestExploreResponseObject;
import org.mycore.solr.MCRSolrClientFactory;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;

/**
 * This RestAPI endpoint provides simple access to the underlying SOLR server.
 * It supports pagination, sorting and filtering.
 * You can configure a global filter query and define solr fields, 
 * which can be delivered as payload in the response.
 * 
 * @author Robert Stephan
 *
 */
@Path("/explore")
@OpenAPIDefinition(
        tags = {})
public class MCRRestExplore {
    public static int MAX_ROWS = 1000;

    @Context
    Request request;

    @Context
    ServletContext context;

    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @MCRCacheControl(
            maxAge = @MCRCacheControl.Age(
                    time = 1,
                    unit = TimeUnit.HOURS),
            sMaxAge = @MCRCacheControl.Age(
                    time = 1,
                    unit = TimeUnit.HOURS))
    /*
     * @Operation( summary = "Explore objects in this repository", responses
     * = @ApiResponse( content = @Content(array = @ArraySchema(schema
     * = @Schema(implementation = MCRObjectIDDate.class)))), tags =
     * MCRRestUtils.TAG_MYCORE_OBJECT)
     */
    @XmlElementWrapper(
            name = "mycoreobjects")
    public Response exploreObjects(@QueryParam("start") String start, @QueryParam("rows") String rows,
            @QueryParam("sort") String sort, @QueryParam("filter") List<String> filter) throws IOException {
        Date lastModified = new Date(MCRXMLMetadataManager.instance().getLastModified());
        Optional<Response> cachedResponse = MCRRestUtils.getCachedResponse(request, lastModified);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }
        MCRRestExploreResponse response = new MCRRestExploreResponse();

        SolrClient solrClient = MCRSolrClientFactory.getMainSolrClient();
        SolrQuery q = new SolrQuery("*:*");
        MCRConfiguration2.getString("MCR.RestAPI.V2.Explore.FilterQuery").ifPresent(fq -> {
            q.addFilterQuery(fq);
        });
        if (start != null) {
            int s = 0;
            try {
                s = Integer.parseInt(start);
                if (s < 0) {
                    s = 0;
                }
            } catch (NumberFormatException nfe) {
                // ignore
            }
            q.setStart(s);
        }
        if (rows != null) {
            int r = 0;
            try {
                r = Integer.parseInt(rows);
                if (r < 0) {
                    r = 0;
                }
                if (r > MAX_ROWS) {
                    r = MAX_ROWS;
                }
            } catch (NumberFormatException nfe) {
                // ignore
            }
            q.setRows(r);
        }
        if (sort != null) {
            Arrays.stream(sort.split(",")).map(String::trim).forEach(s -> {
                if (s.toLowerCase().endsWith(" asc")) {
                    q.addSort(s.substring(0, s.length() - 4).trim(), SolrQuery.ORDER.asc);

                } else if (s.toLowerCase().endsWith(" desc")) {
                    q.addSort(s.substring(0, s.length() - 5).trim(), SolrQuery.ORDER.desc);
                } else {
                    q.addSort(s.trim(), SolrQuery.ORDER.asc);
                }
            });
        }
        if (filter != null) {
            for (String f : filter) {
                q.addFilterQuery(f.trim());
            }
        }

        try {
            QueryResponse solrResponse = solrClient.query(q);
            response.getHeader().setRows(q.getRows() == null ? 10 : q.getRows());
            SolrDocumentList solrResults = solrResponse.getResults();
            response.getHeader().setStart(solrResults.getStart());
            response.getHeader().setNumFound(solrResults.getNumFound());
            if (!q.getSorts().isEmpty()) {
                response.getHeader().setSort(String.join(",",
                        q.getSorts().stream()
                                .map(x -> x.getItem() + " " + x.getOrder().name())
                                .collect(Collectors.toList())));
            }
            for (int i = 0; i < solrResults.size(); ++i) {
                SolrDocument solrDoc = solrResults.get(i);
                Date dModified = (Date) solrDoc.getFieldValue("modified");
                MCRRestExploreResponseObject responseObj = new MCRRestExploreResponseObject(
                        String.valueOf(solrDoc.getFieldValue("id")),
                        dModified.toInstant());

                MCRConfiguration2.getString("MCR.RestAPI.V2.Explore.PayloadFields").ifPresent(fields -> {
                    for (String field : fields.split(",")) {
                        Object value = solrDoc.getFieldValue(field);
                        if (value != null) {
                            if (value instanceof List) {
                                for (Object o : (List<?>) value) {
                                    responseObj.addPayload(field, o);
                                }
                            } else {
                                responseObj.addPayload(field, value);
                            }
                        }
                    }
                });
                response.getData().add(responseObj);
            }

        } catch (SolrServerException | IOException e) {
            // TODO Auto-generated catch block
        }

        return Response.ok(response)
                .lastModified(lastModified)
                .build();
    }
}
