package org.mycore.frontend.jersey.filter;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessCheckerFactory;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessFilter;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;

import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;

public class MCRSecurityFilterFactory implements ResourceFilterFactory {
    private static final Logger LOGGER = Logger.getLogger(MCRSecurityFilterFactory.class);

    private static final MCRDBTransactionFilter TRANSACTION_FILTER = new MCRDBTransactionFilter();

    @Context
    HttpServletRequest httpRequest;

    @Override
    public List<ResourceFilter> create(AbstractMethod am) {
        List<ResourceFilter> filters = new ArrayList<ResourceFilter>();
        filters.add(new MCRSessionHookFilter(httpRequest));
        filters.add(TRANSACTION_FILTER);
        
        MCRRestrictedAccess restrictedAccessMETHOD = am.getAnnotation(MCRRestrictedAccess.class);
        MCRRestrictedAccess restrictedAccessTYPE = am.getResource().getAnnotation(MCRRestrictedAccess.class);
        if (restrictedAccessMETHOD != null) {
            LOGGER.info("Access to " + am.getMethod().toString() + " is restricted by " + restrictedAccessMETHOD.value().getCanonicalName());
            addFilter(filters, restrictedAccessMETHOD);
        } else if(restrictedAccessTYPE != null) {
            LOGGER.info("Access to " + am.getResource().getResourceClass().getName() + " is restricted by " + restrictedAccessTYPE.value().getCanonicalName());
            addFilter(filters, restrictedAccessTYPE);
        }
        return filters;
    }

    private void addFilter(List<ResourceFilter> filters, MCRRestrictedAccess restrictedAccess) {
        MCRResourceAccessChecker accessChecker;
        try {
            accessChecker = MCRResourceAccessCheckerFactory.getInstance(restrictedAccess.value());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        filters.add(new MCRResourceAccessFilter(accessChecker));
    }
}
