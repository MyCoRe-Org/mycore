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
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

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

/**
 * This prematching filter checks the given MCRObjectIDs in an REST API call beginning with /objects,
 * normalizes them and sends a redirect if necessary.
 * 
 * @author Robert Stephan
 *
 */
@Provider
@PreMatching
public class MCRNormalizeMCRObjectIDsFilter implements ContainerRequestFilter {

	private static final Logger LOGGER = LogManager.getLogger();

	private static List<String> SEARCHKEYS_FOR_OBJECTS = Arrays.asList(
			MCRConfiguration2.getString("MCR.RestAPI.V2.AlternativeIdentifier.Objects.Keys").orElse("").split(","));
	private static List<String> SEARCHKEYS_FOR_DERIVATES = Arrays.asList(
			MCRConfiguration2.getString("MCR.RestAPI.V2.AlternativeIdentifier.Derivates.Keys").orElse("").split(","));

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
			try {
				if (mcrid.contains(":")) {
					String key = mcrid.substring(0, mcrid.indexOf(":"));
					String value = mcrid.substring(mcrid.indexOf(":") + 1);
					if (SEARCHKEYS_FOR_OBJECTS.contains(key)) {
						ModifiableSolrParams params = new ModifiableSolrParams();
						params.set("start", 0);
						params.set("rows", 1);
						params.set("fl", "id");
						params.set("fq", "objectKind:mycoreobject");
						params.set("q", key + ":" + ClientUtils.escapeQueryChars(value));
						QueryResponse solrResponse;
						try {
							solrResponse = MCRSolrClientFactory.getMainSolrClient().query(params);
							SolrDocumentList solrResults = solrResponse.getResults();
							if (solrResults.getNumFound() >= 1) {
								mcrid = String.valueOf(solrResults.get(0).getFieldValue("id"));
								pathParts[1] = mcrid;
							}
						} catch (Exception e) {
							LOGGER.error("Error retrieving object id from SOLR", e);
						}
					}
				} else {
					MCRObjectID mcrObjID = MCRObjectID.getInstance(mcrid);
					// set the properly formated mcrObjID back to URL
					mcrid = mcrObjID.toString();
					if (!mcrid.equals(pathParts[1])) {
						pathParts[1] = mcrid;
					}
				}
			} catch (MCRException ex) {
				// ignore

			}

			if (pathParts.length >= 4 && pathParts[2].equals("derivates")) {
				String derid = pathParts[3];
				try {
					if (derid.contains(":")) {
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
							QueryResponse solrResponse;
							try {
								solrResponse = MCRSolrClientFactory.getMainSolrClient().query(params);
								SolrDocumentList solrResults = solrResponse.getResults();
								if (solrResults.getNumFound() >= 1) {
									derid = String.valueOf(solrResults.get(0).getFieldValue("id"));
									pathParts[3] = derid;
								}
							} catch (Exception e) {
								LOGGER.error("Error retrieving derivate id from SOLR", e);
							}
						}
					} else {
						MCRObjectID mcrDerID = MCRObjectID.getInstance(derid);
						// set the properly formated mcrObjID back to URL
						derid = mcrDerID.toString();
						if (!derid.equals(pathParts[3])) {
							pathParts[3] = derid;
						}
					}
				} catch (MCRException ex) {
					// ignore
				}
			}
			String newPath = StringUtils.join(pathParts, "/");
			if (!newPath.equals(path)) {
				URI uri = uriInfo.getBaseUri().resolve(newPath);
				response.sendRedirect(uri.toString());
				// without server sent redirect:
				//requestContext.setRequestUri(uri);
			}
		}
	}
}
