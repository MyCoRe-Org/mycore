package org.mycore.frontend.jersey.filter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.RolesAllowed;
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
        LOGGER.info("Adding hook filter");
        filters.add(new MCRSessionHookFilter(httpRequest));
        filters.add(TRANSACTION_FILTER);

        MCRRestrictedAccess restrictedAccess = am.getAnnotation(MCRRestrictedAccess.class);
        if (restrictedAccess != null) {
            LOGGER.info("Access to " + am.getMethod().toString() + " is restricted by " + restrictedAccess.value().getCanonicalName());
            MCRResourceAccessChecker accessChecker;
            try {
                accessChecker = MCRResourceAccessCheckerFactory.getInstance(restrictedAccess.value());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
            }
            filters.add(new MCRResourceAccessFilter(accessChecker));
        }
        RolesAllowed ra = am.getAnnotation(RolesAllowed.class);
        if (ra != null) {
            LOGGER.warn(MessageFormat.format(
                "MCRCheckAccessFilter will be removed with release version 2.2. Migrate {0} to @MCRRestrictedAccess!", am
                    .getMethod()
                    .toString()));
            @SuppressWarnings("deprecation")
            ResourceFilter filter = new MCRCheckAccessFilter(am);
            filters.add(filter);
        }
        return filters;
    }
}
