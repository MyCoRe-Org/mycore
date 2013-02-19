package org.mycore.frontend.jersey.filter;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.jersey.config.MCRResourceSercurityConf;

import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.api.model.PathValue;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

@Deprecated
class MCRCheckAccessFilter implements ResourceFilter, ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger LOGGER = Logger.getLogger(MCRCheckAccessFilter.class);

    private String resourceName;

    private String resourceOperation;

    public MCRCheckAccessFilter(AbstractMethod am) {
        this.resourceName = am.getResource().getResourceClass().getName();
        this.resourceOperation = getPath(am) + "_" + getHttpMethod(am);
        MCRResourceSercurityConf.instance().registerResource(resourceName, resourceOperation);
    }

    private MCRAccessManagerConnector getAccessManagerConnector() {
        MCRConfiguration instance = MCRConfiguration.instance();
        String defaultConnector = MCRAccessManagerConnector.class.getName();
        return (MCRAccessManagerConnector) instance.getInstanceOf("McrSessionSecurityFilter.MCRAccessManager.Connector", defaultConnector);
    }

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        MCRSession session = MCRSessionMgr.getCurrentSession();

        boolean hasPermission = getAccessManagerConnector().checkPermission(resourceName, resourceOperation, session);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("current user ID: " + session.getUserInformation().getUserID());
            LOGGER.debug("resource name: " + resourceName);
            LOGGER.debug("resource operation: " + resourceOperation);
            LOGGER.debug("has permission: " + hasPermission);
        }
        if (!hasPermission) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
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

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        return response;
    }

    private String getPath(AbstractMethod am) {
        PathValue resourcePath = am.getResource().getPath();
        if (resourcePath == null) {
            return null;
        }

        String path = "/" + resourcePath.getValue();
        Path methodPath = am.getAnnotation(Path.class);
        if (methodPath != null) {
            return path + "/" + methodPath.value();
        }

        return path;
    }

    private String getHttpMethod(AbstractMethod am) {
        Class[] httpMethods = { POST.class, GET.class, PUT.class, DELETE.class };
        for (Class httpMethod : httpMethods) {
            if (am.isAnnotationPresent(httpMethod)) {
                return httpMethod.getSimpleName();
            }
        }

        return null;
    }
}
