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
        switch (task) {
            case "ssetfile":
                setMainFile(myCoreDerivateId, file, job.getResponse());
                break;
            case "sdelfile":
                deleteFile(myCoreDerivateId, file, job.getResponse());
                break;
            default:
                job.getResponse()
                    .sendError(HttpServletResponse.SC_BAD_REQUEST,
                        MessageFormat.format("The task \"{0}\" is not supported.", task));
                break;
        }
        return !job.getResponse().isCommitted();
    }

    /**
     * The method set the main file of a derivate object that is stored in the
     * server. The method use the input parameter: <b>type</b>,<b>step</b>
     * <b>se_mcrid</b> and <b>re_mcrid</b>. Access rights must be 'writedb'.
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
