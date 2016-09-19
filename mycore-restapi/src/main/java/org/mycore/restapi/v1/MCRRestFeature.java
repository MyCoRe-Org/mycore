package org.mycore.restapi.v1;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import org.mycore.frontend.jersey.feature.MCRJerseyDefaultFeature;

@Provider
public class MCRRestFeature extends MCRJerseyDefaultFeature {

    @Override
    protected List<String> getPackages() {
        return Arrays.asList("org.mycore.restapi.v1");
    }

    @Override
    protected void registerSessionHookFilter(FeatureContext context) {
        // don't register session hook
    }

    @Override
    protected void registerTransactionFilter(FeatureContext context) {
        // don't register transaction filter
    }

}
