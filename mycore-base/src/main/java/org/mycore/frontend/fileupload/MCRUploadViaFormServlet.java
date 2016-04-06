/*
 *
 * $Revision: 34120 $ $Date: 2015-12-02 23:16:17 +0100 (Mi, 02 Dez 2015) $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.fileupload;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.streams.MCRNotClosingInputStream;
import org.mycore.frontend.MCRWebsiteWriteProtection;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.editor.MCRRequestParameters;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

/**
 * This servlet handles form based file upload.
 *
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 *
 * @version $Revision: 34120 $ $Date: 2015-12-02 23:16:17 +0100 (Mi, 02 Dez 2015) $
 *
 * @see org.mycore.frontend.fileupload.MCRUploadHandler
 */

public final class MCRUploadViaFormServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRUploadViaFormServlet.class);

    @Override
    public void doGetPost(MCRServletJob job) throws Exception {
        guardWebsiteCurrentlyReadOnly();

        MCREditorSubmission sub = (MCREditorSubmission) job.getRequest().getAttribute("MCREditorSubmission");
        MCRRequestParameters rp = sub == null ? new MCRRequestParameters(job.getRequest()) : sub.getParameters();
        MCRUploadHandler handler = getUploadHandler(rp);
        LOGGER.info("UploadHandler form based file upload for ID " + handler.getID());

        List<FileItem> files = getUploadedFiles(rp);
        handleUploadedFiles(handler, files);

        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(handler.getRedirectURL()));
        handler.finishUpload();
        handler.unregister();
    }

    private List<FileItem> getUploadedFiles(MCRRequestParameters rp) {
        List<FileItem> files = new ArrayList<FileItem>();
        for (FileItem file : rp.getFileList())
            if (file.getSize() > 0)
                files.add(file);
        return files;
    }

    private void guardWebsiteCurrentlyReadOnly() throws IOException {
        if (MCRWebsiteWriteProtection.isActive())
            throw new RuntimeException("System is currently in read-only mode");
    }

    private MCRUploadHandler getUploadHandler(MCRRequestParameters rp) {
        String uploadId = rp.getParameter("uploadId");

        if ((uploadId != null) && !uploadId.isEmpty())
            return MCRUploadHandlerManager.getHandler(uploadId);
        else
            return createUploadHandler(rp);
    }

    /* I actually don't like this, because the type of handler and parameters are hard-coded */
    private MCRUploadHandler createUploadHandler(MCRRequestParameters rp) {
        String parentObjectID = rp.getParameter("parentObjectID");
        String derivateID = getSubmittedDerivateID(rp);

        LOGGER.info("Create missing upload handler for " + parentObjectID + " derivateID " + derivateID);
        guardAgainstMissingPermissions(parentObjectID, derivateID);

        String cancelUrl = rp.getParameter("cancelUrl");
        return new MCRUploadHandlerIFS(parentObjectID, derivateID, cancelUrl);
    }

    private String getSubmittedDerivateID(MCRRequestParameters rp) {
        String derivateID = rp.getParameter("derivateID");
        if ((derivateID != null) && derivateID.trim().isEmpty())
            derivateID = null;
        return derivateID;
    }

    private void guardAgainstMissingPermissions(String parentObjectID, String derivateID) {
        if (!MCRAccessManager.checkPermission(parentObjectID, PERMISSION_WRITE))
            throw new MCRPersistenceException(
                "You do not have \"" + PERMISSION_WRITE + "\" permission on " + parentObjectID + ".");

        if ((derivateID != null) && !MCRAccessManager.checkPermission(derivateID, PERMISSION_WRITE))
            throw new MCRPersistenceException(
                "You do not have \"" + PERMISSION_WRITE + "\" permission on " + derivateID + ".");
    }

    private void handleUploadedFiles(MCRUploadHandler handler, List<FileItem> files) throws Exception, IOException {
        int numFiles = files.size();
        LOGGER.info("UploadHandler uploading " + numFiles + " file(s)");
        handler.startUpload(numFiles);

        MCRSession session = MCRSessionMgr.getCurrentSession();
        session.commitTransaction();

        for (FileItem file : files)
            handleUploadedFile(handler, file);

        session.beginTransaction();
    }

    private void handleUploadedFile(MCRUploadHandler handler, FileItem file) throws IOException, Exception {
        InputStream in = file.getInputStream();
        String path = MCRUploadHelper.getFileName(file.getName());

        if (path.toLowerCase(Locale.ROOT).endsWith(".zip"))
            handleZipFile(handler, in);
        else
            handleUploadedFile(handler, file.getSize(), path, in);
    }

    private void handleUploadedFile(MCRUploadHandler handler, long size, String path, InputStream in) throws Exception {
        LOGGER.info("UploadServlet uploading " + path);
        MCRUploadHelper.checkPathName(path);

        Transaction tx = MCRUploadHelper.startTransaction();
        try {
            handler.receiveFile(path, in, size, null);
            MCRUploadHelper.commitTransaction(tx);
        } catch (Exception exc) {
            MCRUploadHelper.rollbackAnRethrow(tx, exc);
        }
    }

    private void handleZipFile(MCRUploadHandler handler, InputStream in) throws IOException, Exception {
        ZipInputStream zis = new ZipInputStream(in);
        MCRNotClosingInputStream nis = new MCRNotClosingInputStream(zis);

        for (ZipEntry entry; (entry = zis.getNextEntry()) != null;) {
            String path = convertAbsolutePathToRelativePath(entry.getName());

            if (entry.isDirectory())
                LOGGER.debug("UploadServlet skipping ZIP entry " + path + ", is a directory");
            else
                handleUploadedFile(handler, entry.getSize(), path, nis);
        }
        nis.reallyClose();
    }

    private String convertAbsolutePathToRelativePath(String path) {
        int pos = path.indexOf(":");
        if (pos >= 0) {
            path = path.substring(pos + 1);
        }
        while (path.startsWith("\\") || path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }
}
