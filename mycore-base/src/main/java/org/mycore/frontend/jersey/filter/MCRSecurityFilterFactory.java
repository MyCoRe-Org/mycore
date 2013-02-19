package org.mycore.frontend.jersey.filter;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.mycore.common.MCRSession;

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
        RolesAllowed ra = am.getAnnotation(RolesAllowed.class);
        if (ra != null) {
            filters.add(new MCRCheckAccessFilter(am));
        }
        return filters;
    }
}
