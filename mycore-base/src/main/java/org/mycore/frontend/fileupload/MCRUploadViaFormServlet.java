/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.frontend.fileupload;

import java.io.InputStream;
import java.io.Serial;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRTransactionManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.streams.MCRNotClosingInputStream;
import org.mycore.frontend.MCRWebsiteWriteProtection;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

import jakarta.persistence.EntityTransaction;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

/**
 * This servlet handles form based file upload.
 *
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 *
 *
 * @see MCRUploadHandler
 * @see MCRUploadServletDeployer
 */

public final class MCRUploadViaFormServlet extends MCRServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void doGetPost(MCRServletJob job) throws Exception {
        guardWebsiteCurrentlyReadOnly();

        Optional<MCRUploadHandler> uh = getUploadHandler(job);
        if (!uh.isPresent()) {
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter 'uploadId' is missing!");
            return;
        }

        MCRUploadHandler handler = uh.get();
        LOGGER.info("UploadHandler form based file upload for ID {}", handler::getID);

        handleUploadedFiles(handler, job.getRequest().getParts());

        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(handler.getRedirectURL()));
        handler.finishUpload();
        handler.unregister();
    }

    private void guardWebsiteCurrentlyReadOnly() {
        if (MCRWebsiteWriteProtection.isActive()) {
            throw new MCRConfigurationException("System is currently in read-only mode");
        }
    }

    private Optional<MCRUploadHandler> getUploadHandler(MCRServletJob job) {
        return Optional.ofNullable(job.getRequest().getParameter("uploadId")).map(MCRUploadHandlerManager::getHandler);
    }

    private void handleUploadedFiles(MCRUploadHandler handler, Collection<Part> files) throws Exception {
        int numFiles = (int) files.stream().map(Part::getSubmittedFileName).filter(Objects::nonNull).count();
        LOGGER.info("UploadHandler uploading {} file(s)", numFiles);
        handler.startUpload(numFiles);
        MCRTransactionManager.commitTransactions();

        for (Part file : files) {
            try {
                handleUploadedFile(handler, file);
            } finally {
                file.delete();
            }
        }

        MCRTransactionManager.beginTransactions();
    }

    private void handleUploadedFile(MCRUploadHandler handler, Part file) throws Exception {
        String submitted = file.getSubmittedFileName();
        if (submitted == null || Objects.equals(submitted, "")) {
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
        return MCRConfiguration2.getBoolean("MCR.FileUpload.DecompressZip").orElse(true)
            && path.toLowerCase(Locale.ROOT).endsWith(".zip");
    }

    private void handleUploadedFile(MCRUploadHandler handler, long size, String path, InputStream in) throws Exception {
        LOGGER.info("UploadServlet uploading {}", path);
        MCRUploadHelper.checkPathName(path);

        EntityTransaction tx = MCRUploadHelper.startTransaction();
        try {
            handler.receiveFile(path, in, size, null);
            MCRUploadHelper.commitTransaction(tx);
        } catch (Exception exc) {
            MCRUploadHelper.rollbackAnRethrow(tx, exc);
        }
    }

    private void handleZipFile(MCRUploadHandler handler, InputStream in) throws Exception {
        ZipInputStream zis = new ZipInputStream(in);
        MCRNotClosingInputStream nis = new MCRNotClosingInputStream(zis);
        for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
            String path = convertAbsolutePathToRelativePath(entry.getName());
            if (entry.isDirectory()) {
                LOGGER.debug("UploadServlet skipping ZIP entry {}, is a directory", path);
            } else {
                handler.incrementNumFiles();
                handleUploadedFile(handler, entry.getSize(), path, nis);
            }
        }
        handler.decrementNumFiles(); //ZIP file does not count
        nis.reallyClose();
    }

    private String convertAbsolutePathToRelativePath(String absolutePath) {
        int pos = absolutePath.indexOf(':');
        String relativePath = (pos >= 0) ? absolutePath.substring(pos + 1) : absolutePath;
        while (relativePath.startsWith("\\") || relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        return relativePath;
    }

}
