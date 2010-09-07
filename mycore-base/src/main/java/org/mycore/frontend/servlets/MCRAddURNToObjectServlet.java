/**
 * 
 */
package org.mycore.frontend.servlets;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.services.urn.MCRURNManager;
import org.mycore.services.urn.MCRURNAdder;

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
    protected void doGetPost(MCRServletJob job) throws Exception {
        String object = job.getRequest().getParameter("object");
        String target = job.getRequest().getParameter("target");

        if (object == null) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + MCRIDERRORPAGE));
            return;
        }

        MCRURNAdder urn = new MCRURNAdder();

        if (target != null && target.equals("file")) {
            LOGGER.info("Adding URN to single file");
            String path = job.getRequest().getParameter("path");
            String fileId = job.getRequest().getParameter("fileId");
            if (path == null || fileId == null) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + USERERRORPAGE));
                return;
            }
            urn.addURNToSingleFile(object, path, fileId);
        } else {
            if (!MCRURNManager.hasURNAssigned(object)) {
                if (object.indexOf("_derivate_") != -1) {
                    LOGGER.info("Adding URN to all files in derivate " + object);
                    if (!urn.addURNToDerivates(object)) {
                        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + USERERRORPAGE));
                        return;
                    }
                } else {
                    if (!urn.addURN(object)) {
                        LOGGER.info("Adding URN to metadata of object " + object);
                        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + USERERRORPAGE));
                        return;
                    }
                }
            }
        }
        returnToMetadataView(job, object);
    }

    /** Returns to the metadata view this servlet was called from */
    private void returnToMetadataView(MCRServletJob job, String objectId) throws Exception {
        String href = null;

        /* object is a derivate */
        if (objectId.indexOf("_derivate_") != -1) {
            MCRDerivate derivate = new MCRDerivate();
            derivate.receiveFromDatastore(objectId);
            href = derivate.getDerivate().getMetaLink().getXLinkHref();
        }
        /* object is ordinary mcr object */
        else {
            href = objectId;
        }

        StringBuffer sb = new StringBuffer();
        sb.append(getBaseURL()).append("receive/").append(href);
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(sb.toString()));
    }

}