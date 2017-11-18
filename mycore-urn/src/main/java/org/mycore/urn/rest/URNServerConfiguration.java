package org.mycore.urn.rest;

/**
 * Interface to configure an urn server.
 */
public interface URNServerConfiguration {

    String getLogin();

    String getPassword();

    String getServiceURL();

}
