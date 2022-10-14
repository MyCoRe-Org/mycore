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

package org.mycore.lod.controller;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.lod.MCRJerseyLodApp;
import org.mycore.restapi.v2.MCRErrorResponse;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

/**
 * Linked Open Data: Root End point
 * 
 * (Work in Progress - find some reasonable schema to for repository metadata)
 * 
 * @author Robert Stephan
 */
@Path("/")
public class MCRLodRoot {

    private static Namespace NS_FOAF = Namespace.getNamespace("foaf", "http://xmlns.com/foaf/0.1/");

    @Context
    ContainerRequestContext request;

    /**
     * provide some basic information about the linked open data endpoint
     * 
     * @return a short description using FOAF vocabulary
     */
    @GET
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS))
    public Response outputLODRoot() {
        try {
            Document docRepository = createRepositoryInfo();
            String rdfxmlString = new MCRJDOMContent(docRepository).asString();
            URI uri = request.getUriInfo().getBaseUri();
            List<String> mimeTypes = request.getAcceptableMediaTypes().parallelStream().map(x -> x.toString()).toList();
            return MCRJerseyLodApp.returnLinkedData(rdfxmlString, uri, mimeTypes);
        } catch (IOException e) {
            throw MCRErrorResponse.fromStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .withErrorCode("INFO_ERROR")
                .withMessage("Could not create Repository information")
                .toException();
        }
    }

    private Document createRepositoryInfo() {
        Element eAgent = new Element("Agent", NS_FOAF);
        eAgent.addContent(
            new Element("name", NS_FOAF)
                .setText(MCRConfiguration2.getString("MCR.NameOfProject").orElse("MyCoRe Repository")));
        eAgent.addContent(
            new Element("homepage", NS_FOAF)
                .setAttribute("resource", MCRFrontendUtil.getBaseURL(), MCRConstants.RDF_NAMESPACE));
        return new Document(eAgent);
    }

}
