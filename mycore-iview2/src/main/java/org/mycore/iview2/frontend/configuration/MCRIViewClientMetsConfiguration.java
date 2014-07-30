package org.mycore.iview2.frontend.configuration;

import javax.servlet.http.HttpServletRequest;

import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.iview2.services.MCRIView2Tools;

public class MCRIViewClientMetsConfiguration extends MCRIViewClientBaseConfiguration {

    @Override
    public MCRIViewClientConfiguration setup(HttpServletRequest request) {
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

        // script
        addLocalScript("iview-client-mets.js");

        return this;
    }

    @Override
    public String getDocType(HttpServletRequest request) {
        return "mets";
    }

}
