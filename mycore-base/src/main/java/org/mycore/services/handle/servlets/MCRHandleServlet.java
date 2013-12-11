/**
 * 
 */
package org.mycore.services.handle.servlets;

import java.text.MessageFormat;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.services.handle.MCRHandleManager;


/**
 * @author shermann
 *
 */
public class MCRHandleServlet extends MCRServlet {
    private static final Logger LOGGER = Logger.getLogger(MCRHandleServlet.class);

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        if (!MCRAccessManager.checkPermission("request-handle")) {
            job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, "Sorry but you do not have sufficient permissions.");
            return;
        }

        String derivate = job.getRequest().getParameter("derivate");
        if ((derivate == null || derivate.length() == 0) && !MCRMetadataManager.exists(MCRObjectID.getInstance(derivate))) {
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        MCRDerivate derivateObject = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(derivate));

        for (MCRFile file : MCRFilesystemNode.getRootNode(derivate).getFiles()) {
            try {
                if (MCRHandleManager.isHandleRequested(derivateObject, file.getAbsolutePath()) || file.getName().equals("mets.xml")) {
                    LOGGER.info(MessageFormat.format("Handle already set for \"{0}@{1}\"", file.getAbsolutePath(), derivate));
                    continue;
                }
                LOGGER.info(MessageFormat.format("Requesting handle for {0}@{1}", file.getAbsolutePath(), derivate));
                MCRHandleManager.requestHandle(file);
            } catch (Throwable e) {
                LOGGER.error("Could not request handle for file " + file);
            }
        }
        toReferrer(job.getRequest(), job.getResponse());
    }
}