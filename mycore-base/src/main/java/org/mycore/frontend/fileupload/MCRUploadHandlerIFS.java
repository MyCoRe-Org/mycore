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

package org.mycore.frontend.fileupload;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.processing.MCRProcessableStatus;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * handles uploads and store files directly into the IFS.
 *
 * @author Thomas Scheffler (yagee)
 * @author Frank L\u00FCtzenkirchen
 * @version $Revision$ $Date$
 * @see MCRUploadHandler
 */
public class MCRUploadHandlerIFS extends MCRUploadHandler {

    private static final Logger LOGGER = LogManager.getLogger(MCRUploadHandlerIFS.class);

    private static final String ID_TYPE = "derivate";

    private static final String FILE_PROCESSOR_PROPERTY = "MCR.MCRUploadHandlerIFS.FileProcessors";

    private static final List<MCRPostUploadFileProcessor> FILE_PROCESSORS = initProcessorList();

    protected String documentID;

    protected String derivateID;

    protected MCRDerivate derivate;

    protected MCRPath rootDir;

    private int filesUploaded;

    public MCRUploadHandlerIFS(String documentID, String derivateID) {
        super();
        this.documentID = Objects.requireNonNull(documentID, "Document ID may not be 'null'.");
        this.derivateID = derivateID;
        this.setName(this.derivateID);
    }

    public MCRUploadHandlerIFS(String documentID, String derivateID, String returnURL) {
        this(documentID, derivateID);
        this.url = Objects.requireNonNull(returnURL, "Return URL may not be 'null'.");
    }

    private static List<MCRPostUploadFileProcessor> initProcessorList() {
        return MCRConfiguration2.getString(FILE_PROCESSOR_PROPERTY)
            .map(MCRConfiguration2::splitValue)
            .orElseGet(Stream::empty)
            .map(MCRConfiguration2::<MCRPostUploadFileProcessor>instantiateClass)
            .collect(Collectors.toList());
    }

    @Override
    public void startUpload(int numFiles) {
        super.startUpload(numFiles);
        this.filesUploaded = 0;
        this.setStatus(MCRProcessableStatus.processing);
        this.setProgress(0);
        this.setProgressText("start upload...");
    }

    private synchronized void prepareUpload() throws MCRPersistenceException, MCRAccessException, IOException {
        if (this.derivate != null) {
            return;
        }
        LOGGER.debug("upload starting, expecting {} files", getNumFiles());

        MCRObjectID derivateID = getOrCreateDerivateID();

        if (MCRMetadataManager.exists(derivateID)) {
            this.derivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);
        } else {
            this.derivate = createDerivate(derivateID);
        }

        getOrCreateRootDirectory();

