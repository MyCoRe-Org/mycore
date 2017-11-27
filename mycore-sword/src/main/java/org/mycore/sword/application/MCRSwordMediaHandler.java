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

package org.mycore.sword.application;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.mets.validator.METSValidator;
import org.mycore.mets.validator.validators.ValidationException;
import org.mycore.sword.MCRSwordConstants;
import org.mycore.sword.MCRSwordUtil;
import org.swordapp.server.Deposit;
import org.swordapp.server.MediaResource;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.UriRegistry;

/**
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRSwordMediaHandler implements MCRSwordLifecycle, MCRSwordUtil.MCRFileValidator {

    protected static final Logger LOGGER = LogManager.getLogger(MCRSwordMediaHandler.class);

    protected static boolean isValidFilePath(String filePath) {
        return filePath != null && filePath.length() > 1;
    }

    protected static void checkFile(MCRPath path) throws SwordError {
        if (!Files.exists(path)) {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, HttpServletResponse.SC_NOT_FOUND,
                "The requested file '" + path + "' does not exists.");
        }
    }

    public MediaResource getMediaResourceRepresentation(String derivateID, String requestFilePath,
        Map<String, String> accept) throws SwordError, SwordServerException {
        MediaResource resultRessource;

        if (!MCRAccessManager.checkPermission(derivateID, MCRAccessManager.PERMISSION_READ)) {
            throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED,
                "You dont have the right to read from the derivate!");
        }

        if (requestFilePath != null && isValidFilePath(requestFilePath)) {
            final MCRPath path = MCRPath.getPath(derivateID, requestFilePath);
            checkFile(path);

            InputStream is = null;
            try {
                // MediaResource/Sword2 api should close the stream.
                is = Files.newInputStream(path);
                resultRessource = new MediaResource(is, Files.probeContentType(path), UriRegistry.PACKAGE_BINARY);
            } catch (IOException e) {
                LOGGER.error("Error while opening File: {}", path, e);
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e1) {
                        LOGGER.error("Could not close Stream after error. ", e);
                    }
                }
                throw new SwordError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            // if there is no file path or file is just "/" or "" then send the zipped Derivate
            resultRessource = MCRSwordUtil.getZippedDerivateMediaResource(derivateID);
        }

        MCRSessionMgr.getCurrentSession().commitTransaction();
        return resultRessource;
    }

    public void replaceMediaResource(String derivateId, String requestFilePath, Deposit deposit)
        throws SwordError, SwordServerException {
        if (!MCRAccessManager.checkPermission(derivateId, MCRAccessManager.PERMISSION_WRITE)) {
            throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED,
                "You dont have the right to write to the derivate!");
        }

        MCRPath path = MCRPath.getPath(derivateId, requestFilePath);
        if (!Files.exists(path)) {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, HttpServletResponse.SC_NOT_FOUND,
                "Cannot replace a not existing file.");
        }

        final boolean pathIsDirectory = Files.isDirectory(path);

        if (pathIsDirectory) {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "replaceMediaResource is not supported with directories");
        }

        // TODO: replace file

    }

    public void addResource(String derivateId, String requestFilePath, Deposit deposit)
        throws SwordError, SwordServerException {
        MCRPath ifsRootPath = MCRPath.getPath(derivateId, requestFilePath);
        final boolean pathIsDirectory = Files.isDirectory(ifsRootPath);
        final String depositFilename = deposit.getFilename();
        final String packaging = deposit.getPackaging();

        if (!MCRAccessManager.checkPermission(derivateId, MCRAccessManager.PERMISSION_WRITE)) {
            throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED,
                "You dont have the right to write to the derivate!");
        }

        Path tempFile = null;
        try {
            try {
                tempFile = MCRSwordUtil.createTempFileFromStream(deposit.getFilename(), deposit.getInputStream(),
                    deposit.getMd5());
            } catch (IOException e) {
                throw new SwordServerException("Could not store deposit to temp files", e);
            }

            if (packaging != null && packaging.equals(UriRegistry.PACKAGE_SIMPLE_ZIP)) {
                if (pathIsDirectory && deposit.getMimeType().equals(MCRSwordConstants.MIME_TYPE_APPLICATION_ZIP)) {
                    ifsRootPath = MCRPath.getPath(derivateId, requestFilePath);
                    try {
                        List<MCRSwordUtil.MCRValidationResult> invalidResults = MCRSwordUtil
                            .validateZipFile(this, tempFile)
                            .stream()
                            .filter(validationResult -> !validationResult.isValid())
                            .collect(Collectors.toList());

                        if (invalidResults.size() > 0) {
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, HttpServletResponse.SC_BAD_REQUEST,
                                invalidResults.stream()
                                    .map(MCRSwordUtil.MCRValidationResult::getMessage)
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(Collectors.joining(System.lineSeparator())));
                        }

                        MCRSwordUtil.extractZipToPath(tempFile, ifsRootPath);
                    } catch (IOException | NoSuchAlgorithmException | URISyntaxException e) {
                        throw new SwordServerException("Error while extracting ZIP.", e);
                    }
                } else {
                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, HttpServletResponse.SC_BAD_REQUEST,
                        "The Request makes no sense. (mime type must be " + MCRSwordConstants.MIME_TYPE_APPLICATION_ZIP
                            + " and path must be a directory)");
                }
            } else if (packaging != null && packaging.equals(UriRegistry.PACKAGE_BINARY)) {
                try {
                    MCRSwordUtil.MCRValidationResult validationResult = validate(tempFile);
                    if (!validationResult.isValid()) {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, HttpServletResponse.SC_BAD_REQUEST,
                            validationResult.getMessage().get());
                    }
                    ifsRootPath = MCRPath.getPath(derivateId, requestFilePath + depositFilename);
                    try (InputStream is = Files.newInputStream(tempFile)) {
                        Files.copy(is, ifsRootPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new SwordServerException("Error while adding file " + ifsRootPath, e);
                }
            }
        } finally {
            if (tempFile != null) {
                try {
                    LOGGER.info("Delete temp file: {}", tempFile);
                    Files.delete(tempFile);
                } catch (IOException e) {
                    LOGGER.error("Could not delete temp file: {}", tempFile, e);
                }
            }
        }
    }

    public void deleteMediaResource(String derivateId, String requestFilePath) throws SwordError, SwordServerException {
        if (!MCRAccessManager.checkPermission(derivateId, MCRAccessManager.PERMISSION_DELETE)) {
            throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED,
                "You dont have the right to delete (from) the derivate!");
        }

        if (requestFilePath == null || requestFilePath.equals("/")) {
            final MCRObjectID derivateID = MCRObjectID.getInstance(derivateId);
            if (MCRMetadataManager.exists(derivateID)) {
                final MCRDerivate mcrDerivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);
                try {
                    MCRMetadataManager.delete(mcrDerivate);
                } catch (MCRAccessException e) {
                    throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, e);
                }
            } else {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, HttpServletResponse.SC_NOT_FOUND,
                    "The requested Object '" + requestFilePath + "' does not exists.");
            }
        } else {
            final MCRPath path = MCRPath.getPath(derivateId, requestFilePath);
            checkFile(path);
            try {
                if (Files.isDirectory(path)) {
                    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } else {
                    Files.delete(path);
                }
            } catch (IOException e) {
                throw new SwordServerException("Error while deleting media resource!", e);
            }
        }
    }

    @Override
    public void init(MCRSwordLifecycleConfiguration lifecycleConfiguration) {
        //lifecycleConfiguration is not used here;
    }

    @Override
    public void destroy() {

    }

    @Override
    public MCRSwordUtil.MCRValidationResult validate(Path pathToFile) {
        // single added file name are remapped to mets.xml -> swordv2_*mets.xml
        if (pathToFile.getFileName().toString().endsWith("mets.xml")) {
            try (InputStream is = Files.newInputStream(pathToFile)) {
                METSValidator validator = new METSValidator(is);
                List<ValidationException> validateResult = validator.validate();

                if (validateResult.size() > 0) {
                    String result = validateResult.stream().map(Throwable::getMessage)
                        .collect(Collectors.joining(System.lineSeparator()));
                    return new MCRSwordUtil.MCRValidationResult(false, result);
                } else {
                    return new MCRSwordUtil.MCRValidationResult(true, null);
                }

            } catch (IOException | JDOMException e) {
                return new MCRSwordUtil.MCRValidationResult(false, "Could not read mets.xml: " + e.getMessage());
            }
        } else {
            return new MCRSwordUtil.MCRValidationResult(true, null);
        }
    }
}
