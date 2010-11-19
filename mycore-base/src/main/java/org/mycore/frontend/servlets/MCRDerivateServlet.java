/**
 * 
 */
package org.mycore.frontend.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * @author basti, shermann
 */
public class MCRDerivateServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    static Logger LOGGER = Logger.getLogger(MCRDerivateServlet.class);

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        // check what to do
        String task = job.getRequest().getParameter("todo");
        if (task == null) {
            LOGGER.error("Parameter \"todo\" is not provided");
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
        // derivateid
        String myCoreDerivateId = job.getRequest().getParameter("derivateid");
        if (myCoreDerivateId == null) {
            LOGGER.error("Parameter \"derivateid\" is not provided");
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
        // owner id
        String myCoreObjectId = job.getRequest().getParameter("objectid");
        if (myCoreObjectId == null) {
            LOGGER.error("Parameter \"objectid\" is not provided");
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
        // file to delete or to set
        String file = job.getRequest().getParameter("file");
        if (file == null) {
            LOGGER.error("Parameter \"file\" is not provided");
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST);
        }

        performTask(job, task, myCoreDerivateId, myCoreObjectId, file);
        job.getResponse().sendRedirect(
                job.getResponse().encodeRedirectURL(getBaseURL() + "servlets/MCRFileNodeServlet/" + myCoreDerivateId));
    }

    private void performTask(MCRServletJob job, String task, String myCoreDerivateId, String myCoreObjectId, String file)
            throws IOException {
        if (task.equals("ssetfile")) {
            if (MCRAccessManager.checkPermission(myCoreDerivateId, "writedb")) {
                setMainFile(myCoreDerivateId, myCoreObjectId, file);
            } else {
                LOGGER.error("User has not the \"writedb\" permission on object " + myCoreDerivateId);
                job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN);
            }
        } else if (task.equals("sdelfile")) {
            if (MCRAccessManager.checkPermission(myCoreDerivateId, "deletedb")) {
                deleteFile(myCoreDerivateId, myCoreObjectId, file);
            } else {
                LOGGER.error("User has not the \"deletedb\" permission on object " + myCoreDerivateId);
                job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN);
            }
        } else {
            LOGGER.warn("The task \"" + task + "\" is not supported");
        }
    }

    /**
     * The method set the main file of a derivate object that is stored in the
     * server. The method use the input parameter: <b>type</b>,<b>step</b>
     * <b>se_mcrid</b> and <b>re_mcrid</b>. Access rights must be 'writedb'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    private boolean setMainFile(String derivateId, String ObjectId, String file) throws IOException {
        try {
            MCRObjectID mcrid = MCRObjectID.getInstance(derivateId);
            MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(mcrid);
            der.getDerivate().getInternals().setMainDoc(file);
            MCRMetadataManager.updateMCRDerivateXML(der);
            return true;
        } catch (MCRException ex) {
            LOGGER.error("Cannot set main file in derivate " + derivateId, ex);
            return false;
        }
    }

    /**
     * The method delete a file from a derivate object that is stored in the
     * server. The method use the input parameter: <b>type</b>,<b>step</b>
     * <b>se_mcrid</b> and <b>re_mcrid</b>. Access rights must be 'deletedb'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    private boolean deleteFile(String derivateId, String ObjectId, String file) throws IOException {
        MCRDirectory rootdir = MCRDirectory.getRootDirectory(derivateId);
        try {
            rootdir.getChildByPath(file).delete();
            return true;
        } catch (Exception ex) {
            LOGGER.error("Cannot delete file " + file + " in derivate " + derivateId, ex);
            return false;
        }
    }
}
