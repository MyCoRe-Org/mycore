package org.mycore.iview2.frontend.configuration;

import javax.servlet.http.HttpServletRequest;

import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.iview2.services.MCRIView2Tools;

public class MCRViewerMetsConfiguration extends MCRViewerBaseConfiguration {

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        // properties
        setProperty("metsURL", MCRServlet.getServletBaseURL() + "MCRMETSServlet/" + getDerivate(request));
        String imageXmlPath = MCRIView2Tools.getIView2Property("BaseURL"); // Parameter can be used to provide multiple urls
        
        if (imageXmlPath == null || imageXmlPath.isEmpty()) {
            imageXmlPath = MCRServlet.getServletBaseURL() + "MCRTileServlet/";
        }
        setProperty("tileProviderPath", imageXmlPath);
        if (imageXmlPath.contains(",")) {
            imageXmlPath = imageXmlPath.split(",")[0];
        }
        setProperty("imageXmlPath", imageXmlPath);
        
        setProperty("pdfCreatorStyle", MCRIView2Tools.getIView2Property("PDFCreatorStyle"));
        setProperty("pdfCreatorURI", MCRIView2Tools.getIView2Property("PDFCreatorURI"));
        setProperty("text.enabled", MCRIView2Tools.getIView2Property("text.enabled", "false"));
        // script
        addLocalScript("iview-client-mets.js", isDebugParameterSet(request));

        return this;
    }

    @Override
    public String getDocType(HttpServletRequest request) {
        return "mets";
    }

}
