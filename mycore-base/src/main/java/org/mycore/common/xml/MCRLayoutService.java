/*
 * 
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.apache.xalan.templates.ElemTemplate;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.trace.GenerateEvent;
import org.apache.xalan.trace.SelectionEvent;
import org.apache.xalan.trace.TraceManager;
import org.apache.xalan.trace.TracerEvent;
import org.apache.xml.utils.WrappedRuntimeException;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.fo.MCRFoFormatterInterface;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs2.MCRContent;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Does the layout for other MyCoRe servlets by transforming XML input to
 * various output formats, using XSL stylesheets.
 * 
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date: 2008-05-21 15:53:52 +0200 (Mi, 21. Mai
 *          2008) $
 */
public class MCRLayoutService implements org.apache.xalan.trace.TraceListener {

    /** A cache of already compiled stylesheets */
    private static MCRCache STYLESHEETS_CACHE = new MCRCache(MCRConfiguration.instance().getInt("MCR.LayoutService.XSLCacheSize", 100), "XSLT Stylesheets");

    private static MCRXMLResource XML_RESOURCE = MCRXMLResource.instance();

    /** The XSL transformer factory to use */
    private SAXTransformerFactory factory;

    private MCRFoFormatterInterface fo_formatter;

    private final static Logger LOGGER = Logger.getLogger(MCRLayoutService.class);

    private static final MCRLayoutService SINGLETON = new MCRLayoutService();

    private ThreadLocal<HashMap<String, String>> transformMap = new ThreadLocal<HashMap<String, String>>() {

        @Override
        protected HashMap<String, String> initialValue() {
            return new HashMap<String, String>();
        }

    };

    public static MCRLayoutService instance() {
        return SINGLETON;
    }

