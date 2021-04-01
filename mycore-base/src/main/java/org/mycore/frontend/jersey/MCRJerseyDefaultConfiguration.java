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

package org.mycore.frontend.jersey;

import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.jersey.access.MCRRequestScopeACLFilter;

/**
 * Default jersey configuration for mycore.
 *
 * <ul>
 *     <li>adds the multipart feature</li>
 *     <li>resolves all resource of the property 'MCR.Jersey.Resource.Packages'</li>
 * </ul>
 *
 */
public class MCRJerseyDefaultConfiguration implements MCRJerseyConfiguration {

    @Override
    public void configure(ResourceConfig resourceConfig) {
        resourceConfig.register(MCRRequestScopeACLFilter.class);
        // setup resources
        setupResources(resourceConfig);

        // include mcr jersey feature
        setupFeatures(resourceConfig);
    }

    /**
     * Setup the jersey resources.
     *
     * @param resourceConfig the jersey resource configuration
     */
    protected void setupResources(ResourceConfig resourceConfig) {
        String propertyString = MCRConfiguration2.getString("MCR.Jersey.Resource.Packages")
            .orElse("org.mycore.frontend.jersey.resources");
        resourceConfig.packages(propertyString.split(","));
        LogManager.getLogger().info("Scanning jersey resource packages {}", propertyString);
    }

    /**
     * Setup features. By default the multi part feature and every mycore feature
     * class in "org.mycore.frontend.jersey.feature".
     *
     * @param resourceConfig the jersey resource configuration
     */
    protected void setupFeatures(ResourceConfig resourceConfig) {
        // multi part
        resourceConfig.register(MultiPartFeature.class);
        // mycore features
        resourceConfig.packages("org.mycore.frontend.jersey.feature");
    }

}
