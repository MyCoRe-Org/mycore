package org.mycore.viewer.configuration;

import javax.servlet.http.HttpServletRequest;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.servlets.MCRServlet;

public class MCRViewerMetsConfiguration extends MCRViewerBaseConfiguration {

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        // properties
        setProperty("metsURL", MCRServlet.getServletBaseURL() + "MCRMETSServlet/" + getDerivate(request));
        String imageXmlPath = MCRConfiguration.instance().getString("MCR.Viewer.BaseURL", null); // Parameter can be used to provide multiple urls

        if (imageXmlPath == null || imageXmlPath.isEmpty()) {
            imageXmlPath = MCRServlet.getServletBaseURL() + "MCRTileServlet/";
        }
        setProperty("tileProviderPath", imageXmlPath);
        if (imageXmlPath.contains(",")) {
            imageXmlPath = imageXmlPath.split(",")[0];
        }
        setProperty("imageXmlPath", imageXmlPath);

        setProperty("pdfCreatorStyle", MCRConfiguration.instance().getString("MCR.Viewer.PDFCreatorStyle", null));
        setProperty("pdfCreatorURI", MCRConfiguration.instance().getString("MCR.Viewer.PDFCreatorURI", null));
        setProperty("text.enabled", MCRConfiguration.instance().getString("MCR.Viewer.text.enabled", "false"));

        MCRConfiguration configuration = MCRConfiguration.instance();
        setProperty("pdfCreatorFormatString",
            configuration.getString("MCR.Viewer.PDFCreatorFormatString", null));
        setProperty("pdfCreatorRestrictionFormatString", configuration
            .getString("MCR.Viewer.PDFCreatorRestrictionFormatString", null));

        // script
        addLocalScript("iview-client-mets.js", isDebugParameterSet(request));

        return this;
    }

    @Override
    public String getDocType(HttpServletRequest request) {
        return "mets";
    }

}
