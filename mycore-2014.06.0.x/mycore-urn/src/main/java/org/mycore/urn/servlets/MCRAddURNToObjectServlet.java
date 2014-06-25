/**
 * 
 */
package org.mycore.urn.servlets;

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.urn.services.MCRURNAdder;
import org.mycore.urn.services.MCRURNManager;

/**
 * Class is responsible for adding urns to the metadata of a mycore object
 * 
 * @author shermann
 */
public class MCRAddURNToObjectServlet extends MCRServlet {
    private static final Logger LOGGER = Logger.getLogger(MCRAddURNToObjectServlet.class);

    /***/
    private static final long serialVersionUID = 1L;

    private static final String PAGEDIR = MCRConfiguration.instance().getString("MCR.SWF.PageDir", "");

    private static final String USERERRORPAGE = PAGEDIR + MCRConfiguration.instance().getString("MCR.SWF.PageErrorUser", "editor_error_user.xml");

    @Override
    protected void doGetPost(MCRServletJob job) throws IOException {
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

        MCRURNAdder urnAdder = new MCRURNAdder();

        if (target != null && target.equals("file")) {
            try {
                LOGGER.info("Adding URN to single file");
                String path = job.getRequest().getParameter("path");
                if (path == null) {
                    job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + USERERRORPAGE));
                    return;
                }

                urnAdder.addURNToSingleFile(object, path);
            } catch (Exception e) {
                LOGGER.error("Error while assigning urn to single file", e);
            }
        } else {
            if (!MCRURNManager.hasURNAssigned(object)) {
                /* assign urn to derivate */
                if (object.contains("_derivate_")) {
                    try {
                        LOGGER.info("Adding URN to all files in derivate " + object);
                        if (!urnAdder.addURNToDerivates(object)) {
                            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + USERERRORPAGE));
                            return;
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error while assigning urn to derivate '" + object + "'", e);
                    }
                } else {
                    /* assign urn to a mycore object */
                    boolean assignmentSuccessful = false;
                    try {
                        LOGGER.info("Assigning urn to object '" + object + "'");
                        assignmentSuccessful = urnAdder.addURN(object);
                    } catch (Exception e) {
                        LOGGER.info("Error while assigning urn to object '" + object + "'");
                    }
                    if (!assignmentSuccessful) {
                        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + USERERRORPAGE));
                        return;
                    }
                }
            }
        }

        String referrer = job.getRequest().getHeader("referer"); // yes, with misspelling.
        if (referrer != null && referrer.length() > 0) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(referrer));
        } else {
            job.getResponse().sendError(HttpServletResponse.SC_EXPECTATION_FAILED, "Could not get referrer from request");
        }
    }
}