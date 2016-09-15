package org.mycore.frontend.jersey.feature;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.jersey.filter.MCRDBTransactionFilter;
import org.mycore.frontend.jersey.filter.MCRSessionHookFilter;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessCheckerFactory;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessFilter;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;

@Provider
public class MCRJerseyDefaultFeature implements DynamicFeature {

    private static final Logger LOGGER = Logger.getLogger(MCRJerseyDefaultFeature.class);

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        String propertyString = MCRConfiguration.instance().getString("MCR.Jersey.resource.packages",
            "org.mycore.frontend.jersey.resources");
        List<String> packages = Arrays.asList(propertyString.split(","));

        Class<?> resourceClass = resourceInfo.getResourceClass();
        Method resourceMethod = resourceInfo.getResourceMethod();

        String packageName = resourceClass.getPackage().getName();
        if (packages.contains(packageName)) {
            context.register(MCRDBTransactionFilter.class);
            context.register(MCRSessionHookFilter.class);

            MCRRestrictedAccess restrictedAccessMETHOD = resourceMethod.getAnnotation(MCRRestrictedAccess.class);
            MCRRestrictedAccess restrictedAccessTYPE = resourceClass.getAnnotation(MCRRestrictedAccess.class);

            if (restrictedAccessMETHOD != null) {
                LOGGER.info("Access to " + resourceMethod.toString() + " is restricted by "
                    + restrictedAccessMETHOD.value().getCanonicalName());
                addFilter(context, restrictedAccessMETHOD);
            } else if (restrictedAccessTYPE != null) {
                LOGGER.info("Access to " + resourceClass.getName() + " is restricted by "
                    + restrictedAccessTYPE.value().getCanonicalName());
                addFilter(context, restrictedAccessTYPE);
            }
        }
    }

    private void addFilter(FeatureContext context, MCRRestrictedAccess restrictedAccess) {
        MCRResourceAccessChecker accessChecker;
        try {
            accessChecker = MCRResourceAccessCheckerFactory.getInstance(restrictedAccess.value());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        context.register(new MCRResourceAccessFilter(accessChecker));
    }

}
