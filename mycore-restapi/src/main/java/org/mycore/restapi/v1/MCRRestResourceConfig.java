package org.mycore.restapi.v1;

import org.glassfish.jersey.server.ResourceConfig;

public class MCRRestResourceConfig extends ResourceConfig {

    public MCRRestResourceConfig() {
        super();
        this.packages("org.mycore.restapi.v1");
    }

}
