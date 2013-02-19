package org.mycore.frontend.jersey.filter;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessCheckerFactory;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessFilter;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;

import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;

public class MCRSecurityFilterFactory implements ResourceFilterFactory {

    private static final MCRDBTransactionFilter TRANSACTION_FILTER = new MCRDBTransactionFilter();

    @Context
    HttpServletRequest httpRequest;

    @Override
    public List<ResourceFilter> create(AbstractMethod am) {
        List<ResourceFilter> filters = new ArrayList<ResourceFilter>();
        filters.add(new MCRSessionHookFilter(httpRequest));
        filters.add(TRANSACTION_FILTER);
        MCRRestrictedAccess restrictedAccess = am.getAnnotation(MCRRestrictedAccess.class);
        if (restrictedAccess != null) {
            MCRResourceAccessChecker accessChecker;
            try {
                accessChecker = MCRResourceAccessCheckerFactory.getInstance(restrictedAccess.impl());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
            }
            filters.add(new MCRResourceAccessFilter(accessChecker));
        }
        RolesAllowed ra = am.getAnnotation(RolesAllowed.class);
        if (ra != null) {
            @SuppressWarnings("deprecation")
            ResourceFilter filter = new MCRCheckAccessFilter(am);
            filters.add(filter);
        }
        return filters;
    }
}
