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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.apache.xalan.templates.ElemTemplate;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.trace.GenerateEvent;
import org.apache.xalan.trace.SelectionEvent;
import org.apache.xalan.trace.TraceManager;
import org.apache.xalan.trace.TracerEvent;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.user.MCRUserMgr;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Does the layout for other MyCoRe servlets by transforming XML input to
 * various output formats, using XSL stylesheets.
 * 
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRLayoutService implements org.apache.xalan.trace.TraceListener {

    /** A cache of already compiled stylesheets */
    private MCRCache STYLESHEETS_CACHE = new MCRCache(100, "XSLT Stylesheets");

    /** The directory containing the xsl files */
    private String stylesheetsDir;

    /** The XSL transformer factory to use */
    private SAXTransformerFactory factory;

    /** The logger */
    private final static Logger LOGGER = Logger.getLogger(MCRLayoutService.class);

    private static final MCRLayoutService singleton = new MCRLayoutService();
    
    public static MCRLayoutService instance()
    {
    	return singleton;
    }
    
    public void setStylesheetsDir(String stylesheetsDir) {
        this.stylesheetsDir = stylesheetsDir;
    }
    
    private MCRLayoutService() {
        // System.setProperty("javax.xml.transform.TransformerFactory",
        // "org.apache.xalan.xsltc.trax.TransformerFactoryImpl");
        System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
        TransformerFactory tf = TransformerFactory.newInstance();
        LOGGER.info("Transformerfactory: " + tf.getClass().getName());

        if (!tf.getFeature(SAXTransformerFactory.FEATURE)) {
            throw new MCRConfigurationException("Could not load a SAXTransformerFactory for use with XSLT");
        }

        factory = (SAXTransformerFactory) (tf);
        factory.setURIResolver(MCRURIResolver.instance());
    }

    public void sendXML(HttpServletRequest req, HttpServletResponse res, org.jdom.Document jdom) throws IOException {
        res.setContentType("text/xml");
        OutputStream out = res.getOutputStream();
        new org.jdom.output.XMLOutputter().output(jdom, out);
        out.close();
    }

    public void sendXML(HttpServletRequest req, HttpServletResponse res, org.w3c.dom.Document dom) throws IOException {
        sendXML(req, res, new org.jdom.input.DOMBuilder().build(dom));
    }

    public void sendXML(HttpServletRequest req, HttpServletResponse res, InputStream in) throws IOException {
        res.setContentType("text/xml");
        OutputStream out = res.getOutputStream();
        MCRUtils.copyStream(in, out);
        out.close();
    }

    public void sendXML(HttpServletRequest req, HttpServletResponse res, File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        sendXML(req, res, fis);
        fis.close();
    }

    public void doLayout(HttpServletRequest req, HttpServletResponse res, org.jdom.Document jdom) throws IOException {
        String docType = (jdom.getDocType() == null ? jdom.getRootElement().getName() : jdom.getDocType().getElementName());
        Properties parameters = buildXSLParameters(req);
        File styleFile = chooseStyleFile(req, parameters, docType);
        if (styleFile == null)
            sendXML(req, res, jdom);
        else
            transform(res, new org.jdom.transform.JDOMSource(jdom), docType, parameters, styleFile);
    }

    /**
     * writes the transformation result directly into the Writer
     * uses the HttpServletResponse only for error messages
     */
    public void doLayout(HttpServletRequest req, HttpServletResponse res, Writer out, org.jdom.Document jdom) throws IOException {
		String docType = (jdom.getDocType() == null ? jdom.getRootElement().getName() 
				: jdom.getDocType().getElementName());
		Properties parameters = buildXSLParameters(req);
		File styleFile = chooseStyleFile(req, parameters, docType);
		if (styleFile == null)
			new org.jdom.output.XMLOutputter().output(jdom, out);
		else {
			Templates stylesheet = buildCompiledStylesheet(styleFile);
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
				if (!"mcr_error".equals(docType)) throw ex;
				String msg = "Error while generating error page!";
				LOGGER.warn(msg, ex);				
				res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
			}
		}
	}
    
    public void doLayout(HttpServletRequest req, HttpServletResponse res, org.w3c.dom.Document dom) throws IOException {
        String docType = (dom.getDoctype() == null ? dom.getDocumentElement().getTagName() : dom.getDoctype().getName());
        Properties parameters = buildXSLParameters(req);
        File styleFile = chooseStyleFile(req, parameters, docType);
        if (styleFile == null)
            sendXML(req, res, dom);
        else
            transform(res, new DOMSource(dom), docType, parameters, styleFile);
    }

    public void doLayout(HttpServletRequest req, HttpServletResponse res, InputStream is) throws IOException {
        MCRContentInputStream cis = new MCRContentInputStream(is);
        String docType = MCRUtils.parseDocumentType(new ByteArrayInputStream(cis.getHeader()));

        Properties parameters = buildXSLParameters(req);
        File styleFile = chooseStyleFile(req, parameters, docType);
        if (styleFile == null)
            sendXML(req, res, cis);
        else
            transform(res, new StreamSource(cis), docType, parameters, styleFile);
    }

    public void doLayout(HttpServletRequest req, HttpServletResponse res, File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        doLayout(req, res, fis);
        fis.close();
    }

    public Document doLayout(Document doc, String stylesheetName, Hashtable<String, String> params) throws Exception {
    	HttpServletRequest req = null;
    	MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
    	MCRServletJob job = (MCRServletJob)mcrSession.get("MCRServletJob");
    	if ( job != null )
    		req = job.getRequest();
    	
        Properties parameters = buildXSLParameters(req);
        if ( null != params)
        	parameters.putAll(params);
        File styleFile = getStylesheetFile(stylesheetName);
        if ( styleFile == null)
        {
            LOGGER.error("Stylesheet not found: " + stylesheetName);
        	return null;
        }
        Templates stylesheet = buildCompiledStylesheet(styleFile);
        Transformer transformer = buildTransformer(stylesheet);
        setXSLParameters(transformer, parameters);
        JDOMResult out = new JDOMResult();
        transformer.transform(new JDOMSource(doc), out);
        return out.getDocument();
    }

    private void transform(HttpServletResponse res, Source sourceXML, String docType, Properties parameters, File styleFile) throws IOException {
        Templates stylesheet = buildCompiledStylesheet(styleFile);
        Transformer transformer = buildTransformer(stylesheet);
        setXSLParameters(transformer, parameters);

        try {
            transform(sourceXML, stylesheet, transformer, res);
        } catch (IOException ex) {
            LOGGER.error("IOException while XSL-transforming XML document", ex);
        } catch (MCRException ex) {
            // Check if it is an error page to suppress later recursively
            // generating an error page when there is an error in the stylesheet
            if (!"mcr_error".equals(docType))
                throw ex;

            String msg = "Error while generating error page!";
            LOGGER.warn(msg, ex);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        }
    }

    private File chooseStyleFile(HttpServletRequest req, Properties parameters, String docType) {
        String style = parameters.getProperty("Style", "default");
        LOGGER.debug("MCRLayoutService using style " + style);

        String styleName = buildStylesheetName(docType, style);
        File styleFile = getStylesheetFile(styleName);
        if (styleFile != null)
            return styleFile;

        // If no stylesheet exists, forward raw xml instead
        // You can transform raw xml code by providing a stylesheed named
        // [doctype]-xml.xsl now
        if ((styleFile == null) && (style.equals("xml") || (style.equals("default"))))
            return null;
        throw new MCRException("XSL stylesheet not found: " + styleName);
    }

    public static  Properties buildXSLParameters(HttpServletRequest request) {
        // PROPERTIES: Read all properties from system configuration
        Properties parameters = (Properties) (MCRConfiguration.instance().getProperties().clone());
        // added properties of MCRSession and request
        if (request != null) 
        {
			parameters.putAll(mergeProperties(request));

			// handle HttpSession
			HttpSession session = request.getSession(false);
			if (session != null) {
				String jSessionID = MCRConfiguration.instance().getString(
						"MCR.Session.Param", ";jsessionid=");
				if (!request.isRequestedSessionIdFromCookie()) {
					parameters.put("HttpSession", jSessionID + session.getId());
				}
				parameters.put("JSessionID", jSessionID + session.getId());
			}
		}

        String uid = MCRSessionMgr.getCurrentSession().getCurrentUserID();

        boolean setCurrentGroups = MCRConfiguration.instance().getBoolean("MCR.Users.SetCurrentGroups", true);
        if (setCurrentGroups) { // for MyCoRe applications, always true
            StringBuffer groups = new StringBuffer();

            List<String> groupList = MCRUserMgr.instance().retrieveUser(uid).getGroupIDs();
            for (int i = 0; i < groupList.size(); i++) {
                if (i != 0)
                    groups.append(" ");
                groups.append((String) groupList.get(i));
            }
            parameters.put("CurrentGroups", groups.toString());
        }

    	MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
    	
        // set parameters
        parameters.put("CurrentUser", uid);
        parameters.put("DefaultLang", MCRConfiguration.instance().getString("MCR.Metadata.DefaultLang", "en"));
        parameters.put("CurrentLang", mcrSession.getCurrentLanguage());
        parameters.put("WebApplicationBaseURL", MCRServlet.getBaseURL());
        parameters.put("ServletsBaseURL", MCRServlet.getServletBaseURL());
        
        if (request != null) 
        {
			parameters.put("RequestURL", getCompleteURL(request));
			parameters.put("Referer", (request.getHeader("Referer") != null) ? request.getHeader("Referer") : "");
		}

        LOGGER.debug("LayoutServlet XSL.MCRSessionID=" + parameters.getProperty("MCRSessionID"));
        LOGGER.debug("LayoutServlet XSL.CurrentUser =" + mcrSession.getCurrentUserID());
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
            for (Enumeration e = session.getAttributeNames(); e.hasMoreElements();) {
                String name = (String) (e.nextElement());
                if (name.startsWith("XSL."))
                    props.put(name.substring(4), session.getAttribute(name));
            }
        }
        for (Iterator it = mcrSession.getObjectsKeyList(); it.hasNext();) {
            String name = it.next().toString();
            if (name.startsWith("XSL."))
                props.put(name.substring(4), mcrSession.get(name));
        }
        for (Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
            String name = e.nextElement().toString();
            if (name.startsWith("XSL.") && !name.endsWith(".SESSION")) {
                props.put(name.substring(4), request.getParameter(name));
            }
        }
        for (Enumeration e = request.getAttributeNames(); e.hasMoreElements();) {
            String name = e.nextElement().toString();
            if (name.startsWith("XSL.") && !name.endsWith(".SESSION")) {
                props.put(name.substring(4), request.getAttribute(name).toString());
            }
        }
        return props;
    }

    private final static String getCompleteURL(HttpServletRequest request) {
        StringBuffer buffer = request.getRequestURL();
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
        StringBuffer filename = new StringBuffer(docType);

        if (!"default".equals(style)) {
            filename.append("-");
            filename.append(style);
        }

        filename.append(".xsl");

        return filename.toString();
    }

    /**
     * Gets a File object for the given filename and directory, or returns null
     * if no such file exists.
     */
    private File getStylesheetFile(String name) {
        File file = new File(stylesheetsDir, name);

        if (!file.exists()) {
            LOGGER.debug("MCRLayoutService did not find stylesheet " + name);
            return null;
        }

        if (!file.canRead()) {
            String msg = "XSL stylesheet " + name + " not readable";
            throw new MCRConfigurationException(msg);
        }

        return file;
    }

    /**
     * Reads an XSL stylesheet from the given file and returns it as compiled
     * XSL Templates object.
     * 
     * @param file
     *            the File that contains the XSL stylesheet
     * @return the compiled stylesheet
     */
    private Templates buildCompiledStylesheet(File file) throws MCRConfigurationException {
        String path = file.getPath();
        long time = file.lastModified();

        Templates stylesheet = (Templates) (STYLESHEETS_CACHE.getIfUpToDate(path, time));

        if (stylesheet == null) {
            try {
                stylesheet = factory.newTemplates(new StreamSource(file));
                LOGGER.debug("MCRLayoutService compiled stylesheet " + file.getName());
            } catch (TransformerConfigurationException exc) {
                String msg = "Error while compiling XSL stylesheet " + file.getName() + ": " + exc.getMessageAndLocation();
                throw new MCRConfigurationException(msg, exc);
            }
            try {
                STYLESHEETS_CACHE.put(path, stylesheet);
            } catch (MCRUsageException exc) {
                String msg = "Error while putting XSL stylesheet in cache " + file.getName();
                throw new MCRConfigurationException(msg, exc);
            }
        } else {
            LOGGER.debug("MCRLayoutService using cached stylesheet " + file.getName());
        }

        return stylesheet;
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
    private void setXSLParameters(Transformer transformer, Properties parameters) {
        Enumeration names = parameters.propertyNames();

        while (names.hasMoreElements()) {
            String name = (String) (names.nextElement());
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
        LOGGER.debug("MCRLayoutService starts to output " + ct + "; charset=" + enc);

        OutputStream out = response.getOutputStream();

        try {
            transformer.transform(xml, new StreamResult(out));
        } catch (TransformerException ex) {
            String msg = "Error while transforming XML using XSL stylesheet: " + ex.getMessageAndLocation();
            throw new MCRException(msg, ex);
        } finally {
            out.close();
        }
    }
    
    /**
     * Traces the execution of xsl stylesheet elements in debug mode. The trace is
     * written to the log, and in parallel as comment elements to the output html.
     */
    public void trace(TracerEvent ev) {
        ElemTemplateElement ete = ev.m_styleNode; // Current position in stylesheet

        StringBuffer log = new StringBuffer();

        // Find the name of the stylesheet file currently processed
        try {
            StringTokenizer st = new StringTokenizer(ete.getBaseIdentifier(), "/\\");
            String stylesheet = null;
            while (st.hasMoreTokens())
                stylesheet = st.nextToken();
            if (stylesheet != null)
                log.append(" ").append(stylesheet);
        } catch (Exception ignored) {
        }

        // Output current line number and column number
        log.append(" line " + ete.getLineNumber() + " col " + ete.getColumnNumber());

        // Find the name of the xsl:template currently processed
        try {
            ElemTemplate et = ev.m_processor.getCurrentTemplate();
            log.append(" in <xsl:template");
            if (et.getMatch() != null)
                log.append(" match=\"" + et.getMatch().getPatternString() + "\"");
            if (et.getName() != null)
                log.append(" name=\"" + et.getName().getLocalName() + "\"");
            if (et.getMode() != null)
                log.append(" mode=\"" + et.getMode().getLocalName() + "\"");
            log.append(">");
        } catch (Exception ignored) {
        }

        // Output name of the xsl or html element currently processed
        log.append(" " + ete.getTagName());
        LOGGER.debug("Trace" + log.toString() );

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
        if (ev.m_eventtype == 12)
            LOGGER.debug("Output " + new String(ev.m_characters, ev.m_start, ev.m_length).trim());
    }

    /**
     * When a stylesheet does a selection, like in &lt;xsl:value-of /&gt; or similar elements,
     * the selection element and xpath is logged in debug mode.
     */
    public void selected(SelectionEvent ev) {
        String log = "Selection <xsl:" + ev.m_styleNode.getTagName() + " " + ev.m_attributeName + "=\"" + ev.m_xpath.getPatternString() + "\">";
        LOGGER.debug(log);
        try {
            if ("true".equals(ev.m_processor.getParameter("DEBUG")))
                ev.m_processor.getResultTreeHandler().comment(" " + log + " ");
        } catch (SAXException ignored) {
        }
    }
}
