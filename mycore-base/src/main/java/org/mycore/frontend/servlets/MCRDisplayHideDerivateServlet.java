/**
 * 
 */
package org.mycore.frontend.servlets;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.xpath.XPath;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * @author shermann
 *
 */
public class MCRDisplayHideDerivateServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRDisplayHideDerivateServlet.class);

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        if (!MCRAccessManager.checkPermission("writedb")) {
            job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, "You have to be logged in.");
            return;
        }

        String derivate = job.getRequest().getParameter("derivate");
        if (derivate == null || (derivate.indexOf("_derivate_") == -1)) {
            LOGGER.error("Cannot toogle display attribute. No derivate id provided.");
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, "You must provide a proper derivate id");
            return;
        }
        LOGGER.info("Toggling display attribute of " + derivate);
        MCRDerivate obj = MCRDerivate.createFromDatastore(new MCRObjectID(derivate));
        toggleDisplay(obj);

        String url = getBaseURL() + "receive/" + getParentHref(obj);
        job.getResponse().encodeRedirectURL(url);
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(url));
    }

    private String getParentHref(MCRDerivate obj) {
        return obj.getDerivate().getMetaLink().getXLinkHref();
    }

    /** 
     * Toggles the display attribute value of the derivate element. 
     * */
    private void toggleDisplay(MCRDerivate derObj) throws Exception {
        Document xml = derObj.createXML();
        XPath xp = XPath.newInstance("mycorederivate/derivate");
        Element derivateNode = (Element) xp.selectSingleNode(xml);

        Attribute displayAttr = null;
        displayAttr = derivateNode.getAttribute("display");

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
            LOGGER.info("Setting display attribute of derivate with id " + derObj.getId() + " to " + newVal);
        }
        MCRDerivate updated = new MCRDerivate(xml);
        updated.updateXMLInDatastore();
    }
}
