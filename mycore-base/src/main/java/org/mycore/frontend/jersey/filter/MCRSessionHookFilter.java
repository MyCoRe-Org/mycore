package org.mycore.frontend.jersey.filter;

import java.io.IOException;
import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;

public class MCRSessionHookFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Context
    private HttpServletRequest httpRequest;

    @Context
    private HttpServletResponse httpResponse;

    private static final Logger LOGGER = LogManager.getLogger(MCRSessionHookFilter.class);

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        MCRSession session = MCRServlet.getSession(httpRequest);
        MCRSessionMgr.setCurrentSession(session);
        LOGGER.info(MessageFormat.format("{0} ip={1} mcr={2} user={3}", request.getUriInfo().getPath(),
            MCRFrontendUtil.getRemoteAddr(httpRequest), session.getID(), session.getUserInformation().getUserID()));
        MCRFrontendUtil.configureSession(session, httpRequest, httpResponse);
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        MCRSessionMgr.releaseCurrentSession();
    }

}
