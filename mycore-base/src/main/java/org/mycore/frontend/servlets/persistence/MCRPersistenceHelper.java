/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.frontend.servlets.persistence;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

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
import org.mycore.resource.MCRResourceHelper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

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
        LOGGER.debug("Referer: {}", referer);
        return referer;
    }

    /**
     * returns a jdom document representing the output of a mycore editor form.
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
            if (failOnMissing) {
                throw new ServletException("No MCREditorSubmission");
            }
            return null;
        }
        if (inDoc.getRootElement().getAttribute("ID") == null) {
            String mcrID = MCRServlet.getProperty(request, "mcrid");
            LOGGER.info("Adding MCRObjectID from request: {}", mcrID);
            inDoc.getRootElement().setAttribute("ID", mcrID);
        }
        return inDoc;
    }

    /**
     * creates a MCRObject instance on base of JDOM document
     * @param doc
     *  MyCoRe object as XML.
     * @throws JDOMException
     *  exception from underlying {@link MCREditorOutValidator}
     * @throws IOException
     *  exception from underlying {@link MCREditorOutValidator} or {@link XMLOutputter}
     */
    static MCRObject getMCRObject(Document doc) throws JDOMException, IOException, MCRException {
        String id = doc.getRootElement().getAttributeValue("ID");
        MCRObjectID objectID = MCRObjectID.getInstance(id);
        MCREditorOutValidator ev = new MCREditorOutValidator(doc, objectID);
        Document validMyCoReObject = ev.generateValidMyCoReObject();
        if (!ev.getErrorLog().isEmpty() && LOGGER.isDebugEnabled()) {
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            StringWriter swOrig = new StringWriter();
            xout.output(doc, swOrig);
            LOGGER.debug("Input document \n{}", swOrig);
            for (String logMsg : ev.getErrorLog()) {
                LOGGER.debug(logMsg);
            }
            StringWriter swClean = new StringWriter();
            xout.output(validMyCoReObject, swClean);
            LOGGER.debug("Results in \n{}", swClean);
        }
        return new MCRObject(validMyCoReObject);
    }

    protected static String getWebPage(String modernPage, String deprecatedPage) {
        if (MCRResourceHelper.getWebResourceUrl(modernPage) == null
            && MCRResourceHelper.getWebResourceUrl(deprecatedPage) != null) {
            LOGGER.warn("Could not find {} in webapp root, using deprecated {} instead.",
                modernPage, deprecatedPage);
            return deprecatedPage;
        }
        return modernPage; //even if it does not exist: nice 404 helps the user
    }

}
