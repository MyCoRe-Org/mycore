package org.mycore.iview2.frontend.configuration;

import javax.servlet.http.HttpServletRequest;

import org.mycore.iview2.services.MCRIView2Tools;

public class MCRIViewClientPDFConfiguration extends MCRIViewClientBaseConfiguration {

    @Override
    public MCRIViewClientConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        // properties
        setProperty("pdfCreatorStyle", MCRIView2Tools.getIView2Property("PDFCreatorStyle"));
        setProperty("pdfCreatorURI", MCRIView2Tools.getIView2Property("PDFCreatorURI"));

        // script
        addLocalScript("iview-client-pdf.js");

        return this;
    }

    @Override
    public String getDocType(HttpServletRequest request) {
        return "pdf";
    }

}
