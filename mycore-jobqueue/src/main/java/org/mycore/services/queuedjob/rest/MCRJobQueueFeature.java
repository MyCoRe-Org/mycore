/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.services.queuedjob.rest;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.jersey.feature.MCRJerseyDefaultFeature;
import org.mycore.restapi.MCREnableTransactionFilter;
import org.mycore.restapi.annotations.MCRRequireTransaction;

import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

/**
 * Jersey configuration for JobQueue Endpoint
 * 
 * @author Sebastian Hofmann
 * 
 * @see MCRJerseyDefaultFeature
 * 
 */
@Provider
public class MCRJobQueueFeature extends MCRJerseyDefaultFeature {
    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        Class<?> resourceClass = resourceInfo.getResourceClass();
        Method resourceMethod = resourceInfo.getResourceMethod();
        if (requiresTransaction(resourceClass, resourceMethod)) {
            context.register(MCREnableTransactionFilter.class);
        }
        super.configure(resourceInfo, context);
    }

    /**
     * Checks if the class/method is annotated by {@link MCRRequireTransaction}.
     *
     * @param resourceClass the class to check
     * @param resourceMethod the method to check
     * @return true if one ore both is annotated and requires transaction
     */
    protected boolean requiresTransaction(Class<?> resourceClass, Method resourceMethod) {
        return resourceClass.getAnnotation(MCRRequireTransaction.class) != null
            || resourceMethod.getAnnotation(MCRRequireTransaction.class) != null;
    }

    @Override
    protected List<String> getPackages() {
        return MCRConfiguration2.getString("MCR.JobQueue.API.Resource.Packages").map(MCRConfiguration2::splitValue)
            .orElse(Stream.empty())
            .collect(Collectors.toList());
    }

    @Override
    protected void registerSessionHookFilter(FeatureContext context) {
        // don't register transaction filter, is already implemented by MCRSessionFilter
    }

    @Override
    protected void registerTransactionFilter(FeatureContext context) {
        // don't register transaction filter, is already implemented by MCRSessionFilter
    }

    @Override
    protected void registerAccessFilter(FeatureContext context, Class<?> resourceClass, Method resourceMethod) {
        super.registerAccessFilter(context, resourceClass, resourceMethod);
    }

}
