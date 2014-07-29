package org.mycore.iview2.frontend.configuration;

import javax.servlet.http.HttpServletRequest;

import org.mycore.iview2.services.MCRIView2Tools;

public class MCRIViewClientLogoConfiguration extends MCRIViewClientConfiguration {

    @Override
    public MCRIViewClientConfiguration setup(HttpServletRequest request) {
        super.setup(request);
        String logoURL = MCRIView2Tools.getIView2Property("logoURL");
        if (logoURL != null) {
            this.addLocalScript("iview-client-logo.js");
            this.setProperty("logoURL", logoURL);
        }
        return this;
    }

}
