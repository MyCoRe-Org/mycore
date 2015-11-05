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
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
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
public class MCRSwordMediaHandler implements MCRSwordLifecycle {

    protected final static Logger LOGGER = Logger.getLogger(MCRSwordMediaHandler.class);

    private MCRSwordLifecycleConfiguration configuration;

    protected static boolean isValidFilePath(String filePath) {
        return filePath != null && filePath.length() > 1;
    }

    protected static void checkFile(MCRPath path) throws SwordError {
        if (!Files.exists(path)) {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, HttpServletResponse.SC_NOT_FOUND, "The requested file '" + path.toString() + "' does not exists.");
        }
    }

    public MediaResource getMediaResourceRepresentation(String derivateID, String requestFilePath, Map<String, String> accept) throws SwordError, SwordServerException {
        MediaResource resultRessource;

        if(!MCRAccessManager.checkPermission(derivateID, MCRAccessManager.PERMISSION_READ)){
            throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "You dont have the right to read from the derivate!");
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
                LOGGER.error("Error while opening File: " + path, e);
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

        return resultRessource;
    }

    public void replaceMediaResource(String derivateId, String requestFilePath, Deposit deposit) throws SwordError, SwordServerException {
        if(!MCRAccessManager.checkPermission(derivateId, MCRAccessManager.PERMISSION_WRITE)){
            throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "You dont have the right to write to the derivate!");
        }

        MCRPath path = MCRPath.getPath(derivateId, requestFilePath);
        if (!Files.exists(path)) {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, HttpServletResponse.SC_NOT_FOUND, "Cannot replace a not existing file.");
        }

        final boolean pathIsDirectory = Files.isDirectory(path);
        final String depositFilename = deposit.getFilename();

        if (pathIsDirectory) {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "replaceMediaResource is not supported with directories");
        }

        // TODO: replace file

    }

    public void addResource(String derivateId, String requestFilePath, Deposit deposit) throws SwordError, SwordServerException {
        MCRPath path = MCRPath.getPath(derivateId, requestFilePath);
        final boolean pathIsDirectory = Files.isDirectory(path);
        final String depositFilename = deposit.getFilename();
        final String packaging = deposit.getPackaging();

        if(!MCRAccessManager.checkPermission(derivateId, MCRAccessManager.PERMISSION_WRITE)){
            throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "You dont have the right to write to the derivate!");
        }

        if (packaging != null && packaging.equals(UriRegistry.PACKAGE_SIMPLE_ZIP)) {
            if (pathIsDirectory && deposit.getMimeType().equals(MCRSwordConstants.MIME_TYPE_APPLICATION_ZIP)) {
                path = MCRPath.getPath(derivateId, requestFilePath);
                try {
                    MCRSwordUtil.extractZipToPath(deposit.getInputStream(), path);
                } catch (IOException | NoSuchAlgorithmException | URISyntaxException e) {
                    throw new SwordServerException("Error while extracting ZIP.", e);
                }
            } else {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, HttpServletResponse.SC_BAD_REQUEST, "The Request makes no sense. (mime type must be " + MCRSwordConstants.MIME_TYPE_APPLICATION_ZIP + " and path must be a directory)");
            }
        } else if (packaging.equals(UriRegistry.PACKAGE_BINARY)) {
            path = MCRPath.getPath(derivateId, requestFilePath + depositFilename);
            try {
                Files.copy(deposit.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                LOGGER.error("Could not add " + path);
            }
        }

    }

    public void deleteMediaResource(String derivateId, String requestFilePath) throws SwordError, SwordServerException {
        if(!MCRAccessManager.checkPermission(derivateId, MCRAccessManager.PERMISSION_DELETE)){
            throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "You dont have the right to delete (from) the derivate!");
        }

        if (requestFilePath == null || requestFilePath.equals("/")) {
            final MCRObjectID derivateID = MCRObjectID.getInstance(derivateId);
            if (MCRMetadataManager.exists(derivateID)) {
                final MCRDerivate mcrDerivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);
                MCRMetadataManager.delete(mcrDerivate);
            } else {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, HttpServletResponse.SC_NOT_FOUND, "The requested Object '" + requestFilePath + "' does not exists.");
            }
        } else {
            final MCRPath path = MCRPath.getPath(derivateId, requestFilePath);
            checkFile(path);
            try {
                if(Files.isDirectory(path)){
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
        this.configuration = lifecycleConfiguration;
    }

    @Override
    public void destroy() {

    }
}


