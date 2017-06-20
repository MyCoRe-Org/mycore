package org.mycore.urn.rest;

/**
 * Interface to configure an urn server.
 */
public interface URNServerConfiguration {

    public String getLogin();

    public String getPassword();

    public String getServiceURL();

}
