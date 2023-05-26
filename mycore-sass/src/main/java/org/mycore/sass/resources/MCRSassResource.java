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

package org.mycore.sass.resources;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.mycore.frontend.jersey.MCRStaticContent;
import org.mycore.sass.MCRSassCompilerManager;
import org.mycore.sass.MCRServletContextResourceImporter;

import de.larsgrefer.sass.embedded.SassCompilationFailedException;
import de.larsgrefer.sass.embedded.importer.CustomImporter;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

/**
 * Resource to deliver CSS files compiled by SASS
 */
@Path("/sass/")
@MCRStaticContent
public class MCRSassResource {

    private static final int SECONDS_OF_ONE_DAY = 60 * 60 * 24;

    @Context
    ServletContext context;

    /**
     * return the compiled CSS 
     * @param name - the name of file
     * @param request - the Http request
     * @return the response object
     */
    @GET
    @Path("{fileName:.+}")
    @Produces("text/css")
    public Response getCSS(@PathParam("fileName") String name, @Context Request request) {
        try {
            final String fileName = MCRSassCompilerManager.getRealFileName(name);
            CustomImporter importer = new MCRServletContextResourceImporter(context, fileName).autoCanonicalize();
            Optional<String> cssFile = MCRSassCompilerManager.getInstance()
                .getCSSFile(name, List.of(importer));

            if (cssFile.isPresent()) {
                CacheControl cc = new CacheControl();
                cc.setMaxAge(SECONDS_OF_ONE_DAY);

                String etagString = MCRSassCompilerManager.getInstance().getLastMD5(name).get();
                EntityTag etag = new EntityTag(etagString);

                Response.ResponseBuilder builder = request.evaluatePreconditions(etag);
                if (builder != null) {
                    return builder.cacheControl(cc).tag(etag).build();
                }

                return Response.ok().status(Response.Status.OK)
                    .cacheControl(cc)
                    .tag(etag)
                    .entity(cssFile.get())
                    .build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .build();
            }
        } catch (IOException | SassCompilationFailedException e) {
            StreamingOutput so = (OutputStream os) -> e.printStackTrace(new PrintStream(os, true,
                StandardCharsets.UTF_8));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(so).build();
        }
    }
}
