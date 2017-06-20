/**
 * 
 */
package org.mycore.frontend.servlets;

import static org.mycore.access.MCRAccessManager.PERMISSION_DELETE;
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.utils.MCRRecursiveDeleter;

/**
 * @author Sebastian Hofmann; Silvio Hermann; Thomas Scheffler (yagee); Sebastian Röher
 */
public class MCRDerivateServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        checkPreConditions(request, response);
        if (response.isCommitted()) {
            return;
        }
        String derivateId = getProperty(request, "derivateid");
        if (performTask(job, getProperty(request, "todo"), derivateId, getProperty(request, "file"))) {
            String url = request.getParameter("url");
            if (url != null && ("".equals(url))) {
                response.sendError(HttpServletResponse.SC_NO_CONTENT, "Parameter 'url' is set but empty!");
                return;
            }
            if (url != null) {
                response.sendRedirect(response.encodeRedirectURL(url));
                return;
            }
            toReferrer(request, response,
                response.encodeRedirectURL(getServletBaseURL() + "MCRFileNodeServlet/" + derivateId + "/"));
        }
    }

    protected void checkPreConditions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (getProperty(request, "todo") == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter \"todo\" is not provided");
        } else if (getProperty(request, "derivateid") == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter \"derivateid\" is not provided");
        } else if (getProperty(request, "file") == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter \"file\" is not provided");
        }
    }

    private boolean performTask(MCRServletJob job, String task, String myCoreDerivateId, String file)
        throws IOException {
        if (task.equals("ssetfile")) {
            setMainFile(myCoreDerivateId, file, job.getResponse());
        } else if (task.equals("sdelfile")) {
            deleteFile(myCoreDerivateId, file, job.getResponse());
        } else {
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST,
                MessageFormat.format("The task \"{0}\" is not supported.", task));
        }
        return !job.getResponse().isCommitted();
    }

    /**
     * The method set the main file of a derivate object that is stored in the
     * server. The method use the input parameter: <b>type</b>,<b>step</b>
     * <b>se_mcrid</b> and <b>re_mcrid</b>. Access rights must be 'writedb'.
     * @param job
     *            the MCRServletJob instance
     */
    private void setMainFile(String derivateId, String file, HttpServletResponse response) throws IOException {
        if (MCRAccessManager.checkPermission(derivateId, PERMISSION_WRITE)) {
            MCRObjectID mcrid = MCRObjectID.getInstance(derivateId);
            MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(mcrid);
            der.getDerivate().getInternals().setMainDoc(file);
            MCRMetadataManager.updateMCRDerivateXML(der);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, MessageFormat.format("User has not the \""
                + PERMISSION_WRITE + "\" permission on object {0}.", derivateId));
        }
    }

    /**
     * The method delete a file from a derivate object that is stored in the
     * server. The method use the input parameter: <b>type</b>,<b>step</b>
     * <b>se_mcrid</b> and <b>re_mcrid</b>. Access rights must be 'deletedb'.
     * @param job
     *            the MCRServletJob instance
     */
    private void deleteFile(String derivateId, String file, HttpServletResponse response) throws IOException {
        if (MCRAccessManager.checkPermission(derivateId, PERMISSION_DELETE)) {
            MCRPath pathToFile = MCRPath.getPath(derivateId, file);
            if (!Files.isDirectory(pathToFile)) {
                Files.delete(pathToFile);
            } else {
                Files.walkFileTree(pathToFile, MCRRecursiveDeleter.instance());
            }
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, MessageFormat.format("User has not the \""
                + PERMISSION_DELETE + "\" permission on object {0}.", derivateId));
        }
    }

}
