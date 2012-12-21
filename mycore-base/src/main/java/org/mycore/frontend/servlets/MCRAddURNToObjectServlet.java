/**
 * 
 */
package org.mycore.frontend.servlets;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.urn.MCRURNAdder;
import org.mycore.services.urn.MCRURNManager;

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

    private static final String MCRIDERRORPAGE = PAGEDIR
            + MCRConfiguration.instance().getString("MCR.SWF.PageErrorMcrid", "editor_error_mcrid.xml");

    private static final String USERERRORPAGE = PAGEDIR
            + MCRConfiguration.instance().getString("MCR.SWF.PageErrorUser", "editor_error_user.xml");

    @Override
    protected void doGetPost(MCRServletJob job) throws IOException {
        String object = job.getRequest().getParameter("object");
        String target = job.getRequest().getParameter("target");
        String xpath = job.getRequest().getParameter("xpath");

        if (object == null) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + MCRIDERRORPAGE));
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
                    if (xpath != null && xpath.length() > 0) {
                        try {
                            LOGGER.info("Assigning urn with xpath '" + xpath + "' to object '" + object + "'");
                            assignmentSuccessful = urnAdder.addURN(object, xpath);
                        } catch (Exception e) {
                            LOGGER.error("Error while Assigning urn with xpath '" + xpath + "' to object '" + object + "'", e);
                        }
                    } else {
                        try {
                            LOGGER.info("Assigning urn to object '" + object + "'");
                            assignmentSuccessful = urnAdder.addURN(object);
                        } catch (Exception e) {
                            LOGGER.info("Error while assigning urn to object '" + object + "'");
                        }
                    }

                    if (!assignmentSuccessful) {
                        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + USERERRORPAGE));
                        return;
                    }
                }
            }
        }
        returnToMetadataView(job, object);
    }

    /** Returns to the metadata view this servlet was called from */
    private void returnToMetadataView(MCRServletJob job, String objectId) throws IOException {
        String href = null;

        /* object is a derivate */
        if (objectId.contains("_derivate_")) {
            MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(objectId));
            href = derivate.getDerivate().getMetaLink().getXLinkHref();
        }
        /* object is ordinary mcr object */
        else {
            href = objectId;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(getBaseURL()).append("receive/").append(href);
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(sb.toString()));
    }

}