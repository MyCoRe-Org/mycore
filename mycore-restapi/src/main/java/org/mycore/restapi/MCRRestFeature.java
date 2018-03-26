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

package org.mycore.restapi;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.jersey.MCRStaticContent;
import org.mycore.frontend.jersey.feature.MCRJerseyDefaultFeature;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.frontend.jersey.filter.MCRCacheFilter;
import org.mycore.restapi.annotations.MCRRequireTransaction;

/**
 * Jersey configuration 
 * @author Matthias Eichner
 * 
 * @see MCRJerseyDefaultFeature
 * 
 * @version $Revision: $ $Date: $
 * 
 */
@Provider
public class MCRRestFeature extends MCRJerseyDefaultFeature {
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
     * Checks if the class/method is annotated by {@link MCRStaticContent}.
     *
     * @param resourceClass the class to check
     * @param resourceMethod the method to check
     * @return true if one of both is annotated as static
     */
    protected boolean requiresTransaction(Class<?> resourceClass, Method resourceMethod) {
        return resourceClass.getAnnotation(MCRRequireTransaction.class) != null
            || resourceMethod.getAnnotation(MCRRequireTransaction.class) != null;
    }

    @Override
    protected List<String> getPackages() {
        return MCRConfiguration.instance().getStrings("MCR.RestAPI.Resource.Packages");
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
        context.register(MCRRestAuthorizationFilter.class);
        super.registerAccessFilter(context, resourceClass, resourceMethod);
    }

}
