package org.mycore.frontend.jersey;

import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.inject.MCRInjectorConfig;

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
