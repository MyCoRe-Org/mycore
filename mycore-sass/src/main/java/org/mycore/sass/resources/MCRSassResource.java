/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  This program is free software; you can use it, redistribute it
 *  and / or modify it under the terms of the GNU General Public License
 *  (GPL) as published by the Free Software Foundation; either version 2
 *  of the License or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program, in a file called gpl.txt or license.txt.
 *  If not, write to the Free Software Foundation Inc.,
 *  59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 */

/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  This program is free software; you can use it, redistribute it
 *  and / or modify it under the terms of the GNU General Public License
 *  (GPL) as published by the Free Software Foundation; either version 2
 *  of the License or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program, in a file called gpl.txt or license.txt.
 *  If not, write to the Free Software Foundation Inc.,
 *  59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 */

package org.mycore.sass.resources;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.mycore.frontend.jersey.MCRStaticContent;
import org.mycore.sass.MCRSassCompilerManager;
import org.mycore.sass.MCRServletContextResourceImporter;

import io.bit3.jsass.CompilationException;

@Path("/sass/")
@MCRStaticContent
public class MCRSassResource {

    private static final int SECONDS_OF_ONE_DAY = 60 * 60 * 24;

    @Context
    ServletContext context;

    @GET
    @Path("{fileName:.+}")
    @Produces("text/css")
    public Response getCSS(@PathParam("fileName") String name, @Context Request request) {
        try {
            MCRServletContextResourceImporter importer = new MCRServletContextResourceImporter(context);
            Optional<String> cssFile = MCRSassCompilerManager.getInstance()
                .getCSSFile(name, Stream.of(importer).collect(Collectors.toList()));

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
        } catch (IOException | CompilationException e) {
            StreamingOutput so = (OutputStream os) -> e.printStackTrace(new PrintStream(os, true, "UTF-8"));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(so).build();
        }
    }
}
