package org.mycore.frontend.jersey.feature;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.jersey.MCRStaticContent;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessCheckerFactory;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessFilter;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;

public abstract class MCRJerseyBaseFeature implements DynamicFeature {

    /**
     * Checks if the class/method is annotated by {@link MCRStaticContent}.
     * 
     * @param resourceClass the class to check
     * @param resourceMethod the method to check
     * @return true if one of both is annotated as static
     */
    protected boolean isStaticContent(Class<?> resourceClass, Method resourceMethod) {
        return resourceClass.getAnnotation(MCRStaticContent.class) != null
            || resourceMethod.getAnnotation(MCRStaticContent.class) != null;
    }

    /**
     * Returns a list of packages which will be used to scan for components.
     * 
     * @return a list of java package names
     */
    protected List<String> getPackages() {
        String propertyString = MCRConfiguration.instance().getString("MCR.Jersey.resource.packages",
            "org.mycore.frontend.jersey.resources");
        return Arrays.asList(propertyString.split(","));
    }

    /**
     * Register a MCRRestrictedAccess filter to the context.
     * 
     * @param context the context to add the restricted access too
     * @param restrictedAccess the restricted access
     */
    protected void addFilter(FeatureContext context, MCRRestrictedAccess restrictedAccess) {
        MCRResourceAccessChecker accessChecker;
        try {
            accessChecker = MCRResourceAccessCheckerFactory.getInstance(restrictedAccess.value());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        context.register(new MCRResourceAccessFilter(accessChecker));
    }
}
