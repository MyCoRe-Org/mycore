/**
 * 
 */
package org.mycore.restapi;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.mycore.common.config.MCRConfiguration;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRRESTResourceConfig extends ResourceConfig {

    protected String[] packages;

    public MCRRESTResourceConfig() {
        super();
        String[] restPackages = MCRConfiguration.instance()
            .getStrings("MCR.RestAPI.Resource.Packages")
            .stream()
            .toArray(String[]::new);
        packages(restPackages);
        register(MultiPartFeature.class);
    }

}
