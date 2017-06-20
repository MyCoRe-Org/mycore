package org.mycore.frontend.jersey.resources;

import java.net.URI;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

public abstract class MCRJerseyResource {

    @Context
    protected UriInfo uriInfo;

    protected URI getBaseURI() {
        return uriInfo.getBaseUri();
    }

    public UriInfo getContext() {
        return this.uriInfo;
    }

}
