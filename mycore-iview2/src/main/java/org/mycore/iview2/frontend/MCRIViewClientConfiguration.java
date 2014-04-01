package org.mycore.iview2.frontend;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.mycore.common.MCRLanguageDetector;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.common.content.MCRXMLContent;
import org.mycore.datamodel.language.MCRLanguage;
import org.mycore.iview2.services.MCRIView2Tools;

import com.google.gson.Gson;

@XmlRootElement(name = "iviewClientConfiguration")
abstract class MCRIViewClientConfiguration {

    public MCRIViewClientConfiguration() {
        this.i18nPath = "/servlets/MCRLocaleServlet/{lang}/component.iview2.*";
        this.lang = "de";
        this.pdfCreatorStyle = MCRIView2Tools.getIView2Property("PDFCreatorStyle");
        this.pdfCreatorURI = MCRIView2Tools.getIView2Property("PDFCreatorURI");
    }

    /**
    * Should the mobile or the desktop client started
    */
    @XmlElement()
    public Boolean mobile;

    /**
     *  The type of the structure(mets /pdf)
     */
    @XmlElement()
    public String doctype;

    /**
     * The location of the Structure (mets.xml / document.pdf)
     */
    @XmlElement()
    public String location;

    /**
     * The location where the iview-client can load the i18n.json
     */
    @XmlElement()
    public String i18nPath;

    /**
     * [default = en]
     * The language 
     */
    @XmlElement()
    public String lang;

    /**
     * [optional] 
     */
    @XmlElement()
    public String pdfCreatorURI;

    /**
     * [optional]
     */
    @XmlElement()
    public String pdfCreatorStyle;

    public String toJSON() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public MCRXMLContent toXMLContent() throws JAXBException {
        MCRJAXBContent<MCRIViewClientConfiguration> config = new MCRJAXBContent<MCRIViewClientConfiguration>(
            JAXBContext.newInstance(MCRIViewClientConfiguration.class, MCRIViewMetsClientConfiguration.class), this);
        return config;
    }
    
    public static boolean isMobile(HttpServletRequest req){
        return req.getHeader("User-Agent").indexOf("Mobile") != -1;
    }

}
