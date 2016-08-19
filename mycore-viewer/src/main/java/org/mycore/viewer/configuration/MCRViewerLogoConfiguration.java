package org.mycore.viewer.configuration;

import javax.servlet.http.HttpServletRequest;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.MCRFrontendUtil;

public class MCRViewerLogoConfiguration extends MCRViewerConfiguration {

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);
        String logoURL = MCRConfiguration.instance().getString("MCR.Viewer.logo.URL", null);
        if (logoURL != null) {
            String framedParameter = request.getParameter("frame");
            if (framedParameter == null || !Boolean.parseBoolean(framedParameter)) {
                this.addLocalScript("iview-client-logo.js", isDebugParameterSet(request));
                this.setProperty("logoURL", MCRFrontendUtil.getBaseURL() + logoURL);

                String logoCssProperty = MCRConfiguration.instance().getString("MCR.Viewer.logo.css", null);
                if (logoCssProperty != null) {
                    this.addCSS(MCRFrontendUtil.getBaseURL() + logoCssProperty);
                }
            }
        }

        return this;
    }

}
