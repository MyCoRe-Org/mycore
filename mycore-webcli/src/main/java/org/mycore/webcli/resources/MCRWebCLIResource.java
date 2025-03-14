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

package org.mycore.webcli.resources;

import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.jersey.MCRStaticContent;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;
import org.mycore.resource.MCRResourceHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * @author Michel Buechner (mcrmibue)
 *
 */
@Path("WebCLI")
public class MCRWebCLIResource {

    @Context
    HttpServletRequest request;

    private static final Logger LOGGER = LogManager.getLogger();

    @GET
    @MCRRestrictedAccess(MCRWebCLIPermission.class)
    @Produces(MediaType.TEXT_HTML)
    public Response start() {
        InputStream mainGui = getWebCLIResource("index.html");
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        LOGGER.info("MyCore Session REST ID: {}", mcrSession::getID);
        LOGGER.info("REST ThreadID: {}", () -> Thread.currentThread().getName());
        return Response.ok(mainGui).build();
    }

    @GET
    @Path("gui/{filename:.*}")
    @MCRStaticContent
    public Response getResources(@PathParam("filename") String filename) {
        if (filename.endsWith(".js")) {
            return Response.ok(getWebCLIResource(filename))
                .header("Content-Type", "application/javascript")
                .build();
        }

        if (filename.endsWith(".css")) {
            return Response.ok(getWebCLIResource(filename))
                .header("Content-Type", "text/css")
                .build();
        }

        return Response.ok(getWebCLIResource(filename))
            .build();
    }

    private InputStream getWebCLIResource(String filename) {
        return MCRResourceHelper.getWebResourceAsStream("/modules/webcli/" + filename);
    }

}
