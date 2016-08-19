package org.mycore.viewer.configuration;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

/**
 * Default image viewer configuration. Decides if the mets or the pdf configuration is used.
 * Returns the appropriate configuration with all plugins (piwik, metadata, logo).
 * 
 * @author Matthias Eichner
 */
public class MCRViewerDefaultConfigurationStrategy implements MCRViewerConfigurationStrategy {

    @Override
    public MCRViewerConfiguration get(HttpServletRequest request) {
        if (isPDF(request)) {
            return getPDF(request);
        } else {
            return getMETS(request);
        }
    }

    protected boolean isPDF(HttpServletRequest request) {
        // well, this is the best test to check the type, for sure!
        String filePath = MCRViewerConfiguration.getFilePath(request);
        return filePath != null && filePath.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    protected MCRViewerConfiguration getPDF(HttpServletRequest request) {
        return MCRViewerConfigurationBuilder.pdf(request).mixin(MCRViewerConfigurationBuilder.plugins(request).get())
            .get();
    }

    protected MCRViewerConfiguration getMETS(HttpServletRequest request) {
        return MCRViewerConfigurationBuilder.metsAndPlugins(request).get();
    }

}
