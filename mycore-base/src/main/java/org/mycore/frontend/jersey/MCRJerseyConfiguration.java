package org.mycore.frontend.jersey;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * Base interface to configure jersey in an mycore application. Used to register resource, features and so on.
 *
 * @author Matthias Eichner
 */
public interface MCRJerseyConfiguration {

    /**
     * Configures the application.
     *
     * @param resourceConfig the jersey resource configuration
     */
    void configure(ResourceConfig resourceConfig);

}
