/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.common.xml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
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
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Does the layout for other MyCoRe servlets by transforming XML input to
 * various output formats, using XSL stylesheets.
 * 
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRLayoutServlet extends MCRServlet {
    //TODO: we should invent something here
    private static final long serialVersionUID = 1L;

    private static MCRCache CACHE;

    private static final Logger LOGGER = Logger
            .getLogger(MCRLayoutServlet.class);

    public static final String DOM_ATTR = "MCRLayoutServlet.Input.DOM";

    public static final String JDOM_ATTR = "MCRLayoutServlet.Input.JDOM";

    public static final String BYTE_ATTR = "MCRLayoutServlet.Input.BYTES";

    public static final String FILE_ATTR = "MCRLayoutServlet.Input.FILE";

    public static final String STREAM_ATTR = "MCRLayoutServlet.Input.STREAM";

    public void init() {
        super.init();
        buildTransformerFactory();
        CACHE = new MCRCache(100);
  		MCRURIResolver.init( getServletContext(), getBaseURL() );
    }

    protected String parseDocumentType(InputStream in) {
        SAXParser parser = null;
        try {
            parser = SAXParserFactory.newInstance().newSAXParser();
        } catch (Exception ex) {
            String msg = "Could not build a SAX Parser for processing XML input";
            throw new MCRConfigurationException(msg, ex);
        }

        final Properties detected = new Properties();
        final String forcedInterrupt = "mcr.forced.interrupt";

        DefaultHandler handler = new DefaultHandler() {
            public void startElement(String uri, String localName,
                    String qName, Attributes attributes) throws SAXException {
                LOGGER.debug("MCRLayoutServlet detected root element = "
                        + qName);
                detected.setProperty("docType", qName);
                throw new MCRException(forcedInterrupt);
            }

            // We would need SAX 2.0 to be able to do this, for later use:
            public void startDTD(String name, String publicId, String systemId)
                    throws SAXException {
                LOGGER.debug("MCRLayoutServlet detected DOCTYPE declaration = "
                        + name);
                detected.setProperty("docType", name);
                throw new MCRException(forcedInterrupt);
            }
        };

        try {
            parser.parse(new InputSource(in), handler);
        } catch (Exception ex) {
            if (!forcedInterrupt.equals(ex.getMessage())) {
                String msg = "Error while detecting XML document type from input source";
                throw new MCRException(msg, ex);
            }
        }

        return detected.getProperty("docType");
    }

    protected void forwardXML(HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/xml");
        OutputStream out = response.getOutputStream();

        if (request.getAttribute(JDOM_ATTR) != null) {
            org.jdom.Document jdom = (org.jdom.Document) (request
                    .getAttribute(JDOM_ATTR));
            new org.jdom.output.XMLOutputter().output(jdom, out);
        } else if (request.getAttribute(DOM_ATTR) != null) {
            org.w3c.dom.Document dom = (org.w3c.dom.Document) (request
                    .getAttribute(DOM_ATTR));
            org.jdom.Document jdom = new org.jdom.input.DOMBuilder().build(dom);
            new org.jdom.output.XMLOutputter().output(jdom, out);
        } else if (request.getAttribute(STREAM_ATTR) != null) {
            InputStream in = (InputStream) (request.getAttribute(STREAM_ATTR));
            MCRUtils.copyStream(in, out);
        } else if (request.getAttribute(BYTE_ATTR) != null) {
            byte[] bytes = (byte[]) (request.getAttribute(BYTE_ATTR));
            MCRUtils.copyStream(new ByteArrayInputStream(bytes), out);
        } else if (request.getAttribute(FILE_ATTR) != null) {
            File file = (File) (request.getAttribute(FILE_ATTR));
            FileInputStream fis = new FileInputStream(file);
            MCRUtils.copyStream(fis, out);
            fis.close();
        }

        out.close();
    }

    public void doGetPost(MCRServletJob job) throws IOException,
            ServletException {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        Source sourceXML = null;
        String docType = null;
        boolean errorPage = false;

        if (request.getAttribute(JDOM_ATTR) != null) {
            org.jdom.Document jdom = (org.jdom.Document) (request
                    .getAttribute(JDOM_ATTR));
            sourceXML = new org.jdom.transform.JDOMSource(jdom);

            if (jdom.getDocType() != null)
                docType = jdom.getDocType().getElementName();
            else
                docType = jdom.getRootElement().getName();
            //errorpage is delivered as JDOM-Document - always
            //check if it's a errorpage to suppress later generating a error
            //page if there's a error in the errorpage stylesheet
            if (jdom.getRootElement().getName().equals("mcr_error"))
                errorPage = true;
        } else if (request.getAttribute(DOM_ATTR) != null) {
            org.w3c.dom.Document dom = (org.w3c.dom.Document) (request
                    .getAttribute(DOM_ATTR));
            sourceXML = new DOMSource(dom);

            if (dom.getDoctype() != null)
                docType = dom.getDoctype().getName();
            else
                docType = dom.getDocumentElement().getTagName();
        } else if (request.getAttribute(STREAM_ATTR) != null) {
            int bufferSize = 1000;
            InputStream is = (InputStream) (request.getAttribute(STREAM_ATTR));
            PushbackInputStream pis = new PushbackInputStream(is, bufferSize);
            pis.mark(bufferSize);
            docType = parseDocumentType(pis);
            pis.reset();
            sourceXML = new StreamSource(pis);
        } else if (request.getAttribute(BYTE_ATTR) != null) {
            byte[] bytes = (byte[]) (request.getAttribute(BYTE_ATTR));
            docType = parseDocumentType(new ByteArrayInputStream(bytes));
            sourceXML = new StreamSource(new ByteArrayInputStream(bytes));
        } else if (request.getAttribute(FILE_ATTR) != null) {
            File file = (File) (request.getAttribute(FILE_ATTR));
            FileInputStream fis = new FileInputStream(file);
            docType = parseDocumentType(fis);
            fis.close();
            sourceXML = new StreamSource(file);
        }

        Properties parameters = buildXSLParameters(request);
        String style = parameters.getProperty("Style", "default");
        LOGGER.debug("MCRLayoutServlet using style " + style);

        String type = getProperty(request, "type");
        String styleName = buildStylesheetName(style, docType, type);
        String styleDir = "/WEB-INF/stylesheets/";
        File styleFile = getStylesheetFile(styleDir, styleName);
        if ((styleFile == null)
                && ((style.equals(MCRSessionMgr.getCurrentSession().getCurrentLanguage())))){
            /* We are here because we tried StaticFileServlet tried
             * to get a stylesheet for a specific language */
            style="default";
            styleName = buildStylesheetName(style, docType, type);
            styleFile = getStylesheetFile(styleDir, styleName);
        }
        /*
         * if there is no stylesheet present forward as xml instead you can
         * transform xml-code using "doctype"-xml.xsl now
         */
        if ((styleFile == null)
                && ((style.equals("xml") || (style.equals("default")))))
            forwardXML(request, response);
        else {
            if (styleFile == null) {
                /*
                 * What's that? Maybe a kid wants to hack our mycore to get that
                 * complicated raw xml source code. We should stop that now and
                 * forever and go to lunch!
                 */
                String mode = getProperty(request, "mode");
                String layout = getProperty(request, "layout");
                String lang = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
                if ((layout == null) || (layout.equals("")))
                    layout = type;
                style = mode + "-" + layout + "-" + lang;
                styleName = buildStylesheetName(style, docType, type);
                styleFile = getStylesheetFile(styleDir, styleName);
            }
            Templates stylesheet = null;
            Transformer transformer = null;
            ;
            if (styleFile != null) {
                stylesheet = buildCompiledStylesheet(styleFile);
                transformer = buildTransformer(stylesheet);
                setXSLParameters(transformer, parameters);
            }
            try {
                if (styleFile == null) {
                    throw new MCRException("Stylesheetfile: " + styleName
                            + " not found!");
                }
                transform(sourceXML, stylesheet, transformer, response);
            } catch (IOException ex) {
                LOGGER
                        .error("IO Error while XSL transforming XML Document",
                                ex);
            } catch (MCRException ex) {
                if (errorPage)
                    throw new MCRException("Error while genrating error page!",
                            ex);
                generateErrorPage(request, response,
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex
                                .getMessage(), ex, false);
            }
        }
    }

    public static Properties buildXSLParameters(HttpServletRequest request) {
        Properties parameters = (Properties)( MCRConfiguration.instance().getProperties().clone() );

	// Read all properties from mycore.properties
        
        // Read all *.xsl attributes that are stored in the browser session
        HttpSession session = request.getSession(false);
        if (session != null) {
            for (Enumeration e = session.getAttributeNames(); e
                    .hasMoreElements();) {
                String name = (String) (e.nextElement());
                if (name.startsWith("XSL."))
                    parameters.put(name.substring(4), session
                            .getAttribute(name));
            }
        }

        // Read all *.xsl attributes from the client HTTP request parameters
        for (Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
            String name = (String) (e.nextElement());
            if (name.startsWith("XSL.")) {
                parameters.put(name.substring(4), request.getParameter(name));
            }
        }
        // Read all *.xsl attributes provided by the invoking servlet
        for (Enumeration e = request.getAttributeNames(); e.hasMoreElements();) {
            String name = (String) (e.nextElement());
            if (name.startsWith("XSL."))
                parameters.put(name.substring(4), request.getAttribute(name));
        }

        // Set some predefined XSL parameters:
        String defaultLang = MCRConfiguration.instance().getString(
                "MCR.metadata_default_lang", "en");

        String user = MCRConfiguration.instance().getString(
                "MCR.users_guestuser_username");
        String lang = defaultLang;
        String referer = request.getHeader("Referer");
        if (referer==null) referer="";
        
        // handle HttpSession
        if (session !=null && !request.isRequestedSessionIdFromCookie()){
        	parameters.put("HttpSession",";jsessionid="+session.getId());
        }
        if (session != null) {
        		parameters.put("JSessionID",";jsessionid="+session.getId());
            MCRSession mcrSession = (MCRSession) (session
                    .getAttribute("mycore.session"));
            if (mcrSession != null) {
                user = mcrSession.getCurrentUserID();
                lang = mcrSession.getCurrentLanguage();
            }
        }

        LOGGER.debug("LayoutServlet XSL.MCRSessionID="
                + parameters.getProperty("MCRSessionID"));
        LOGGER.debug("LayoutServlet XSL.CurrentUser =" + user);
        LOGGER.debug("LayoutServlet HttpSession =" + parameters.getProperty("HttpSession"));
        LOGGER.debug("LayoutServlet JSessionID =" + parameters.getProperty("JSessionID"));
        LOGGER.debug("LayoutServlet RefererURL =" + referer);

        parameters.put("CurrentUser", user);
        parameters.put("RequestURL", getCompleteURL(request));
        parameters.put("WebApplicationBaseURL", getBaseURL());
        parameters.put("ServletsBaseURL", getServletBaseURL());
        parameters.put("DefaultLang", defaultLang);
        parameters.put("CurrentLang", lang);
        parameters.put("Referer", referer);

        return parameters;
    }

    protected static final String getCompleteURL(HttpServletRequest request) {
        StringBuffer buffer = request.getRequestURL();
        String queryString = request.getQueryString();
        if (queryString != null)
            buffer.append("?").append(queryString);
        return buffer.toString();
    }

    /**
     * Builds the filename of the stylesheet to use, e. g. "playlist-simple.xsl"
     */
    protected String buildStylesheetName(String style, String docType,
            String type) {
        StringBuffer filename = new StringBuffer(docType);
        if (!"default".equals(style)) {
            if (("xml".equals(style)) && (type != null) && (type.length() > 0))
                filename.append("-").append(type);
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
    protected File getStylesheetFile(String dir, String name) {
        String path = getServletContext().getRealPath(dir + name);
        File file = new File(path);

        if (!file.exists()) {
            LOGGER.debug("MCRLayoutServlet did not find stylesheet " + name);
            return null;
        }

        if (!file.canRead()) {
            String msg = "XSL stylesheet " + path + " not readable";
            throw new MCRConfigurationException(msg);
        }

        return file;
    }

    /** The XSL transformer factory to use */
    protected SAXTransformerFactory factory;

    /**
     * Builds a SAX transformer factory for later use
     * 
     * @throws MCRConfigurationException
     *             if no SAXTransformerFactory was found
     */
    protected void buildTransformerFactory() {
        TransformerFactory tf = TransformerFactory.newInstance();

        if (!tf.getFeature(SAXTransformerFactory.FEATURE))
            throw new MCRConfigurationException(
                    "Could not load a SAXTransformerFactory for use with XSLT");

        factory = (SAXTransformerFactory) (tf);

        factory.setURIResolver( MCRURIResolver.instance() ); 
    }

    /**
     * Reads an XSL stylesheet from the given file and returns it as compiled
     * XSL Templates object.
     * 
     * @param file
     *            the File that contains the XSL stylesheet
     * @return the compiled stylesheet
     */
    protected Templates buildCompiledStylesheet(File file) {
        String path = file.getPath();
        long time = file.lastModified();

        Templates stylesheet = (Templates) (CACHE.getIfUpToDate(path, time));

        if (stylesheet == null) {
            try {
                stylesheet = factory.newTemplates(new StreamSource(file));
                LOGGER.debug("MCRLayoutServlet compiled stylesheet "
                        + file.getName());
            } catch (TransformerConfigurationException exc) {
                String msg = "Error while compiling XSL stylesheet "
                        + file.getName() + ": " + exc.getMessageAndLocation();
                throw new MCRConfigurationException(msg, exc);
            }

            CACHE.put(path, stylesheet);
        } else {
            LOGGER.debug("MCRLayoutServlet using cached stylesheet "
                    + file.getName());
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
    protected Transformer buildTransformer(Templates stylesheet) {
        try {
            return factory.newTransformerHandler(stylesheet).getTransformer();
        } catch (TransformerConfigurationException exc) {
            String msg = "Error while building XSL transformer: "
                    + exc.getMessageAndLocation();
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
    protected void setXSLParameters(Transformer transformer,
            Properties parameters) {
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
    protected void transform(Source xml, Templates xsl,
            Transformer transformer, HttpServletResponse response)
            throws IOException, MCRException {
        // Set content type from "<xsl:output media-type = "...">
        // Set char encoding from "<xsl:output encoding = "...">
        String ct = xsl.getOutputProperties().getProperty("media-type");
        String enc = xsl.getOutputProperties().getProperty("encoding");
        response.setContentType(ct + "; charset=" + enc);
        LOGGER.debug("MCRLayoutServlet starts to output " + ct + "; charset="
                + enc);

        OutputStream out = response.getOutputStream();

        try {
            transformer.transform(xml, new StreamResult(out));
        } catch (TransformerException ex) {
            String msg = "Error while transforming XML using XSL stylesheet: "
                    + ex.getMessageAndLocation();
            throw new MCRException(msg, ex);
        } finally {
            out.close();
        }
    }
}