        LOGGER.debug("uploading into {} of {}", this.derivateID, this.documentID);
    }

    private MCRObjectID getOrCreateDerivateID() {
        if (derivateID == null) {
            String projectID = MCRObjectID.getInstance(this.documentID).getProjectId();
            MCRObjectID oid = MCRObjectID.getNextFreeId(projectID + '_' + ID_TYPE);
            this.derivateID = oid.toString();
            return oid;
        } else {
            return MCRObjectID.getInstance(derivateID);
        }
    }

    private MCRDerivate createDerivate(MCRObjectID derivateID)
        throws MCRPersistenceException, IOException, MCRAccessException {
        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(derivateID);

        String schema = MCRConfiguration2.getString("MCR.Metadata.Config.derivate")
            .orElse("datamodel-derivate.xml")
            .replaceAll(".xml", ".xsd");
        derivate.setSchema(schema);

        MCRMetaLinkID linkId = new MCRMetaLinkID();
        linkId.setSubTag("linkmeta");
        linkId.setReference(documentID, null, null);
        derivate.getDerivate().setLinkMeta(linkId);

        MCRMetaIFS ifs = new MCRMetaIFS();
        ifs.setSubTag("internal");
        ifs.setSourcePath(null);
        derivate.getDerivate().setInternals(ifs);

        LOGGER.debug("Creating new derivate with ID {}", this.derivateID);
        MCRMetadataManager.create(derivate);

        setDefaultPermissions(derivateID);

        return derivate;
    }

    protected void setDefaultPermissions(MCRObjectID derivateID) {
        if (MCRConfiguration2.getBoolean("MCR.Access.AddDerivateDefaultRule").orElse(true)) {
            MCRAccessInterface accessImpl = MCRAccessManager.getAccessImpl();
            Collection<String> configuredPermissions = accessImpl.getAccessPermissionsFromConfiguration();
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
        LOGGER.debug("incoming acceptFile request: {} {} {} bytes", path, checksum, length);
        boolean shouldAcceptFile = true;
        if (rootDir != null) {
            MCRPath child = MCRPath.toMCRPath(rootDir.resolve(path));
            if (Files.isRegularFile(child)) {
                @SuppressWarnings("rawtypes")
                MCRFileAttributes attrs = Files.readAttributes(child, MCRFileAttributes.class);
                shouldAcceptFile = attrs.size() != length
                    || !(checksum.equals(attrs.md5sum()) && child.getFileSystem().verifies(child, attrs));
            }
        }
        LOGGER.debug("Should the client send this file? {}", shouldAcceptFile);
        return shouldAcceptFile;
    }

    @Override
    public synchronized long receiveFile(String path, InputStream in, long length, String checksum)
        throws IOException, MCRPersistenceException, MCRAccessException {
        LOGGER.debug("incoming receiveFile request: {} {} {} bytes", path, checksum, length);

        this.setProgressText(path);

        List<Path> tempFiles = new LinkedList<>();
        Supplier<Path> tempFileSupplier = () -> {
            try {
                Path tempFile = Files.createTempFile(derivateID + "-" + path.hashCode(), ".upload");
                tempFiles.add(tempFile);
                return tempFile;
            } catch (IOException e) {
                throw new UncheckedIOException("Error while creating temp File!", e);
            }
        };

        try (InputStream fIn = preprocessInputStream(path, in, length, tempFileSupplier)) {
            if (rootDir == null) {
                //MCR-1376: Create derivate only if at least one file was successfully uploaded
                prepareUpload();
            }
            MCRPath file = getFile(path);
            LOGGER.info("Creating file {}.", file);
            Files.copy(fIn, file, StandardCopyOption.REPLACE_EXISTING);
            return tempFiles.isEmpty() ? length : Files.size(tempFiles.stream().reduce((a, b) -> b).get());
        } finally {
            tempFiles.stream().filter(Files::exists).forEach((tempFilePath) -> {
                try {
                    Files.delete(tempFilePath);
                } catch (IOException e) {
                    LOGGER.error("Could not delete temp file {}", tempFilePath);
                }
            });
            this.filesUploaded++;
            int progress = (int) (((float) this.filesUploaded / (float) getNumFiles()) * 100f);
            this.setProgress(progress);
        }
    }

    private InputStream preprocessInputStream(String path, InputStream in, long length, Supplier<Path> tempFileSupplier)
        throws IOException {
        List<MCRPostUploadFileProcessor> activeProcessors = FILE_PROCESSORS.stream().filter(p -> p.isProcessable(path))
            .collect(Collectors.toList());
        if (activeProcessors.isEmpty()) {
            return in;
        }
        Path currentTempFile = tempFileSupplier.get();
        try (InputStream initialIS = in) {
            Files.copy(initialIS, currentTempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        long myLength = Files.size(currentTempFile);
        if (length >= 0 && length != myLength) {
            throw new IOException("Length of transmitted data does not match promised length: " + myLength + "!="
                + length);
        }
        for (MCRPostUploadFileProcessor pufp : activeProcessors) {
            currentTempFile = pufp.processFile(path, currentTempFile, tempFileSupplier);
        }
        return Files.newInputStream(currentTempFile);
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
    public synchronized void finishUpload() throws IOException, MCRAccessException {
        if (this.derivate == null) {
            return;
        }
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(rootDir)) {
            if (dirStream.iterator().hasNext()) {
                updateMainFile();
            } else {
                throw new IllegalStateException(
                    "No files were uploaded, delete entry in database for " + derivate.getId() + "!");
            }
        }
        this.setStatus(MCRProcessableStatus.successful);
    }

    private void updateMainFile() throws IOException, MCRAccessException {
        String mainFile = derivate.getDerivate().getInternals().getMainDoc();
        MCRObjectDerivate der = MCRMetadataManager.retrieveMCRDerivate(getOrCreateDerivateID()).getDerivate();
        boolean hasNoMainFile = ((der.getInternals().getMainDoc() == null) || (der.getInternals().getMainDoc().trim()
            .isEmpty()));
        if ((mainFile == null) || mainFile.trim().isEmpty() && hasNoMainFile) {
            mainFile = getPathOfMainFile();
            LOGGER.debug("Setting main file to {}", mainFile);
            derivate.getDerivate().getInternals().setMainDoc(mainFile);
            MCRMetadataManager.update(derivate);
        }
    }

    protected String getPathOfMainFile() throws IOException {
        MainFileFinder mainFileFinder = new MainFileFinder();
        Files.walkFileTree(rootDir, mainFileFinder);
        Path mainFile = mainFileFinder.getMainFile();
        return mainFile == null ? "" : mainFile.subpath(0, mainFile.getNameCount()).toString();
    }

    public String getDerivateID() {
        return derivateID;
    }

    public String getDocumentID() {
        return documentID;
    }

    private static class MainFileFinder extends SimpleFileVisitor<Path> {

        private Path mainFile;

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
