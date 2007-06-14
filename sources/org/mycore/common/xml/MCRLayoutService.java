/*
 * $RCSfile$
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

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

import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.user.MCRUserMgr;

/**
 * Does the layout for other MyCoRe servlets by transforming XML input to
 * various output formats, using XSL stylesheets.
 * 
 * @author Frank L�tzenkirchen
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRLayoutService {

    /** A cache of already compiled stylesheets */
    private MCRCache STYLESHEETS_CACHE = new MCRCache(100, "XSLT Stylesheets");

    /** The directory containing the xsl files */
    private String stylesheetsDir;

    /** The XSL transformer factory to use */
    private SAXTransformerFactory factory;

    /** The logger */
    private final static Logger LOGGER = Logger.getLogger(MCRLayoutService.class);

    public MCRLayoutService(String stylesheetsDir) {
        this.stylesheetsDir = stylesheetsDir;
//        System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.xsltc.trax.TransformerFactoryImpl");
        System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
        TransformerFactory tf = TransformerFactory.newInstance();
        LOGGER.info("Transformerfactory: "+tf.getClass().getName());

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

    public Properties buildXSLParameters(HttpServletRequest request) {
        // PROPERTIES: Read all properties from system configuration
        Properties parameters = (Properties) (MCRConfiguration.instance().getProperties().clone());
        // added properties of MCRSession and request
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

        // get current groups
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String uid = mcrSession.getCurrentUserID();
        StringBuffer groups = new StringBuffer();
        
        if (MCRConfiguration.instance().getString("MCR.Users.Guestuser.UserName").equals(uid)) {
            groups.append(MCRConfiguration.instance().getString("MCR.Users.Guestuser.GroupName"));
        }
        else {
          ArrayList groupList = MCRUserMgr.instance().retrieveUser(uid).getGroupIDs();
          for (int i = 0; i < groupList.size(); i++) {
              if (i != 0) groups.append(" ");
              groups.append((String) groupList.get(i));
          }
        }

        // set parameters
        parameters.put("CurrentUser", uid);
        parameters.put("CurrentGroups", groups.toString());
        parameters.put("RequestURL", getCompleteURL(request));
        parameters.put("WebApplicationBaseURL", MCRServlet.getBaseURL());
        parameters.put("ServletsBaseURL", MCRServlet.getServletBaseURL());
        parameters.put("DefaultLang", MCRConfiguration.instance().getString("MCR.Metadata.DefaultLang", "en"));
        parameters.put("CurrentLang", mcrSession.getCurrentLanguage());
        parameters.put("Referer", (request.getHeader("Referer") != null) ? request.getHeader("Referer") : "");

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
     * overwritten by Parameters of MCRSession. Finally parameters, then attributes 
     * of HttpServletRequest overwrite the previously defined.
     * 
     * @param request
     * @return merged XSL.* properties of MCR|HttpSession and HttpServletRequest
     */
    private final Properties mergeProperties(HttpServletRequest request) {
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

    private final String getCompleteURL(HttpServletRequest request) {
        String requesturl = request.getRequestURL().toString();
        StringBuffer buffer = new StringBuffer();
        if (!requesturl.toLowerCase().startsWith("http"))
            buffer.append(MCRServlet.getBaseURL());
        buffer.append(requesturl);

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
    private Templates buildCompiledStylesheet(File file) {
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

            STYLESHEETS_CACHE.put(path, stylesheet);
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
            return factory.newTransformerHandler(stylesheet).getTransformer();
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
}
