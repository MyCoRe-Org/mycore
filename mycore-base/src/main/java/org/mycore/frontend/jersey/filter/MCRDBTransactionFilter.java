package org.mycore.frontend.jersey.filter;

import org.mycore.common.MCRSessionMgr;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

class MCRDBTransactionFilter implements ResourceFilter, ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        MCRSessionMgr.getCurrentSession().commitTransaction();
        return response;
    }

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        MCRSessionMgr.getCurrentSession().beginTransaction();
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