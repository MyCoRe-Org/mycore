package org.mycore.webcli.resources;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.jersey.MCRStaticContent;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;

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
        InputStream mainGui = getClass().getResourceAsStream("/META-INF/resources/modules/webcli/index.html");
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        LOGGER.info("MyCore Session REST ID: " + mcrSession.getID());
        LOGGER.info("REST ThreadID: " + Thread.currentThread().getName());
        return Response.ok(mainGui).build();
    }

    @GET
    @Path("gui/{filename:.*}")
    @MCRStaticContent
    public Response getResources(@PathParam("filename") String filename) {
        if (filename.endsWith(".js")) {
            return Response.ok(getClass()
                .getResourceAsStream("/META-INF/resources/modules/webcli/" + filename))
                .header("Content-Type", "application/javascript")
                .build();
        }

        if (filename.endsWith(".css")) {
            return Response.ok(getClass()
                .getResourceAsStream("/META-INF/resources/modules/webcli/" + filename))
                .header("Content-Type", "text/css")
                .build();
        }
        return Response.ok(getClass()
            .getResourceAsStream("/META-INF/resources/modules/webcli/" + filename))
            .build();
    }
}
