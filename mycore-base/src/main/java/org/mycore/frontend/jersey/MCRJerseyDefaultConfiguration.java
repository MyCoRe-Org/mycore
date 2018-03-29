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

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.inject.MCRInjectorConfig;
import org.mycore.frontend.jersey.access.MCRRequestScopeACL;
import org.mycore.frontend.jersey.access.MCRRequestScopeACLFactory;

import com.google.inject.Injector;

/**
 * Default jersey configuration for mycore.
 *
 * <ul>
 *     <li>adds the multipart feature</li>
 *     <li>resolves all resource of the property 'MCR.Jersey.Resource.Packages'</li>
 *     <li>initializes the bridge between jersey and guice</li>
 * </ul>
 *
 */
public class MCRJerseyDefaultConfiguration implements MCRJerseyConfiguration {

    @Override
    public void configure(ResourceConfig resourceConfig) {
        resourceConfig.register(MCRRequestScopeACLFactory.getBinder());
        // setup resources
        setupResources(resourceConfig);

        // include mcr jersey feature
        setupFeatures(resourceConfig);

        // setup guice bridge
        setupGuiceBridge(resourceConfig);
    }

    /**
     * Setup the jersey resources.
     *
     * @param resourceConfig the jersey resource configuration
     */
    protected void setupResources(ResourceConfig resourceConfig) {
        String propertyString = MCRConfiguration.instance().getString("MCR.Jersey.Resource.Packages",
            "org.mycore.frontend.jersey.resources");
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

    /**
     * Adds the binding between guice and hk2. This binding is one directional.
     * You can add guice services into hk2 (jersey) resources. You cannot add
     * a hk2 service into guice.
     * <p>
     * <a href="https://hk2.java.net/guice-bridge/">about the bridge</a>
     * </p>
     *
     * @param resourceConfig the jersey resource configuration
     */
    protected void setupGuiceBridge(ResourceConfig resourceConfig) {
        LogManager.getLogger().info("Initialize hk2 - guice bridge...");
        resourceConfig.register(new AbstractContainerLifecycleListener() {
            @Override
            public void onStartup(Container container) {
                ServiceLocator serviceLocator = container.getApplicationHandler().getServiceLocator();
                Injector injector = MCRInjectorConfig.injector();
                GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
                GuiceIntoHK2Bridge guiceBridge = serviceLocator.getService(GuiceIntoHK2Bridge.class);
                guiceBridge.bridgeGuiceInjector(injector);
            }
        });
    }

}
