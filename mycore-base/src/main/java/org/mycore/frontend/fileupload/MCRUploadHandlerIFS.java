/*
 * 
 * $Revision$ $Date$
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
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * handles uploads via the UploadApplet and store files directly into the IFS.
 * 
 * @author Thomas Scheffler (yagee)
 * @author Frank L\u00FCtzenkirchen
 * 
 * @version $Revision$ $Date$
 * 
 * @see MCRUploadHandler
 */
public class MCRUploadHandlerIFS extends MCRUploadHandler {

    private static final Logger LOGGER = Logger.getLogger(MCRUploadHandlerIFS.class);

    private static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    private static final String ID_TYPE = "derivate";

    private static final String ID_PROJECT = MCRConfiguration.instance().getString("MCR.SWF.Project.ID", "MCR");

    protected String documentID;

    protected String derivateID;

    protected MCRDerivate derivate;

    protected MCRPath rootDir;

    public MCRUploadHandlerIFS(String documentID, String derivateID, String returnURL) {
        super();
        this.url = returnURL;
        this.derivateID = derivateID;
        this.documentID = documentID;
    }

    @Override
    public void startUpload(int numFiles) throws Exception {
        LOGGER.debug("upload starting, expecting " + numFiles + " files");

        MCRObjectID derivateID = getOrCreateDerivateID();

        if (MCRMetadataManager.exists(derivateID))
            this.derivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);
        else
            this.derivate = createDerivate(derivateID);

        getOrCreateRootDirectory();

        LOGGER.debug("uploading into " + this.derivateID + " of " + this.documentID);
    }

    private MCRObjectID getOrCreateDerivateID() {
        if (derivateID == null) {
            MCRObjectID oid = MCRObjectID.getNextFreeId(ID_PROJECT + '_' + ID_TYPE);
            this.derivateID = oid.toString();
            return oid;
        } else
            return MCRObjectID.getInstance(derivateID);
    }

    private MCRDerivate createDerivate(MCRObjectID derivateID) throws MCRPersistenceException, IOException {
        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(derivateID);
        derivate.setLabel("data object from " + documentID);

        String schema = CONFIG.getString("MCR.Metadata.Config.derivate", "datamodel-derivate.xml").replaceAll(".xml",
            ".xsd");
        derivate.setSchema(schema);

        MCRMetaLinkID linkId = new MCRMetaLinkID();
        linkId.setSubTag("linkmeta");
        linkId.setReference(documentID, null, null);
        derivate.getDerivate().setLinkMeta(linkId);

        MCRMetaIFS ifs = new MCRMetaIFS();
        ifs.setSubTag("internal");
        ifs.setSourcePath(null);
        derivate.getDerivate().setInternals(ifs);

        LOGGER.debug("Creating new derivate with ID " + this.derivateID);
        MCRMetadataManager.create(derivate);

        setDefaultPermissions(derivateID);

        return derivate;
    }

    protected void setDefaultPermissions(MCRObjectID derivateID) {
        if (CONFIG.getBoolean("MCR.Access.AddDerivateDefaultRule", true)) {
            MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
            Collection<String> configuredPermissions = AI.getAccessPermissionsFromConfiguration();
            for (String permission : configuredPermissions) {
                MCRAccessManager.addRule(derivateID, permission, MCRAccessManager.getTrueRule(),
                    "default derivate rule");
            }
        }
    }

    private void getOrCreateRootDirectory() throws FileSystemException {
        this.rootDir = MCRPath.getPath(derivateID, "/");
        if (Files.notExists(rootDir)) {
            rootDir.getFileSystem().createRoot(derivateID);
        }
    }

    @Override
    public boolean acceptFile(String path, String checksum, long length) throws Exception {
        LOGGER.debug("incoming acceptFile request: " + path + " " + checksum + " " + length + " bytes");
        boolean shouldAcceptFile = true;
        MCRPath child = MCRPath.toMCRPath(rootDir.resolve(path));
        if (Files.isRegularFile(child)) {
            @SuppressWarnings("rawtypes")
            MCRFileAttributes attrs = Files.readAttributes(child, MCRFileAttributes.class);
            shouldAcceptFile = attrs.size() != length
                || !(checksum.equals(attrs.md5sum()) && child.getFileSystem().verifies(child, attrs));
        }
        LOGGER.debug("Should the client send this file? " + shouldAcceptFile);
        return shouldAcceptFile;
    }

    @Override
    public synchronized long receiveFile(String path, InputStream in, long length, String checksum) throws Exception {
        LOGGER.debug("incoming receiveFile request: " + path + " " + checksum + " " + length + " bytes");

        Path tempFile = Files.createTempFile("upload" + derivateID + checksum, ".stream");
        try {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            long myLength = Files.size(tempFile);
            if (length != myLength) {
                throw new IOException("Length of transmitted data does not match promised length: " + myLength + "!="
                    + length);
            }
            startTransaction();
            MCRPath file = getFile(path);
            Files.copy(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
            commitTransaction();
            return myLength;
        } catch (Exception e) {
            LOGGER.error("Error while uploading file: " + path, e);
            try {
                rollbackTransaction();
            } catch (Exception e2) {
                LOGGER.error("Error while rolling back transaction", e);
            }
            return 0;
        } finally {
            if (Files.exists(tempFile)) {
                Files.delete(tempFile);
            }
        }
    }

    private MCRPath getFile(String path) throws IOException {
        MCRPath pathToFile = MCRPath.toMCRPath(rootDir.resolve(path));
        MCRPath parentDirectory = pathToFile.getParent();
        if (!Files.isDirectory(parentDirectory)) {
            Files.createDirectories(parentDirectory);
        }
        return pathToFile;
    }

    @Override
    public synchronized void finishUpload() throws Exception {
        if (this.derivate == null) {
            return;
        }
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(rootDir)) {
            if (dirStream.iterator().hasNext()) {
                updateMainFile();
            } else {
                deleteEmptyDerivate();
            }
        }
    }

    private void deleteEmptyDerivate() {
        LOGGER.warn("No files were uploaded, delete entry in database for " + derivate.getId().toString()
            + " and return:");
        MCRMetadataManager.deleteMCRDerivate(derivate.getId());
    }

    private void updateMainFile() throws IOException {
        String mainFile = derivate.getDerivate().getInternals().getMainDoc();
        if ((mainFile == null) || mainFile.trim().isEmpty()) {
            mainFile = getPathOfMainFile();
            LOGGER.debug("Setting main file to " + mainFile);
            derivate.getDerivate().getInternals().setMainDoc(mainFile);
            MCRMetadataManager.updateMCRDerivateXML(derivate);
        }
    }

    protected String getPathOfMainFile() throws IOException {
        MainFileFinder mainFileFinder = new MainFileFinder(rootDir);
        Files.walkFileTree(rootDir, mainFileFinder);
        Path mainFile = mainFileFinder.getMainFile();
        return mainFile == null ? "" : mainFile.subpath(0, mainFile.getNameCount()).toString();
    }

    private static class MainFileFinder extends SimpleFileVisitor<Path> {
        private Path mainFile;

        private Path rootPath;

        public MainFileFinder(Path rootPath) {
            this.rootPath = rootPath;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (mainFile != null) {
                if (mainFile.getFileName().compareTo(file.getFileName()) > 0) {
                    mainFile = file;
                }
            } else {
                mainFile = file;
            }
            return super.visitFile(file, attrs);
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            FileVisitResult result = super.postVisitDirectory(dir, exc);
            if (mainFile != null) {
                return FileVisitResult.TERMINATE;
            }
            return result;
        }

        public Path getMainFile() {
            return mainFile;
        }
    }
}
