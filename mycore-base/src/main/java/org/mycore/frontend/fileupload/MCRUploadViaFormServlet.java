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
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.streams.MCRNotClosingInputStream;
import org.mycore.frontend.MCRWebsiteWriteProtection;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet handles form based file upload.
 *
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 *
 * @version $Revision: 34120 $ $Date: 2015-12-02 23:16:17 +0100 (Mi, 02 Dez 2015) $
 *
 * @see MCRUploadHandler
 * @see MCRUploadServletDeployer
 */

public final class MCRUploadViaFormServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger(MCRUploadViaFormServlet.class);

    @Override
    public void doGetPost(MCRServletJob job) throws Exception {
        guardWebsiteCurrentlyReadOnly();

        Optional<MCRUploadHandler> uh = getUploadHandler(job);
        if (!uh.isPresent()) {
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter 'uploadId' is missing!");
            return;
        }

        MCRUploadHandler handler = uh.get();
        LOGGER.info("UploadHandler form based file upload for ID " + handler.getID());

        handleUploadedFiles(handler, job.getRequest().getParts());

        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(handler.getRedirectURL()));
        handler.finishUpload();
        handler.unregister();
    }

    private void guardWebsiteCurrentlyReadOnly() {
        if (MCRWebsiteWriteProtection.isActive()) {
            throw new RuntimeException("System is currently in read-only mode");
        }
    }

    private Optional<MCRUploadHandler> getUploadHandler(MCRServletJob job) {
        return Optional.ofNullable(job.getRequest().getParameter("uploadId")).map(MCRUploadHandlerManager::getHandler);
    }

    private void handleUploadedFiles(MCRUploadHandler handler, Collection<Part> files) throws Exception, IOException {
        int numFiles = (int) files.stream().map(Part::getSubmittedFileName).filter(Objects::nonNull).count();
        LOGGER.info("UploadHandler uploading " + numFiles + " file(s)");
        handler.startUpload(numFiles);

        MCRSession session = MCRSessionMgr.getCurrentSession();
        session.commitTransaction();

        for (Part file : files) {
            try {
                handleUploadedFile(handler, file);
            } finally {
                file.delete();
            }
        }

        session.beginTransaction();
    }

    private void handleUploadedFile(MCRUploadHandler handler, Part file) throws IOException, Exception {
        String submitted = file.getSubmittedFileName();
        if (submitted == null || "".equals(submitted)) {
            return;
        }
        try (InputStream in = file.getInputStream()) {

            String path = MCRUploadHelper.getFileName(submitted);

            if (requireDecompressZip(path)) {
                handleZipFile(handler, in);
            } else {
                handleUploadedFile(handler, file.getSize(), path, in);
            }
        }
    }

    private boolean requireDecompressZip(String path) {
        return MCRConfiguration.instance().getBoolean("MCR.FileUpload.DecompressZip", true)
            && path.toLowerCase(Locale.ROOT).endsWith(".zip");
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
            if (entry.isDirectory()) {
                LOGGER.debug("UploadServlet skipping ZIP entry " + path + ", is a directory");
            } else {
                handler.incrementNumFiles();
                handleUploadedFile(handler, entry.getSize(), path, nis);
            }
        }
        handler.decrementNumFiles(); //ZIP file does not count
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
