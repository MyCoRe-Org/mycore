package org.mycore.iview2.frontend.configuration;

import javax.servlet.http.HttpServletRequest;

/**
 * Default image viewer configuration. Decides if the mets or the pdf configuration is used.
 * Returns the appropriate configuration with all plugins (piwik, metadata, logo).
 * 
 * @author Matthias Eichner
 */
public class MCRIViewClientDefaultConfigurationStrategy implements MCRIViewClientConfigurationStrategy {

    @Override
    public MCRIViewClientConfiguration get(HttpServletRequest request) {
        if (isPDF(request)) {
            return getPDF(request);
        } else {
            return getMETS(request);
        }
    }

    protected boolean isPDF(HttpServletRequest request) {
        // well, this is the best test to check the type, for sure!
        String filePath = MCRIViewClientConfiguration.getFilePath(request);
        return filePath.toLowerCase().endsWith(".pdf");
    }

    protected MCRIViewClientConfiguration getPDF(HttpServletRequest request) {
        return MCRIViewClientConfigurationBuilder.pdf(request)
            .mixin(MCRIViewClientConfigurationBuilder.plugins(request).get()).get();
    }

    protected MCRIViewClientConfiguration getMETS(HttpServletRequest request) {
        return MCRIViewClientConfigurationBuilder.metsAndPlugins(request).get();
    }

}
