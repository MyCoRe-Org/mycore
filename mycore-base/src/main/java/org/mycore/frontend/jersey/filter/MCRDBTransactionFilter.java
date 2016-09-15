package org.mycore.frontend.jersey.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import org.mycore.common.MCRSessionMgr;

public class MCRDBTransactionFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        MCRSessionMgr.getCurrentSession().commitTransaction();
    }

    @Override
    public void filter(ContainerRequestContext request) {
        MCRSessionMgr.getCurrentSession().beginTransaction();
    }

}