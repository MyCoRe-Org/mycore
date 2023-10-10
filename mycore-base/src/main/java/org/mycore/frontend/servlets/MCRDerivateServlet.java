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
import java.util.Locale;
import java.util.Objects;

import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.utils.MCRRecursiveDeleter;
import org.mycore.frontend.fileupload.MCRUploadHelper;
import org.mycore.services.i18n.MCRTranslation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Sebastian Hofmann; Silvio Hermann; Thomas Scheffler (yagee); Sebastian Röher
 */
public class MCRDerivateServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    public static final String TODO_SMOVFILE = "smovfile";

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        checkPreConditions(request, response);
        if (response.isCommitted()) {
            return;
        }
        String derivateId = getProperty(request, "derivateid");
        if (performTask(job, getProperty(request, "todo"), derivateId, getProperty(request, "file"),
            getProperty(request, "file2"))) {
            String url = request.getParameter("url");
            if ((Objects.equals(url, ""))) {
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
        } else if (getProperty(request, "todo").equals(TODO_SMOVFILE) && getProperty(request, "file2") == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter \"file2\" is not provided");
        }
    }

    private boolean performTask(MCRServletJob job, String task, String myCoreDerivateId, String file, String file2)
        throws IOException, MCRAccessException {
        switch (task) {
            case "ssetfile" -> setMainFile(myCoreDerivateId, file, job.getResponse());
            case "sdelfile" -> deleteFile(myCoreDerivateId, file, job.getResponse());
            case TODO_SMOVFILE -> moveFile(myCoreDerivateId, file, file2, job.getResponse());
            default -> job.getResponse()
                .sendError(HttpServletResponse.SC_BAD_REQUEST,
                    String.format(Locale.ENGLISH, "The task \"%s\" is not supported.", task));
        }
        return !job.getResponse().isCommitted();
    }

    /**
     * The method set the main file of a derivate object that is stored in the
     * server. The method use the input parameter: <b>type</b>,<b>step</b>
     * <b>se_mcrid</b> and <b>re_mcrid</b>. Access rights must be 'writedb'.
     */
    private void setMainFile(String derivateId, String file, HttpServletResponse response)
        throws IOException, MCRAccessException {
        if (MCRAccessManager.checkPermission(derivateId, PERMISSION_WRITE)) {
            MCRObjectID mcrid = MCRObjectID.getInstance(derivateId);
            MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(mcrid);
            der.getDerivate().getInternals().setMainDoc(file);
            MCRMetadataManager.update(der);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, String.format(Locale.ENGLISH, "User has not the \""
                + PERMISSION_WRITE + "\" permission on object %s.", derivateId));
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
            response.sendError(HttpServletResponse.SC_FORBIDDEN, String.format(Locale.ENGLISH, "User has not the \""
                + PERMISSION_DELETE + "\" permission on object %s.", derivateId));
        }
    }

    private void moveFile(String derivateIdStr, String file, String target, HttpServletResponse response)
        throws IOException {
        if (!MCRAccessManager.checkPermission(derivateIdStr, PERMISSION_DELETE)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, String.format(Locale.ENGLISH,
                "User has not the \"%s\" permission on object %s.", PERMISSION_DELETE, derivateIdStr));
            return;
        }

        if (!MCRAccessManager.checkPermission(derivateIdStr, PERMISSION_WRITE)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, String.format(Locale.ENGLISH,
                "User has not the \"%s\" permission on object %s.", PERMISSION_WRITE, derivateIdStr));
            return;
        }
        MCRPath pathFrom = MCRPath.getPath(derivateIdStr, file);
        MCRPath pathTo = MCRPath.getPath(derivateIdStr, target);

        if (Files.exists(pathTo)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, String.format(Locale.ENGLISH,
                "The File %s already exists!", pathTo));
            return;
        }

        if (Files.isDirectory(pathFrom)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, String.format(Locale.ENGLISH,
                "Renaming directory %s is not supported!", pathFrom));
            return;
        }

        try {
            MCRUploadHelper.checkPathName(pathTo.getRoot().relativize(pathTo).toString(), true);
        } catch (MCRException ex) {
            String translatedMessage = MCRTranslation.translate("IFS.invalid.fileName", pathTo.getOwnerRelativePath());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, translatedMessage);
            return;
        }

        boolean updateMainFile = false;
        MCRObjectID derivateId = MCRObjectID.getInstance(derivateIdStr);

        // check if the main file is moved, need to be done before the move,
        // because the main file gets lost after the move.
        updateMainFile = isMainFileUpdateRequired(pathFrom, derivateId);

        // this should always be a MCRPath, if not then ClassCastException is okay
        MCRPath resultingFile = (MCRPath) Files.move(pathFrom, pathTo);

        if (updateMainFile) {
            setMainFile(derivateId, resultingFile);
        }
    }

    private void setMainFile(MCRObjectID derivateId, MCRPath resultingFile) {
        // read derivate again, because it was changed by Files.move
        // (The maindoc gets removed, because the file does not exist anymore)
        MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateId);
        MCRMetaIFS internals = derivate.getDerivate().getInternals();
        internals.setMainDoc(resultingFile.getOwnerRelativePath());
        try {
            MCRMetadataManager.update(derivate);
        } catch (MCRAccessException e) {
            throw new MCRException("Error while updating main file", e);
        }
    }

    private boolean isMainFileUpdateRequired(MCRPath pathFrom, MCRObjectID derivateId) {
        if (MCRMetadataManager.exists(derivateId)) {
            MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateId);
            MCRMetaIFS internals = derivate.getDerivate().getInternals();
            String mainDoc = internals.getMainDoc();

            // the getOwnerRelativePath() method returns with a leading slash, but the mainDoc not.
            return mainDoc != null && mainDoc.equals(pathFrom.getOwnerRelativePath().substring(1));
        }
        return false;
    }
}
