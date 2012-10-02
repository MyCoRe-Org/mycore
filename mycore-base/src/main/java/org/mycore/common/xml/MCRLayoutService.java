/*
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.common.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRDOMContent;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.fo.MCRFoFactory;
import org.mycore.common.fo.MCRFoFormatterInterface;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.common.xsl.MCRTemplatesSource;
import org.mycore.common.xsl.MCRXSLTransformerFactory;
import org.mycore.frontend.servlets.MCRServletJob;
import org.xml.sax.SAXParseException;

/**
 * Does the layout for other MyCoRe servlets by transforming XML input to
 * various output formats, using XSL stylesheets.
 * 
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRLayoutService {

    private final static Logger LOGGER = Logger.getLogger(MCRLayoutService.class);

    private static final MCRLayoutService SINGLETON = new MCRLayoutService();

    public static MCRLayoutService instance() {
        return SINGLETON;
    }

    /**
     * Sends a JDOM document as a response. 
     * @deprecated use {@link #sendXML(HttpServletRequest, HttpServletResponse, MCRContent)} instead.
     */
    @Deprecated
    public void sendXML(HttpServletRequest req, HttpServletResponse res, org.jdom.Document jdom) throws IOException {
        sendXML(req, res, new MCRJDOMContent(jdom));
    }

    /**
     * Sends a DOM document as a response. 
     * @deprecated use {@link #sendXML(HttpServletRequest, HttpServletResponse, MCRContent)} instead.
     */
    @Deprecated
    public void sendXML(HttpServletRequest req, HttpServletResponse res, org.w3c.dom.Document dom) throws IOException {
        sendXML(req, res, new MCRDOMContent(dom));
    }

    /**
     * Sends a document from an InputStream as a response. 
     * @deprecated use {@link #sendXML(HttpServletRequest, HttpServletResponse, MCRContent)} instead.
     */
    @Deprecated
    public void sendXML(HttpServletRequest req, HttpServletResponse res, InputStream in) throws IOException {
        sendXML(req, res, new MCRStreamContent(in));
    }

    public void sendXML(HttpServletRequest req, HttpServletResponse res, MCRContent xml) throws IOException {
        res.setContentType("text/xml; charset=UTF-8");
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            StreamResult result = new StreamResult(res.getOutputStream());
            transformer.transform(xml.getSource(), result);
        } catch (TransformerException e) {
            throw new MCRException(e);
        }
        res.flushBuffer();
    }

    /**
     * Sends a document from a File as a response. 
     * @deprecated use {@link #sendXML(HttpServletRequest, HttpServletResponse, MCRContent)} instead.
     */
    @Deprecated
    public void sendXML(HttpServletRequest req, HttpServletResponse res, File file) throws IOException {
        sendXML(req, res, new MCRFileContent(file));
    }

    /**
     * 
     * @deprecated use {@link #doLayout(HttpServletRequest, HttpServletResponse, MCRContent)} instead.
     */
    @Deprecated
    public void doLayout(HttpServletRequest req, HttpServletResponse res, org.jdom.Document jdom) throws IOException {
        doLayout(req, res, new MCRJDOMContent(jdom));
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
        String resourceName = getResourceName(req, parameters, docType);
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
        doLayout(req, res, new MCRDOMContent(dom));
    }

    /**
     * 
     * @deprecated use {@link #doLayout(HttpServletRequest, HttpServletResponse, MCRContent)} instead.
     */
    @Deprecated
    public void doLayout(HttpServletRequest req, HttpServletResponse res, InputStream is) throws IOException, SAXParseException {
        doLayout(req, res, new MCRStreamContent(is));
    }

    public void doLayout(HttpServletRequest req, HttpServletResponse res, MCRContent con) throws IOException {
        String docType = con.getDocType();
        MCRParameterCollector parameters = new MCRParameterCollector(req);
        String resourceName = getResourceName(req, parameters, docType);
        if (resourceName == null) {
            sendXML(req, res, con);
        } else {
            transform(res, con.getSource(), docType, parameters,
                    MCRXSLTransformerFactory.getTransformer(new MCRTemplatesSource(resourceName)));
        }
    }

    /**
     * 
     * @deprecated use {@link #doLayout(HttpServletRequest, HttpServletResponse, MCRContent)} instead.
     */
    @Deprecated
    public void doLayout(HttpServletRequest req, HttpServletResponse res, File file) throws IOException {
        doLayout(req, res, new MCRFileContent(file));
    }

    public DOMSource doLayout(Document doc, String stylesheetName, Hashtable<String, String> params) throws Exception {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        MCRServletJob job = (MCRServletJob) mcrSession.get("MCRServletJob");
        Transformer transformer = MCRXSLTransformerFactory.getTransformer(new MCRTemplatesSource(stylesheetName));
        MCRParameterCollector parameters = job == null ? new MCRParameterCollector() : new MCRParameterCollector(job.getRequest());
        if (params != null)
            for (Map.Entry<String, String> entry : params.entrySet()) {
                parameters.setParameter(entry.getKey(), entry.getValue());
            }

        parameters.setParametersTo(transformer);
        DOMResult out = new DOMResult();
        //temporarily workaround as JDOMResult does not work with xsl:output
        transformer.transform(new JDOMSource(doc), out);
        return new DOMSource(out.getNode());
    }

    private void transform(HttpServletResponse res, Source sourceXML, String docType, MCRParameterCollector parameters,
            Transformer transformer) throws IOException {
        parameters.setParametersTo(transformer);

        try {
            transform(sourceXML, transformer, res);
        } catch (IOException ex) {
            LOGGER.error("IOException while XSL-transforming XML document", ex);
        } catch (MCRException ex) {
            // Check if it is an error page to suppress later recursively
            // generating an error page when there is an error in the stylesheet
            if (!"mcr_error".equals(docType)) {
                throw ex;
            }

            String msg = "Error while generating error page!";
            LOGGER.warn(msg, ex);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        }
    }

    /**
     * Transforms XML input with the given XSL stylesheet and sends the output
     * as HTTP Servlet Response to the client browser.
     * 
     * @param xml
     *            the XML input document
     * @param xsl
     *            the compiled XSL stylesheet
     * @param transformer
     *            the XSL transformer to use
     * @param response
     *            the response object to send the result to
     */
    private void transform(Source xml, Transformer transformer, HttpServletResponse response) throws IOException, MCRException {

        // Set content type from "<xsl:output media-type = "...">
        // Set char encoding from "<xsl:output encoding = "...">
        String ct = transformer.getOutputProperty("media-type");
        String enc = transformer.getOutputProperty("encoding");
        response.setCharacterEncoding(enc);
        response.setContentType(ct + "; charset=" + enc);

        LOGGER.debug("MCRLayoutService starts to output " + response.getContentType());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Result result = new StreamResult(out);
        try {
            transformer.transform(xml, result);
        } catch (TransformerException ex) {
            String msg = "Error while transforming XML using XSL stylesheet: " + ex.getMessageAndLocation();
            throw new MCRException(msg, ex);
        } finally {
            out.close();
        }

        OutputStream sos = response.getOutputStream();
        byte[] bytes = out.toByteArray();

        if ("application/pdf".equals(ct)) // extra XSL-FO step
        {
            MCRFoFormatterInterface fopper = MCRFoFactory.getFoFormatter();
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            try {
                fopper.transform(in, sos);
                in.close();
                sos.close();
            } catch (Exception ex) {
                throw new MCRException("Could not render XSL-FO to PDF", ex);
            }
        } else {
            sos.write(bytes);
            sos.close();
        }
    }

    /**
     * @param xml the document source to transform
     * @param resourceName the name of the stylesheet to invoke (fullqualified name)
     * 
     * @return {@link JDOMResult} or null if either xml or resourceName parameter is null
     * 
     * @throws IOException
     */
    public JDOMResult doLayout(Source xml, String resourceName) throws IOException {
        if (xml == null || resourceName == null) {
            return null;
        }
        return transform(xml, MCRXSLTransformerFactory.getTransformer(new MCRTemplatesSource(resourceName)));
    }

    /**
     * @param xml
     * @param transformer
     * @return
     * @throws IOException
     * @throws MCRException
     */
    private JDOMResult transform(Source xml, Transformer transformer) throws IOException, MCRException {
        JDOMResult result = new JDOMResult();
        try {
            transformer.transform(xml, result);
        } catch (TransformerException ex) {
            LOGGER.error("Error while transforming XML using XSL stylesheet: " + ex.getMessageAndLocation(), ex);
        }
        return result;
    }

    private String getResourceName(HttpServletRequest req, MCRParameterCollector parameters, String docType) {
        String style = parameters.getParameter("Style", "default");
        LOGGER.debug("MCRLayoutService using style " + style);

        String styleName = buildStylesheetName(docType, style);
        boolean resourceExist = false;
        try {
            resourceExist = MCRXMLResource.instance().exists(styleName, this.getClass().getClassLoader());
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
    private String buildStylesheetName(String docType, String style) {
        StringBuffer filename = new StringBuffer("xsl/").append(docType);

        if (!"default".equals(style)) {
            filename.append("-");
            filename.append(style);
        }

        filename.append(".xsl");

        return filename.toString();
    }
}
