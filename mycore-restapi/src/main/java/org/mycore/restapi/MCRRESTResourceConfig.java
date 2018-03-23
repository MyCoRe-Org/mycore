/**
 * 
 */
package org.mycore.restapi;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.jersey.MCRJerseyResourceConfig;
import org.mycore.restapi.feature.MCRRESTFeature;
import org.mycore.restapi.v1.errors.MCRRestAPIExceptionMapper;

/**
 * @author Sebastian Hofmann
 *
 */
public class MCRRESTResourceConfig extends MCRJerseyResourceConfig {

    public static final String REST_API_PACKAGE = "MCR.RestAPI.Resource.Packages";

    public MCRRESTResourceConfig() {
        super();
        register(MCRRestAPIExceptionMapper.class);
    }

    @Override
    protected void setupResources() {
        this.packages(MCRConfiguration.instance().getStrings(REST_API_PACKAGE).toArray(new String[0]));
    }

    @Override
    protected void setupFeatures() {
        super.setupFeatures();
        this.register(MCRRESTFeature.class);
    }
}
