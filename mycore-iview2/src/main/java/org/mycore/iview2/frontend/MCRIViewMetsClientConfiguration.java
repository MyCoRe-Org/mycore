package org.mycore.iview2.frontend;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.iview2.services.MCRIView2Tools;

@XmlRootElement(name = "iviewClientConfiguration")
public class MCRIViewMetsClientConfiguration extends MCRIViewClientConfiguration {

    public MCRIViewMetsClientConfiguration(HttpServletRequest req) {
        super();
        this.mobile = isMobile(req);
        this.doctype = METS_DOCTYPE;

        this.derivate = req.getParameter("derivate");
        String servletBaseUrl = MCRServlet.getServletBaseURL();
        this.location = servletBaseUrl + "MCRMETSServlet/" + this.derivate;
        this.startImage = req.getParameter("startImage");

        this.imageXmlPath = MCRIView2Tools.getIView2Property("BaseURL");
        if (this.imageXmlPath == null || this.imageXmlPath.isEmpty()) {
            this.imageXmlPath = "MCRTileServlet/";
        } 

        this.tileProviderPath = this.imageXmlPath;
       
        if (this.imageXmlPath.contains(",")) {
            this.imageXmlPath = this.imageXmlPath.split(",")[0];
        }
    }

    public MCRIViewMetsClientConfiguration() {
    }

    private static final String METS_DOCTYPE = "mets";


    /**
     * The path where the iview-client can find the image.xml
     */
    @XmlElement()
    public String imageXmlPath;

    /**
     * The path where the iview-client can load tiles
     */
    @XmlElement()
    public String tileProviderPath;

    /**
     * The name of the derivate wich should be displayed
     */
    @XmlElement()
    public String derivate;

    /**
     * The image wich the iview-client should display
     */
    @XmlElement()
    public String startImage;
    
}
