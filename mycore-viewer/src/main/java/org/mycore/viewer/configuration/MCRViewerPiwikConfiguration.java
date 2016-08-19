package org.mycore.viewer.configuration;

import javax.servlet.http.HttpServletRequest;

import org.mycore.common.config.MCRConfiguration;

public class MCRViewerPiwikConfiguration extends MCRViewerConfiguration {

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);
        if (MCRConfiguration.instance().getBoolean("MCR.Piwik.enable", false)) {
            this.addLocalScript("iview-client-piwik.js", isDebugParameterSet(request));
            this.setProperty("MCR.Piwik.baseurl", MCRConfiguration.instance().getString("MCR.Piwik.baseurl"));
            this.setProperty("MCR.Piwik.id", MCRConfiguration.instance().getString("MCR.Piwik.id"));
        }
        return this;
    }

}
