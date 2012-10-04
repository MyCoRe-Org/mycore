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

    private static final MCRLayoutService SINGLETON = MCRLayoutService.instance();

    MCRDeprecatedLayoutService() {
    };

    /**
     * Sends a JDOM document as a response. 
     * @deprecated use {@link #sendXML(HttpServletRequest, HttpServletResponse, MCRContent)} instead.
     */
    @Deprecated
    public void sendXML(HttpServletRequest req, HttpServletResponse res, org.jdom.Document jdom) throws IOException {
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
    public void doLayout(HttpServletRequest req, HttpServletResponse res, org.jdom.Document jdom) throws IOException {
        SINGLETON.doLayout(req, res, new MCRJDOMContent(jdom));
    }

    /**
     * writes the transformation result directly into the Writer uses the
     * HttpServletResponse only for error messages
     * @deprecated will be removed without replacement
     */
    @Deprecated
    public void doLayout(HttpServletRequest req, HttpServletResponse res, Writer out, org.jdom.Document jdom) throws IOException {
        MCRContent content = new MCRJDOMContent(jdom);
        String docType = content.getDocType();
        MCRParameterCollector parameters = new MCRParameterCollector(req);
        String resourceName = SINGLETON.getResourceName(req, parameters, docType);
        if (resourceName == null) {
            new org.jdom.output.XMLOutputter().output(jdom, out);
        } else {
            Transformer transformer = MCRXSLTransformerFactory.getTransformer(new MCRTemplatesSource(resourceName));
            parameters.setParametersTo(transformer);
            try {
                transformer.transform(new org.jdom.transform.JDOMSource(jdom), new StreamResult(out));
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

}
