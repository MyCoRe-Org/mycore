/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.frontend.servlets;

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;

/**
 * @author shermann
 */
public class MCRDisplayHideDerivateServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger(MCRDisplayHideDerivateServlet.class);

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        String derivate = job.getRequest().getParameter("derivate");

        if (derivate == null || (!derivate.contains("_derivate_"))) {
            LOGGER.error("Cannot toogle display attribute. No derivate id provided.");
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, "You must provide a proper derivate id");
            return;
        }

        if (!MCRAccessManager.checkPermission(MCRObjectID.getInstance(derivate), PERMISSION_WRITE)) {
            job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, "You have to be logged in.");
            return;
        }

        LOGGER.info("Toggling display attribute of {}", derivate);
        MCRDerivate obj = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(derivate));
        toggleDisplay(obj);

        String url = MCRFrontendUtil.getBaseURL() + "receive/" + getParentHref(obj);
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(url));
    }

    private String getParentHref(MCRDerivate obj) {
        return obj.getDerivate().getMetaLink().getXLinkHref();
    }

    /**
     * Toggles the display attribute value of the derivate element.
     */
    private void toggleDisplay(MCRDerivate derObj) throws Exception {
        Document xml = derObj.createXML();
        Element derivateNode = xml.getRootElement().getChild("derivate");

        Attribute displayAttr = derivateNode.getAttribute("display");

        /* the attributs is not existing, user wants to hide derivate */
        if (displayAttr == null) {
            displayAttr = new Attribute("display", "false");
            derivateNode.setAttribute(displayAttr);
        }
        /* attribute exists, thus toggle the attribute value */
        else {
            String oldVal = displayAttr.getValue();
            String newVal = oldVal.equals(String.valueOf(true)) ? String.valueOf(false) : String.valueOf(true);
            displayAttr.setValue(newVal);
            LOGGER.info("Setting display attribute of derivate with id {} to {}", derObj.getId(), newVal);
        }
        MCRDerivate updated = new MCRDerivate(xml);
        MCRMetadataManager.updateMCRDerivateXML(updated);
    }
}
