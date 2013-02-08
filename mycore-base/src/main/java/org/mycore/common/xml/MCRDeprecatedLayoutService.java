package org.mycore.common.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRDOMContent;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.common.xsl.MCRTemplatesSource;
import org.mycore.common.xsl.MCRXSLTransformerFactory;
import org.xml.sax.SAXParseException;

public class MCRDeprecatedLayoutService {
    final static Logger LOGGER = Logger.getLogger(MCRDeprecatedLayoutService.class);

    private static final MCRLayoutService SINGLETON = new MCRLayoutService();

    MCRDeprecatedLayoutService() {
    }

    /**
     * Sends a JDOM document as a response. 
     * @deprecated use {@link #sendXML(HttpServletRequest, HttpServletResponse, MCRContent)} instead.
     */
    @Deprecated
    public void sendXML(HttpServletRequest req, HttpServletResponse res, org.jdom2.Document jdom) throws IOException {
        SINGLETON.sendXML(req, res, new MCRJDOMContent(jdom));
    }

    /**
     * Sends a DOM document as a response. 
     * @deprecated use {@link #sendXML(HttpServletRequest, HttpServletResponse, MCRContent)} instead.
     */
    @Deprecated
    public void sendXML(HttpServletRequest req, HttpServletResponse res, org.w3c.dom.Document dom) throws IOException {
        SINGLETON.sendXML(req, res, new MCRDOMContent(dom));
    }

    /**
     * Sends a document from an InputStream as a response. 
     * @deprecated use {@link #sendXML(HttpServletRequest, HttpServletResponse, MCRContent)} instead.
     */
    @Deprecated
    public void sendXML(HttpServletRequest req, HttpServletResponse res, InputStream in) throws IOException {
        SINGLETON.sendXML(req, res, new MCRStreamContent(in));
    }

    /**
     * Sends a document from a File as a response. 
     * @deprecated use {@link #sendXML(HttpServletRequest, HttpServletResponse, MCRContent)} instead.
     */
    @Deprecated
    public void sendXML(HttpServletRequest req, HttpServletResponse res, File file) throws IOException {
        SINGLETON.sendXML(req, res, new MCRFileContent(file));
    }

    /**
     * 
     * @deprecated use {@link #doLayout(HttpServletRequest, HttpServletResponse, MCRContent)} instead.
     */
    @Deprecated
    public void doLayout(HttpServletRequest req, HttpServletResponse res, org.jdom2.Document jdom) throws IOException {
        SINGLETON.doLayout(req, res, new MCRJDOMContent(jdom));
    }

    /**
     * writes the transformation result directly into the Writer uses the
     * HttpServletResponse only for error messages
     * @deprecated will be removed without replacement
     */
    @Deprecated
    public void doLayout(HttpServletRequest req, HttpServletResponse res, Writer out, org.jdom2.Document jdom) throws IOException {
        MCRContent content = new MCRJDOMContent(jdom);
        String docType = content.getDocType();
        MCRParameterCollector parameters = new MCRParameterCollector(req);
        String resourceName = getResourceName(req, parameters, docType);
        if (resourceName == null) {
            new org.jdom2.output.XMLOutputter().output(jdom, out);
        } else {
            try {
                Transformer transformer = MCRXSLTransformerFactory.getTransformer(new MCRTemplatesSource(resourceName));
                parameters.setParametersTo(transformer);
                transformer.transform(new org.jdom2.transform.JDOMSource(jdom), new StreamResult(out));
            } catch (TransformerException ex) {
                String msg = "Error while transforming XML using XSL stylesheet: " + ex.getMessageAndLocation();
                throw new MCRException(msg, ex);
            } catch (MCRException ex) {
                // Check if it is an error page to suppress later recursively
                // generating an error page when there is an error in the
                // stylesheet
                if (!"mcr_error".equals(docType)) {
                    throw ex;
                }
                String msg = "Error while generating error page!";
                LOGGER.warn(msg, ex);
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            }
        }
    }

    /**
     * 
     * @deprecated use {@link #doLayout(HttpServletRequest, HttpServletResponse, MCRContent)} instead.
     */
    @Deprecated
    public void doLayout(HttpServletRequest req, HttpServletResponse res, org.w3c.dom.Document dom) throws IOException, SAXParseException {
        SINGLETON.doLayout(req, res, new MCRDOMContent(dom));
    }

    /**
     * 
     * @deprecated use {@link #doLayout(HttpServletRequest, HttpServletResponse, MCRContent)} instead.
     */
    @Deprecated
    public void doLayout(HttpServletRequest req, HttpServletResponse res, InputStream is) throws IOException, SAXParseException {
        SINGLETON.doLayout(req, res, new MCRStreamContent(is));
    }

    /**
     * 
     * @deprecated use {@link #doLayout(HttpServletRequest, HttpServletResponse, MCRContent)} instead.
     */
    @Deprecated
    public void doLayout(HttpServletRequest req, HttpServletResponse res, File file) throws IOException {
        SINGLETON.doLayout(req, res, new MCRFileContent(file));
    }

    private static String getResourceName(HttpServletRequest req, MCRParameterCollector parameters, String docType) {
        String style = parameters.getParameter("Style", "default");
        LOGGER.debug("MCRLayoutService using style " + style);

        String styleName = buildStylesheetName(docType, style);
        boolean resourceExist = false;
        try {
            resourceExist = MCRXMLResource.instance().exists(styleName, MCRDeprecatedLayoutService.class.getClassLoader());
            if (resourceExist) {
                return styleName;
            }
        } catch (Exception e) {
            throw new MCRException("Error while loading stylesheet: " + styleName, e);
        }

        // If no stylesheet exists, forward raw xml instead
        // You can transform raw xml code by providing a stylesheed named
        // [doctype]-xml.xsl now
        if (style.equals("xml") || style.equals("default")) {
            return null;
        }
        throw new MCRException("XSL stylesheet not found: " + styleName);
    }

    /**
     * Builds the filename of the stylesheet to use, e. g. "playlist-simple.xsl"
     */
    private static String buildStylesheetName(String docType, String style) {
        StringBuilder filename = new StringBuilder("xsl/").append(docType);

        if (!"default".equals(style)) {
            filename.append("-");
            filename.append(style);
        }

        filename.append(".xsl");

        return filename.toString();
    }

}
