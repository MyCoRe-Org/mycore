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

package org.mycore.impex;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.packaging.MCRPacker;

/**
 * Using the {@link MCRPacker} API to build a {@link MCRTransferPackage}.
 * 
 * @author Matthias Eichner
 * @author Silvio Hermann
 */
public class MCRTransferPackagePacker extends MCRPacker {

    private static final Logger LOGGER = LogManager.getLogger(MCRTransferPackagePacker.class);

    private static Path SAVE_DIRECTORY_PATH;

    static {
        String alternative = MCRConfiguration.instance().getString("MCR.datadir") + File.separator + "transferPackages";
        String directoryPath = MCRConfiguration.instance().getString("MCR.TransferPackage.Save.to.Directory",
            alternative);
        SAVE_DIRECTORY_PATH = Paths.get(directoryPath);
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.services.packaging.MCRPacker#checkSetup()
     */
    @Override
    public void checkSetup() throws MCRUsageException, MCRAccessException {
        build();
    }

    /**
     * Builds the transfer package and returns it.
     * 
     * @return the transfer package
     * @throws MCRAccessException
     */
    private MCRTransferPackage build() throws MCRAccessException {
        MCRObject source = getSource();
        MCRTransferPackage transferPackage = new MCRTransferPackage(source);
        transferPackage.build();
        return transferPackage;
    }

    /**
     * Returns the source object.
     * 
     * @return mycore object which should be packed
     * @throws MCRUsageException something went wrong
     */
    protected MCRObject getSource() throws MCRUsageException {
        String sourceId = getSourceId();
        MCRObjectID mcrId = MCRObjectID.getInstance(sourceId);
        if (!MCRMetadataManager.exists(mcrId)) {
            throw new MCRUsageException(
                "Requested object '" + sourceId + "' does not exist. Thus a transfer package cannot be created.");
        }
        return MCRMetadataManager.retrieveMCRObject(mcrId);
    }

    /**
     * Returns the id of the mycore object which should be packed.
     * 
     * @return mycore object id as string
     */
    private String getSourceId() {
        String sourceId = getParameters().get("source");
        if (sourceId == null) {
            throw new MCRUsageException("One does not simply provide 'null' as source.");
        }
        return sourceId;
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.services.packaging.MCRPacker#pack()
     */
    @Override
    public void pack() throws ExecutionException {
        String sourceId = getSourceId();
        try {
            LOGGER.info("Creating transfer package for {} at {}", sourceId, SAVE_DIRECTORY_PATH.toAbsolutePath());
            MCRTransferPackage transferPackage = build();
            checkAndCreateSaveDirectory();
            buildTar(getTarPath(transferPackage), transferPackage);
            LOGGER.info("Transfer package for {} created.", sourceId);
        } catch (Exception exc) {
            throw new ExecutionException("Unable to pack() transfer package for source " + sourceId, exc);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.services.packaging.MCRPacker#rollback()
     */
    @Override
    public void rollback() {

    }

    /**
     * Returns the path to the tar archive where the transfer package will be stored.
     * 
     * @return path to the *.tar location
     */
    public Path getTarPath(MCRTransferPackage transferPackage) {
        return SAVE_DIRECTORY_PATH.resolve(transferPackage.getSource().getId() + ".tar");
    }

    /**
     * Builds a *.tar archive at the path for the given transfer package.
     * 
     * @param pathToTar where to store the *.tar
     * @param transferPackage the package to zip
     * @throws IOException something went wrong while packing
     */
    private void buildTar(Path pathToTar, MCRTransferPackage transferPackage) throws IOException {
        FileOutputStream fos = new FileOutputStream(pathToTar.toFile());
        try (TarArchiveOutputStream tarOutStream = new TarArchiveOutputStream(fos)) {
            for (Entry<String, MCRContent> contentEntry : transferPackage.getContent().entrySet()) {
                String filePath = contentEntry.getKey();
                byte[] data = contentEntry.getValue().asByteArray();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Adding '{}' to {}", filePath, pathToTar.toAbsolutePath());
                }
                writeFile(tarOutStream, filePath, data);
                writeMD5(tarOutStream, filePath + ".md5", data);
            }
        }
    }

    /**
     * Writes a file to a *.tar archive.
     * 
     * @param tarOutStream the stream of the *.tar.
     * @param fileName the file name to write
     * @param data of the file
     * 
     * @throws IOException some writing to the stream went wrong
     */
    private void writeFile(TarArchiveOutputStream tarOutStream, String fileName, byte[] data) throws IOException {
        TarArchiveEntry tarEntry = new TarArchiveEntry(fileName);
        tarEntry.setSize(data.length);
        tarOutStream.putArchiveEntry(tarEntry);
        tarOutStream.write(data);
        tarOutStream.closeArchiveEntry();
    }

    /**
     * Writes a checksum file to a *.tar archive.
     * 
     * @param tarOutStream the stream of the *.tar.
     * @param fileName the file name to write
     * @param data of the file
     * 
     * @throws IOException some writing to the stream went wrong
     */
    private void writeMD5(TarArchiveOutputStream tarOutStream, String fileName, byte[] data) throws IOException {
        String md5 = MCRUtils.getMD5Sum(new ByteArrayInputStream(data));
        byte[] md5Bytes = md5.getBytes(StandardCharsets.ISO_8859_1);
        writeFile(tarOutStream, fileName, md5Bytes);
    }

    private void checkAndCreateSaveDirectory() throws IOException {
        if (!Files.exists(SAVE_DIRECTORY_PATH)) {
            LOGGER.info("Creating directory {}", SAVE_DIRECTORY_PATH.toAbsolutePath());
            Files.createDirectories(SAVE_DIRECTORY_PATH);
        }
    }

}
