/**
 * 
 */
package org.mycore.frontend.servlets.persistence;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.validator.MCREditorOutValidator;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.xml.sax.SAXParseException;

/**
 * Helper methods for various servlets.
 * @author Thomas Scheffler (yagee)
 *
 */
class MCRPersistenceHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    static Properties getXSLProperties(HttpServletRequest request) {
        Properties params = new Properties();
        params.putAll(
            request
                .getParameterMap()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith("XSL.") && e.getValue().length != 0)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0])));
        return params;
    }

    static String getCancelUrl(HttpServletRequest request) {
        String cancelURL = request.getParameter("cancelURL");
        if (cancelURL != null && !cancelURL.isEmpty()) {
            return cancelURL;
        }
        String referer = request.getHeader("Referer");
        if (referer == null || referer.equals("")) {
            referer = MCRFrontendUtil.getBaseURL();
        }
        LOGGER.debug("Referer: " + referer);
        return referer;
    }

    /**
     * returns a jdom document representing the output of a mycore editor form.
     * @param job
     * @param failOnMissing
     *  if not editor submission is present, should we throw an exception (true) or just return null (false)
     * @return
     *  editor submission or null if editor submission is not present and failOnMissing==false
     * @throws ServletException
     *  if failOnMissing==true and no editor submission is present
     */
    static Document getEditorSubmission(HttpServletRequest request, boolean failOnMissing) throws ServletException {
        Document inDoc = (Document) request.getAttribute("MCRXEditorSubmission");
        if (inDoc == null) {
            if (failOnMissing)
                throw new ServletException("No MCREditorSubmission");
            else
                return null;
        }
        if (inDoc.getRootElement().getAttribute("ID") == null) {
            String mcrID = MCRServlet.getProperty(request, "mcrid");
            LOGGER.info("Adding MCRObjectID from request: " + mcrID);
            inDoc.getRootElement().setAttribute("ID", mcrID);
        }
        return inDoc;
    }

    /**
     * creates a MCRObject instance on base of JDOM document
     * @param doc
     *  MyCoRe object as XML.
     * @return
     * @throws JDOMException
     *  exception from underlying {@link MCREditorOutValidator}
     * @throws IOException
     *  exception from underlying {@link MCREditorOutValidator} or {@link XMLOutputter}
     * @throws SAXParseException
     * @throws MCRException
     */
    static MCRObject getMCRObject(Document doc) throws JDOMException, IOException, MCRException, SAXParseException {
        String id = doc.getRootElement().getAttributeValue("ID");
        MCRObjectID objectID = MCRObjectID.getInstance(id);
        MCREditorOutValidator ev = new MCREditorOutValidator(doc, objectID);
        Document jdom_out = ev.generateValidMyCoReObject();
        if (ev.getErrorLog().size() > 0 && LOGGER.isDebugEnabled()) {
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            StringWriter swOrig = new StringWriter();
            xout.output(doc, swOrig);
            LOGGER.debug("Input document \n" + swOrig.toString());
            for (String logMsg : ev.getErrorLog()) {
                LOGGER.debug(logMsg);
            }
            StringWriter swClean = new StringWriter();
            xout.output(jdom_out, swClean);
            LOGGER.debug("Results in \n" + swClean.toString());
        }
        MCRObject mcrObject = new MCRObject(jdom_out);
        return mcrObject;
    }

    protected static String getWebPage(ServletContext context, String modernPage, String deprecatedPage)
        throws ServletException {
        try {
            if (context.getResource("/" + modernPage) == null
                && context.getResource("/" + deprecatedPage) != null) {
                LogManager.getLogger()
                    .warn("Could not find " + modernPage + " in webapp root, using deprecated " + deprecatedPage
                        + " instead.");
                return deprecatedPage;
            }
        } catch (MalformedURLException e) {
            throw new ServletException(e);
        }
        return modernPage; //even if it does not exist: nice 404 helps the user
    }

}
