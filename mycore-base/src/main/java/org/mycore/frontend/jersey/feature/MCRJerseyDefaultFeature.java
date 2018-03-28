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

package org.mycore.frontend.jersey.feature;

import java.lang.reflect.Method;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.frontend.jersey.filter.MCRDBTransactionFilter;
import org.mycore.frontend.jersey.filter.MCRSessionHookFilter;
import org.mycore.frontend.jersey.filter.MCRSessionLockFilter;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;

/**
 * Default feature for mycore. Does register a transaction, session and
 * access filter.
 * 
 * @author Matthias Eichner
 */
@Provider
public class MCRJerseyDefaultFeature extends MCRJerseyBaseFeature {

    private static final Logger LOGGER = LogManager.getLogger(MCRJerseyDefaultFeature.class);

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        Class<?> resourceClass = resourceInfo.getResourceClass();
        Method resourceMethod = resourceInfo.getResourceMethod();
        if (isStaticContent(resourceClass, resourceMethod)) {
            // class or method is annotated with MCRStaticContent
            //   -> do only register session lock filter
            context.register(MCRSessionLockFilter.class);
            return;
        }
        String packageName = resourceClass.getPackage().getName();
        if (getPackages().contains(packageName)) {
            registerTransactionFilter(context);
            registerSessionHookFilter(context);
            registerAccessFilter(context, resourceClass, resourceMethod);
        }
    }

    protected void registerTransactionFilter(FeatureContext context) {
        context.register(MCRDBTransactionFilter.class);
    }

    protected void registerSessionHookFilter(FeatureContext context) {
        context.register(MCRSessionHookFilter.class);
    }

    protected void registerAccessFilter(FeatureContext context, Class<?> resourceClass, Method resourceMethod) {
        MCRRestrictedAccess restrictedAccessMETHOD = resourceMethod.getAnnotation(MCRRestrictedAccess.class);
        MCRRestrictedAccess restrictedAccessTYPE = resourceClass.getAnnotation(MCRRestrictedAccess.class);
        if (restrictedAccessMETHOD != null) {
            LOGGER.info("Access to {} is restricted by {}", resourceMethod,
                restrictedAccessMETHOD.value().getCanonicalName());
            addFilter(context, restrictedAccessMETHOD);
        } else if (restrictedAccessTYPE != null) {
            LOGGER.info("Access to {} is restricted by {}", resourceClass.getName(),
                restrictedAccessTYPE.value().getCanonicalName());
            addFilter(context, restrictedAccessTYPE);
        }
    }

}
