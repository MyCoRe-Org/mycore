/**
 *
 */
package org.mycore.urn.servlets;

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.urn.services.MCRURNAdder;
import org.mycore.urn.services.MCRURNManager;

/**
 * Class is responsible for adding urns to the metadata of a mycore object, to derivate and / or to files within a derivate.
 * For usage you have three URL parameters <code>object</code>, <code>target</code> and <code>path</code>. The <code>object</code>
 * parameter is required and contains the objectID or derivateID. The <code>target</code> parameter provides the following cases:
 * <ul>
 *   <li>empty/missing: derivate including all files gets an urn or if object contains an objectID,
 *     urn is added to metadata at /mycoreobject/metadata/def.identifier/identifier[@type='urn']</li>
 *   <li>file: object has to be derivateID, than the <code>path</code> parameter gives the path to file within this derivate</li>
 *   <li>derivate: generates an urn to the given derivateID</li>
 * </ul>
 *
 * @author shermann
 */
@Deprecated
public class MCRAddURNToObjectServlet extends MCRServlet {
    private static final Logger LOGGER = LogManager.getLogger(MCRAddURNToObjectServlet.class);

    /***/
    private static final long serialVersionUID = 1L;

    private static final String USER_ERROR_PAGE = "editor_error_user.xml";

    @Override
    protected void doGetPost(MCRServletJob job) throws IOException, MCRException {
        String object = job.getRequest().getParameter("object");
        String target = job.getRequest().getParameter("target");

        if (object == null) {
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // checking access right
        if (!MCRAccessManager.checkPermission(object, PERMISSION_WRITE)) {
            job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // checking if URN already assigned
        if (MCRURNManager.hasURNAssigned(object) && !"file".equals(target)) {
            throw new MCRException("Error while assigning urn to object '" + object + "'. It already has an urn.");
        }

        // set default target if not given
        if (target == null) {
            target = "";
        }

        MCRURNAdder urnAdder = new MCRURNAdder();

        switch (target) {
            case "file":
                try {
                    LOGGER.info("Adding URN to single file");
                    String path = job.getRequest().getParameter("path");
                    if (path == null) {
                        job.getResponse().sendRedirect(
                            job.getResponse().encodeRedirectURL(MCRFrontendUtil.getBaseURL() + USER_ERROR_PAGE));
                        return;
                    }

                    urnAdder.addURNToSingleFile(object, path);
                } catch (Exception e) {
                    throw new MCRException("Error while assigning urn to single file", e);
                }
                break;
            case "derivate":
                if (object.contains("_derivate_")) {
                    try {
                        LOGGER.info("Adding URN to derivate " + object);
                        if (!urnAdder.addURNToDerivate(object)) {
                            job.getResponse().sendRedirect(
                                job.getResponse().encodeRedirectURL(MCRFrontendUtil.getBaseURL() + USER_ERROR_PAGE));
                            return;
                        }
                    } catch (Exception e) {
                        throw new MCRException("Error while assigning urn to derivate '" + object + "'", e);
                    }
                } else {
                    throw new MCRException(
                        "Error while assigning urn to derivate '" + object + "'. No derivateID given.");
                }
                break;
            default:
                /* assign urn to derivate */
                if (object.contains("_derivate_")) {
                    try {
                        LOGGER.info("Adding URN to all files in derivate (urn granular) " + object);
                        if (!urnAdder.addURNToDerivates(object)) {
                            job.getResponse().sendRedirect(
                                job.getResponse().encodeRedirectURL(MCRFrontendUtil.getBaseURL() + USER_ERROR_PAGE));
                            return;
                        }
                    } catch (Exception e) {
                        throw new MCRException("Error while assigning urn to derivate '" + object + "'", e);
                    }
                } else {
                    /* assign urn to a mycore object */
                    try {
                        LOGGER.info("Assigning urn to object '" + object);
                        if (!urnAdder.addURN(object)) {
                            job.getResponse().sendRedirect(
                                job.getResponse().encodeRedirectURL(MCRFrontendUtil.getBaseURL() + USER_ERROR_PAGE));
                            return;
                        }
                    } catch (Exception e) {
                        throw new MCRException("Error while assigning urn to object '" + object, e);
                    }
                }
                break;
        }

        String referrer = job.getRequest().getHeader("referer"); // yes, with misspelling.
        if (referrer != null && referrer.length() > 0) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(referrer));
        } else {
            job.getResponse().sendError(HttpServletResponse.SC_EXPECTATION_FAILED,
                "Could not get referrer from request");
        }
    }
}
