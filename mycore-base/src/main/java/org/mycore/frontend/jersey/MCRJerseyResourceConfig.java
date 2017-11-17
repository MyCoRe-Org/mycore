package org.mycore.frontend.jersey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.inject.MCRInjectorConfig;

import com.google.inject.Injector;

/**
 * Entry point for mycore jersey configuration. Loads the {@link MCRJerseyConfiguration} defined in
 * 'MCR.Jersey.Configuration' or the default {@link MCRJerseyDefaultConfiguration}.
 * 
 * @author Matthias Eichner
 */
public class MCRJerseyResourceConfig extends ResourceConfig {

    public MCRJerseyResourceConfig() {
        super();
        LogManager.getLogger().info("Loading jersey resource config...");
        MCRJerseyConfiguration configuration;
        try {
            configuration = MCRConfiguration.instance().getInstanceOf("MCR.Jersey.Configuration",
                new MCRJerseyDefaultConfiguration());
        } catch (MCRConfigurationException exc) {
            LogManager.getLogger().error("Unable to initialize jersey.", exc);
            return;
        }
        try {
            configuration.configure(this);
        } catch (Exception exc) {
            LogManager.getLogger().error("Unable to configure jersey.", exc);
        }
    }

}
