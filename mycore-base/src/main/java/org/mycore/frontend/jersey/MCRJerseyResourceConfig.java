package org.mycore.frontend.jersey;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.mycore.common.config.MCRConfiguration;

public class MCRJerseyResourceConfig extends ResourceConfig {

    public MCRJerseyResourceConfig() {
        super();
        
        // multi part
        this.register(MultiPartFeature.class);
        
        // register all packages from MCRConfiguratin
        String propertyString = MCRConfiguration.instance().getString("MCR.Jersey.resource.packages",
            "org.mycore.frontend.jersey.resources");
        this.packages(propertyString.split(","));
        
        // 
        this.packages("org.mycore.frontend.jersey.feature");
    }

}
