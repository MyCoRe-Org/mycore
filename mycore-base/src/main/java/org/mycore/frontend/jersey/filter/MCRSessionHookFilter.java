package org.mycore.frontend.jersey.filter;

import javax.servlet.http.HttpServletRequest;

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.servlets.MCRServlet;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

class MCRSessionHookFilter implements ResourceFilter, ContainerRequestFilter, ContainerResponseFilter {
    private HttpServletRequest httpRequest;
    
    public MCRSessionHookFilter(HttpServletRequest httpRequest) {
        this.httpRequest = httpRequest;
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