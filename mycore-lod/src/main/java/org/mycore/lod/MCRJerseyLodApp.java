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

package org.mycore.lod;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.jersey.access.MCRRequestScopeACLFilter;
import org.mycore.restapi.MCRCORSResponseFilter;
import org.mycore.restapi.MCRIgnoreClientAbortInterceptor;
import org.mycore.restapi.MCRSessionFilter;
import org.mycore.restapi.MCRTransactionFilter;
import org.mycore.restapi.converter.MCRWrappedXMLWriter;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Response;

/**
 * Basic configuration for the MyCoRe Linked Open Data Endpoint
 * 
 * @author Robert Stephan
 */
@ApplicationPath("/open-data")
public class MCRJerseyLodApp extends ResourceConfig {

    //RDFXML is the default/fallback format an does not have to be on this list
    private static List<RDFFormat> RDF_OUTPUT_FORMATS = List.of(RDFFormat.TURTLE, RDFFormat.JSONLD);

    public MCRJerseyLodApp() {
        super();
        initAppName();
        property(ServerProperties.APPLICATION_NAME, getApplicationName());
        packages(getRestPackages());
        property(ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, true);
        register(MCRSessionFilter.class);
        register(MCRTransactionFilter.class);
        register(MCRLodFeature.class);
        register(MCRCORSResponseFilter.class);
        register(MCRRequestScopeACLFilter.class);
        register(MCRIgnoreClientAbortInterceptor.class);
    }

    protected void initAppName() {
        setApplicationName(MCRConfiguration2.getString("MCR.NameOfProject").orElse("MyCoRe") + " LOD-Endpoint");
        LogManager.getLogger().info("Initiialize {}", getApplicationName());
    }

    protected String[] getRestPackages() {
        return Stream
            .concat(
                Stream.of(MCRWrappedXMLWriter.class.getPackage().getName(),
                    OpenApiResource.class.getPackage().getName()),
                MCRConfiguration2.getOrThrow("MCR.LOD.Resource.Packages", MCRConfiguration2::splitValue))
            .toArray(String[]::new);
    }

    public static Response returnLinkedData(String rdfxmlString, URI uri, List<String> mimeTypes)
        throws RDFParseException, UnsupportedRDFormatException, IOException {
        try {
            for (RDFFormat rdfOutFormat : RDF_OUTPUT_FORMATS) {
                if (!Collections.disjoint(mimeTypes, rdfOutFormat.getMIMETypes())) {
                    Model model = Rio.parse(new StringReader(rdfxmlString), uri.toString(), RDFFormat.RDFXML);
                    StringWriter sw = new StringWriter();
                    Rio.write(model, sw, rdfOutFormat);
                    return Response.ok(sw.toString()).type(rdfOutFormat.getDefaultMIMEType() + ";charset=UTF-8")
                        .build();
                }
            }
        } catch (IOException | RDFParseException | RDFHandlerException e) {
            // do nothing
        }
        //fallback, default: RDFFormat.RDFXML
        return Response.ok(rdfxmlString, RDFFormat.RDFXML.getDefaultMIMEType() + ";charset=UTF-8")
            .build();
    }
}
