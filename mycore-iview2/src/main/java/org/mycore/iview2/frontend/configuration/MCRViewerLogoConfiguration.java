package org.mycore.iview2.frontend.configuration;

        import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.iview2.services.MCRIView2Tools;

import javax.servlet.http.HttpServletRequest;

public class MCRViewerLogoConfiguration extends MCRViewerConfiguration {

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);
        String logoURL = MCRIView2Tools.getIView2Property("logo.URL");
        if (logoURL != null) {
            String framedParameter = request.getParameter("frame");
            if (framedParameter == null || !Boolean.parseBoolean(framedParameter)) {
                this.addLocalScript("iview-client-logo.js");
                this.setProperty("logoURL", MCRFrontendUtil.getBaseURL() + logoURL);

                String logoCssProperty = MCRIView2Tools.getIView2Property("logo.css");
                if (logoCssProperty != null) {
                    this.addCSS(MCRFrontendUtil.getBaseURL() + logoCssProperty);
                }
            }
        }

        return this;
    }

}
