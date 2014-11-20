package org.mycore.iview2.frontend.configuration;

import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.iview2.services.MCRIView2Tools;

import javax.servlet.http.HttpServletRequest;

public class MCRViewerPDFConfiguration extends MCRViewerBaseConfiguration {

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        String pdfProviderURL = MCRIView2Tools.getIView2Property("pdfProviderURL", MCRServlet.getServletBaseURL() + "MCRFileNodeServlet/{derivate}/{filePath}");
        String pdfWorkerLocation = MCRFrontendUtil.getBaseURL() + "modules/iview2/js/lib/pdf.min.worker.js";

        setProperty("pdfProviderURL", pdfProviderURL);
        setProperty("pdfWorkerURL", pdfWorkerLocation);
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