    private MCRLayoutService() {
        System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
        TransformerFactory tf = TransformerFactory.newInstance();
        LOGGER.info("Transformerfactory: " + tf.getClass().getName());

        if (!tf.getFeature(SAXTransformerFactory.FEATURE)) {
            throw new MCRConfigurationException("Could not load a SAXTransformerFactory for use with XSLT");
        }

        factory = (SAXTransformerFactory) tf;
        factory.setURIResolver(MCRURIResolver.instance());
        factory.setErrorListener(new ErrorListener() {
            public void error(TransformerException ex) {
                throw new WrappedRuntimeException(getRealException(ex));
            }

            public void fatalError(TransformerException ex) {
                throw new WrappedRuntimeException(getRealException(ex));
            }

            public void warning(TransformerException ex) {
                LOGGER.warn(ex.getMessageAndLocation());
            }

            private Exception getRealException(TransformerException tex) {
                if (tex.getException() != null) {
                    Throwable cause = tex.getException();
                    while (cause instanceof WrappedRuntimeException) {
                        cause = ((WrappedRuntimeException) cause).getException();
                    }
                    if (cause instanceof TransformerException)
                        return getRealException((TransformerException) cause);
                }
                return tex;
            }

        });
        String fo_class = MCRConfiguration.instance().getString("MCR.LayoutService.FoFormatter.class", "org.mycore.common.fo.MCRFoFormatterFOP");
        try {
            @SuppressWarnings("unchecked")
            Class<? extends MCRFoFormatterInterface> clazz = (Class<? extends MCRFoFormatterInterface>) Class.forName(fo_class);
            fo_formatter = clazz.newInstance();
            LOGGER.debug("Using formatter instance " + fo_class);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Class " + fo_class + " not found!");
            e.printStackTrace();
        } catch (ClassCastException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    public Map<String, String> getCurrentTransformationMap() {
        return transformMap.get();
    }

    public void sendXML(HttpServletRequest req, HttpServletResponse res, org.jdom.Document jdom) throws IOException {
        sendXML(req, res, MCRContent.readFrom(jdom));
    }

    public void sendXML(HttpServletRequest req, HttpServletResponse res, org.w3c.dom.Document dom) throws IOException {
        sendXML(req, res, new org.jdom.input.DOMBuilder().build(dom));
    }

    public void sendXML(HttpServletRequest req, HttpServletResponse res, InputStream in) throws IOException {
        sendXML(req, res, MCRContent.readFrom(in));
    }

    private void sendXML(HttpServletRequest req, HttpServletResponse res, MCRContent xml) throws IOException {
        res.setContentType("text/xml; charset=UTF-8");
        XMLOutputter xout = new XMLOutputter();
        xout.setFormat(Format.getRawFormat().setEncoding("UTF-8"));
        OutputStream out = res.getOutputStream();
        try {
            xout.output(xml.asXML(), out);
        } catch (JDOMException ex) {
            throw new MCRException("Output is not XML", ex);
        }
        out.close();
    }

    public void sendXML(HttpServletRequest req, HttpServletResponse res, File file) throws IOException {
        sendXML(req, res, MCRContent.readFrom(file));
    }

    public void doLayout(HttpServletRequest req, HttpServletResponse res, org.jdom.Document jdom) throws IOException {
        String docType = jdom.getDocType() == null ? jdom.getRootElement().getName() : jdom.getDocType().getElementName();
        Properties parameters = buildXSLParameters(req);
        String resourceName = getResourceName(req, parameters, docType);
        if (resourceName == null) {
            sendXML(req, res, jdom);
        } else {
            transform(res, new org.jdom.transform.JDOMSource(jdom), docType, parameters, resourceName);
        }
    }

    /**
     * writes the transformation result directly into the Writer uses the
     * HttpServletResponse only for error messages
     */
    public void doLayout(HttpServletRequest req, HttpServletResponse res, Writer out, org.jdom.Document jdom) throws IOException {
        String docType = jdom.getDocType() == null ? jdom.getRootElement().getName() : jdom.getDocType().getElementName();
        Properties parameters = buildXSLParameters(req);
        String resourceName = getResourceName(req, parameters, docType);
        if (resourceName == null) {
            new org.jdom.output.XMLOutputter().output(jdom, out);
        } else {
            Templates stylesheet = buildCompiledStylesheet(resourceName);
            Transformer transformer = buildTransformer(stylesheet);
            setXSLParameters(transformer, parameters);
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

    public void doLayout(HttpServletRequest req, HttpServletResponse res, org.w3c.dom.Document dom) throws IOException {
        String docType = dom.getDoctype() == null ? dom.getDocumentElement().getLocalName() : dom.getDoctype().getName();
        Properties parameters = buildXSLParameters(req);
        String resourceName = getResourceName(req, parameters, docType);
        if (resourceName == null) {
            sendXML(req, res, dom);
        } else {
            transform(res, new DOMSource(dom), docType, parameters, resourceName);
        }
    }

    public void doLayout(HttpServletRequest req, HttpServletResponse res, InputStream is) throws IOException {
        MCRContentInputStream cis = new MCRContentInputStream(is);
        String docType = MCRUtils.parseDocumentType(new ByteArrayInputStream(cis.getHeader()));
        int pos = docType.indexOf(':') + 1;
        if (pos > 0) {
            //filter namespace prefix
            docType = docType.substring(pos);
        }

        Properties parameters = buildXSLParameters(req);
        String resourceName = getResourceName(req, parameters, docType);
        if (resourceName == null) {
            sendXML(req, res, cis);
        } else {
            transform(res, new StreamSource(cis), docType, parameters, resourceName);
        }
    }

    public void doLayout(HttpServletRequest req, HttpServletResponse res, File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        doLayout(req, res, fis);
        fis.close();
    }

    public DOMSource doLayout(Document doc, String stylesheetName, Hashtable<String, String> params) throws Exception {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        MCRServletJob job = (MCRServletJob) mcrSession.get("MCRServletJob");
        Templates stylesheet = buildCompiledStylesheet(stylesheetName);
        Transformer transformer = buildTransformer(stylesheet);
        Properties parameters;
        if (job != null) {
            parameters = buildXSLParameters(job.getRequest());
        } else {
            parameters = new Properties();
        }
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                parameters.put(entry.getKey(), entry.getValue());
            }
        }
        setXSLParameters(transformer, parameters);
        DOMResult out = new DOMResult();
        //temporarily workaround as JDOMResult does not work with xsl:output
        transformer.transform(new JDOMSource(doc), out);
        return new DOMSource(out.getNode());
    }

    private void transform(HttpServletResponse res, Source sourceXML, String docType, Properties parameters, Templates stylesheet) throws IOException {
        Transformer transformer = buildTransformer(stylesheet);
        setXSLParameters(transformer, parameters);

        try {
            transform(sourceXML, stylesheet, transformer, res);
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

    private void transform(HttpServletResponse res, Source sourceXML, String docType, Properties parameters, String resourceName) throws IOException {
        Templates stylesheet = buildCompiledStylesheet(resourceName);
        transform(res, sourceXML, docType, parameters, stylesheet);
    }

    private String getResourceName(HttpServletRequest req, Properties parameters, String docType) {
        String style = parameters.getProperty("Style", "default");
        LOGGER.debug("MCRLayoutService using style " + style);

        String styleName = buildStylesheetName(docType, style);
        boolean resourceExist = false;
        try {
            resourceExist = XML_RESOURCE.exists(styleName, this.getClass().getClassLoader());
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

    public static Properties buildXSLParameters(HttpServletRequest request) {
        // PROPERTIES: Read all properties from system configuration
        Properties parameters = (Properties) MCRConfiguration.instance().getProperties().clone();
        // added properties of MCRSession and request
        if (request != null) {
            parameters.putAll(mergeProperties(request));

            // handle HttpSession
            HttpSession session = request.getSession(false);
            if (session != null) {
                String jSessionID = MCRConfiguration.instance().getString("MCR.Session.Param", ";jsessionid=");
                if (!request.isRequestedSessionIdFromCookie()) {
                    parameters.put("HttpSession", jSessionID + session.getId());
                }
                parameters.put("JSessionID", jSessionID + session.getId());
            }
        }

        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String uid = mcrSession.getUserInformation().getUserID();
        // set parameters
        parameters.put("CurrentUser", uid);
        parameters.put("DefaultLang", MCRConfiguration.instance().getString("MCR.Metadata.DefaultLang", MCRConstants.DEFAULT_LANG));
        parameters.put("CurrentLang", mcrSession.getCurrentLanguage());
        parameters.put("WebApplicationBaseURL", MCRServlet.getBaseURL());
        parameters.put("ServletsBaseURL", MCRServlet.getServletBaseURL());

        if (request != null) {
            parameters.put("RequestURL", getCompleteURL(request));
            parameters.put("Referer", request.getHeader("Referer") != null ? request.getHeader("Referer") : "");
        }

        LOGGER.debug("LayoutServlet XSL.MCRSessionID=" + parameters.getProperty("MCRSessionID"));
        LOGGER.debug("LayoutServlet XSL.CurrentUser =" + uid);
        LOGGER.debug("LayoutServlet HttpSession =" + parameters.getProperty("HttpSession"));
        LOGGER.debug("LayoutServlet JSessionID =" + parameters.getProperty("JSessionID"));
        LOGGER.debug("LayoutServlet RefererURL =" + parameters.getProperty("Referer"));

        return parameters;
    }

    /**
     * returns a merged list of XSL parameters.
     * 
     * First parameters stored in current HttpSession are used. These are
     * overwritten by Parameters of MCRSession. Finally parameters, then
     * attributes of HttpServletRequest overwrite the previously defined.
     * 
     * @param request
     * @return merged XSL.* properties of MCR|HttpSession and HttpServletRequest
     */
    private final static Properties mergeProperties(HttpServletRequest request) {
        Properties props = new Properties();

        HttpSession session = request.getSession(false);
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        if (session != null) {
            for (@SuppressWarnings("unchecked")
            Enumeration<String> e = session.getAttributeNames(); e.hasMoreElements();) {
                String name = e.nextElement();
                if (name.startsWith("XSL.")) {
                    props.put(name.substring(4), session.getAttribute(name));
                }
            }
        }
        for (Map.Entry<Object, Object> entry : mcrSession.getMapEntries()) {
            String key = entry.getKey().toString();
            if (key.startsWith("XSL.")) {
                props.put(key.substring(4), entry.getValue());
            }
        }
        for (@SuppressWarnings("unchecked")
        Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            if (name.startsWith("XSL.") && !name.endsWith(".SESSION")) {
                props.put(name.substring(4), request.getParameter(name));
            }
        }
        for (@SuppressWarnings("unchecked")
        Enumeration<String> e = request.getAttributeNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            if (name.startsWith("XSL.") && !name.endsWith(".SESSION")) {
                final Object attributeValue = request.getAttribute(name);
                if (attributeValue != null) {
                    props.put(name.substring(4), attributeValue.toString());
                }
            }
        }
        return props;
    }

    private final static String getCompleteURL(HttpServletRequest request) {
        //when called by MCRErrorServlet
        String errorURI = (String) request.getAttribute("javax.servlet.error.request_uri");
        //assemble URL with baseUrl so that mod_proxy request are supported
        StringBuilder buffer = new StringBuilder(MCRServlet.getBaseURL());
        int pos = buffer.indexOf("/", "https://".length());
        buffer.delete(pos, buffer.length()); //get baseUrl up to hostname
        buffer.append(errorURI != null ? errorURI : request.getRequestURI());
        String queryString = request.getQueryString();
        if (queryString != null && queryString.length() > 0) {
            buffer.append("?").append(queryString);
        }
        LOGGER.debug("Complete request URL : " + buffer.toString());
        return buffer.toString();
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

    private Templates buildCompiledStylesheet(String resource) {
        Templates stylesheet = null;
        try {
            stylesheet = (Templates) STYLESHEETS_CACHE.getIfUpToDate(resource, XML_RESOURCE.getLastModified(resource, this.getClass().getClassLoader()));
        } catch (IOException e) {
            LOGGER.warn("Could not determine last modified date of resource " + resource);
        }
        if (stylesheet == null) {
            try {
                //have to use SAX here to resolve entities
                XMLReader reader = XMLReaderFactory.createXMLReader();
                reader.setEntityResolver(MCRURIResolver.instance());
                URL resourceURL = this.getClass().getClassLoader().getResource(resource);
                InputSource input = new InputSource(resourceURL.toString());
                SAXSource source = new SAXSource(reader, input);
                stylesheet = factory.newTemplates(source);
                LOGGER.debug("MCRLayoutService compiled stylesheet resource " + resource);
            } catch (Exception exc) {
                reportCompileError(resource, exc);
            }
            STYLESHEETS_CACHE.put(resource, stylesheet);
        } else {
            LOGGER.debug("MCRLayoutService using cached stylesheet " + resource);
        }

        return stylesheet;
    }

    private void reportCompileError(String resource, Exception exc) {
        StringBuffer msg = new StringBuffer("Error compiling XSL stylesheet ");
        msg.append(resource);

        Exception cause = exc;
        while (cause instanceof WrappedRuntimeException) {
            cause = ((WrappedRuntimeException) cause).getException();
        }
        if (cause instanceof TransformerException)
            cause = getRealException((TransformerException) cause);
        else if (cause instanceof MCRException)
            cause = getRealException((MCRException) cause);

        if (cause instanceof TransformerException) {
            TransformerException tex = (TransformerException) cause;
            msg.append("\n").append(tex.getMessage());
            SourceLocator sl = tex.getLocator();
            if (sl != null) {
                msg.append(" at line ").append(sl.getLineNumber()).append(" column ").append(sl.getColumnNumber());
            }
        }
        throw new MCRConfigurationException(msg.toString(), cause);
    }

    private Exception getRealException(TransformerException exc) {
        if (exc.getException() != null) {
            Throwable cause = exc.getException();
            while (cause instanceof WrappedRuntimeException) {
                cause = ((WrappedRuntimeException) cause).getException();
            }
            if (cause instanceof TransformerException)
                return getRealException((TransformerException) cause);
            if (cause instanceof MCRException)
                return getRealException((MCRException) cause);
        }
        return exc;
    }

    private Exception getRealException(MCRException exc) {
        if (exc.getCause() != null) {
            Throwable cause = exc.getCause();
            while (cause instanceof WrappedRuntimeException) {
                cause = ((WrappedRuntimeException) cause).getException();
            }
            if (cause instanceof TransformerException)
                return getRealException((TransformerException) cause);
            if (cause instanceof MCRException)
                return getRealException((MCRException) cause);
        }
        return exc;
    }

    /**
     * Builds a XSL transformer that uses the given XSL stylesheet
     * 
     * @param stylesheet
     *            the compiled XSL stylesheet to use
     * @return the XSL transformer that can be used to do the XSL transformation
     */
    private Transformer buildTransformer(Templates stylesheet) {
        try {
            Transformer tf = factory.newTransformerHandler(stylesheet).getTransformer();

            // In debug mode, add a TraceListener to log stylesheet execution
            if (LOGGER.isDebugEnabled()) {
                try {
                    TraceManager tm = ((org.apache.xalan.transformer.TransformerImpl) tf).getTraceManager();
                    tm.addTraceListener(this);

                } catch (Exception ex) {
                    LOGGER.warn(ex);
                }
            }

            return tf;
        } catch (TransformerConfigurationException exc) {
            String msg = "Error while building XSL transformer: " + exc.getMessageAndLocation();
            throw new MCRConfigurationException(msg, exc);
        }
    }

    /**
     * Sets XSL parameters for the given transformer by taking them from the
     * properties object provided.
     * 
     * @param transformer
     *            the Transformer object thats parameters should be set
     * @param parameters
     *            the XSL parameters as name-value pairs
     */
    public static void setXSLParameters(Transformer transformer, Properties parameters) {
        Enumeration<?> names = parameters.propertyNames();

        while (names.hasMoreElements()) {
            String name = names.nextElement().toString();
            String value = parameters.getProperty(name);

            transformer.setParameter(name, value);
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
    private void transform(Source xml, Templates xsl, Transformer transformer, HttpServletResponse response) throws IOException, MCRException {

        // Set content type from "<xsl:output media-type = "...">
        // Set char encoding from "<xsl:output encoding = "...">
        String ct = xsl.getOutputProperties().getProperty("media-type");
        String enc = xsl.getOutputProperties().getProperty("encoding");
        response.setCharacterEncoding(enc);
        response.setContentType(ct + "; charset=" + enc);

        LOGGER.debug("MCRLayoutService starts to output " + response.getContentType());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Result result = new StreamResult(out);
        try {
            transformMap.get().clear();
            transformer.transform(xml, result);
        } catch (TransformerException ex) {
            String msg = "Error while transforming XML using XSL stylesheet: " + ex.getMessageAndLocation();
            throw new MCRException(msg, ex);
        } finally {
            transformMap.get().clear();
            out.close();
        }

        OutputStream sos = response.getOutputStream();
        try {
            byte[] primaryResult = out.toByteArray();
            if ("application/pdf".equals(ct)) {
                LOGGER.debug("Formatting XSL-FO");
                ByteArrayInputStream tmp_stream = new ByteArrayInputStream(primaryResult);
                fo_formatter.transform(tmp_stream, sos);
            } else {
                sos.write(primaryResult);
            }
        } catch (Throwable ex) {
            StringBuffer sb = new StringBuffer();
            while (ex != null) {
                sb.append(ex.getClass().getName());
                if (ex.getMessage() != null)
                    sb.append(": ").append(ex.getMessage());
                ex = ex.getCause();
                if (ex != null)
                    sb.append(" - ");
            }
            LOGGER.warn("Exception writing formatter response to client: " + sb.toString());
        } finally {
            sos.close();
        }
    }

    /**
     * Traces the execution of xsl stylesheet elements in debug mode. The trace
     * is written to the log, and in parallel as comment elements to the output
     * html.
     */
    public void trace(TracerEvent ev) {
        ElemTemplateElement ete = ev.m_styleNode; // Current position in
        // stylesheet

        StringBuffer log = new StringBuffer();

        // Find the name of the stylesheet file currently processed
        try {
            StringTokenizer st = new StringTokenizer(ete.getBaseIdentifier(), "/\\");
            String stylesheet = null;
            while (st.hasMoreTokens()) {
                stylesheet = st.nextToken();
            }
            if (stylesheet != null) {
                log.append(" ").append(stylesheet);
            }
        } catch (Exception ignored) {
        }

        // Output current line number and column number
        log.append(" line " + ete.getLineNumber() + " col " + ete.getColumnNumber());

        // Find the name of the xsl:template currently processed
        try {
            ElemTemplate et = ev.m_processor.getCurrentTemplate();
            log.append(" in <xsl:template");
            if (et.getMatch() != null) {
                log.append(" match=\"" + et.getMatch().getPatternString() + "\"");
            }
            if (et.getName() != null) {
                log.append(" name=\"" + et.getName().getLocalName() + "\"");
            }
            if (et.getMode() != null) {
                log.append(" mode=\"" + et.getMode().getLocalName() + "\"");
            }
            log.append(">");
        } catch (Exception ignored) {
        }

        // Output name of the xsl or html element currently processed
        log.append(" " + ete.getTagName());
        LOGGER.debug("Trace" + log.toString());

        // Output xpath of current xml source node in context
        StringBuffer path = new StringBuffer();
        Node node = ev.m_sourceNode;
        if (node != null) {
            path.append(node.getLocalName());
            while ((node = node.getParentNode()) != null) {
                path.insert(0, node.getLocalName() + "/");
            }
        }
        if (path.length() > 0) {
            LOGGER.debug("Source " + path.toString());
        }
        try {
            if ("true".equals(ev.m_processor.getParameter("DEBUG"))) {
                ev.m_processor.getResultTreeHandler().comment(log.toString() + " ");
                if (path.length() > 0) {
                    ev.m_processor.getResultTreeHandler().comment(" source " + path.toString() + " ");
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * When a stylesheet generates characters, they will be logged in debug
     * mode.
     */
    public void generated(GenerateEvent ev) {
        if (ev.m_eventtype == 12) {
            LOGGER.debug("Output " + new String(ev.m_characters, ev.m_start, ev.m_length).trim());
        }
    }

    /**
     * When a stylesheet does a selection, like in &lt;xsl:value-of /&gt; or
     * similar elements, the selection element and xpath is logged in debug
     * mode.
     */
    public void selected(SelectionEvent ev) {
        String log = "Selection <xsl:" + ev.m_styleNode.getTagName() + " " + ev.m_attributeName + "=\"" + ev.m_xpath.getPatternString() + "\">";
        LOGGER.debug(log);
        try {
            if ("true".equals(ev.m_processor.getParameter("DEBUG"))) {
                ev.m_processor.getResultTreeHandler().comment(" " + log + " ");
            }
        } catch (SAXException ignored) {
        }
    }
}
