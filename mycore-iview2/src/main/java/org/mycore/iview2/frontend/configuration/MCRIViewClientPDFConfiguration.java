package org.mycore.iview2.frontend.configuration;

import javax.servlet.http.HttpServletRequest;

public class MCRIViewClientPDFConfiguration extends MCRIViewClientBaseConfiguration {

    @Override
    public MCRIViewClientConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        // script
        addLocalScript("lib/pdf.js");
        addLocalScript("iview-client-pdf.js");

        return this;
    }

    @Override
    public String getDocType(HttpServletRequest request) {
        return "pdf";
    }

}
