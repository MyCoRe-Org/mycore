package org.mycore.iview2.frontend.configuration;

import javax.servlet.http.HttpServletRequest;

import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.iview2.services.MCRIView2Tools;

public class MCRIViewClientPDFConfiguration extends MCRIViewClientBaseConfiguration {

    @Override
    public MCRIViewClientConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        String pdfProviderURL = MCRIView2Tools.getIView2Property("pdfProviderURL", MCRServlet.getServletBaseURL() + "MCRFileNodeServlet/{derivate}/{filePath}");
        
        setProperty("pdfProviderURL", pdfProviderURL);
        
        // script
        addLocalScript("lib/pdf.js", false);
        addLocalScript("iview-client-pdf.js");

        return this;
    }

    @Override
    public String getDocType(HttpServletRequest request) {
        return "pdf";
    }

}
