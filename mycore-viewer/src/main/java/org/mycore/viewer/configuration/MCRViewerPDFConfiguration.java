package org.mycore.viewer.configuration;

import javax.servlet.http.HttpServletRequest;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;

public class MCRViewerPDFConfiguration extends MCRViewerBaseConfiguration {

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        String pdfProviderURL = MCRConfiguration.instance().getString("MCR.Viewer.pdfProviderURL",
            MCRServlet.getServletBaseURL() + "MCRFileNodeServlet/{derivate}/{filePath}");
        String pdfWorkerLocation = MCRFrontendUtil.getBaseURL() + "modules/iview2/js/lib/pdf.min.worker.js";

        setProperty("pdfProviderURL", pdfProviderURL);
        setProperty("pdfWorkerURL", pdfWorkerLocation);
        // script
        addLocalScript("lib/pdf.js", false);
        addLocalScript("iview-client-pdf.js", isDebugParameterSet(request));

        return this;
    }

    @Override
    public String getDocType(HttpServletRequest request) {
        return "pdf";
    }

}
