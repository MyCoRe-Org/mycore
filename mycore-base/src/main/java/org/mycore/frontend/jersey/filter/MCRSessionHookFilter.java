package org.mycore.frontend.jersey.filter;

import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

class MCRSessionHookFilter implements ResourceFilter, ContainerRequestFilter, ContainerResponseFilter {
    private HttpServletRequest httpRequest;
    private HttpServletResponse httpResponse;

    private static final Logger LOGGER = Logger.getLogger(MCRSessionHookFilter.class);

    public MCRSessionHookFilter(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
    }

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        MCRSessionMgr.releaseCurrentSession();
        return response;
    }

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        MCRSession session = MCRServlet.getSession(httpRequest);
        MCRSessionMgr.setCurrentSession(session);
        LOGGER.info(MessageFormat.format("{0} ip={1} mcr={2} user={3}", request.getPath(),
            MCRFrontendUtil.getRemoteAddr(httpRequest), session.getID(), session.getUserInformation().getUserID()));
        MCRFrontendUtil.configureSession(session, httpRequest, httpResponse);
        return request;
    }

    @Override
    public ContainerRequestFilter getRequestFilter() {
        return this;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
        return this;
    }

}
