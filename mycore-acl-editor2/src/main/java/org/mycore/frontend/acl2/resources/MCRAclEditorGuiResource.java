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

package org.mycore.frontend.acl2.resources;

import java.io.InputStream;

import org.mycore.frontend.jersey.MCRStaticContent;
import org.mycore.resource.MCRResourceHelper;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

@Path("ACLE/gui")
@MCRStaticContent
public class MCRAclEditorGuiResource {

    @Context
    HttpServletResponse response;

    @GET
    @Path("{filename:.*}")
    public Response getResources(@PathParam("filename") String filename) {
        if (filename.endsWith(".js")) {
            return Response.ok(getAclEditorGuiResource(filename))
                .header("Content-Type", "application/javascript")
                .build();
        }

        if (filename.endsWith(".css")) {
            return Response.ok(getAclEditorGuiResource(filename))
                .header("Content-Type", "text/css")
                .build();
        }

        return Response.ok(getAclEditorGuiResource(filename))
            .build();
    }

    private InputStream getAclEditorGuiResource(String filename) {
        return MCRResourceHelper.getWebResourceAsStream("/modules/acl-editor2/gui/" + filename);
    }

}
