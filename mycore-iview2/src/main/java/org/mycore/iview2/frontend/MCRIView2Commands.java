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

package org.mycore.iview2.frontend;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageReader;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.utils.MCRRecursiveDeleter;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.imagetiler.MCRImage;
import org.mycore.imagetiler.MCRTiledPictureProps;
import org.mycore.iview2.services.MCRIView2Tools;
import org.mycore.iview2.services.MCRImageTiler;
import org.mycore.iview2.services.MCRTileJob;
import org.mycore.iview2.services.MCRTilingQueue;

/**
 * Provides commands for Image Viewer.
 * @author Thomas Scheffler (yagee)
 *
 */

@MCRCommandGroup(name = "IView2 Tile Commands")
public class MCRIView2Commands extends MCRAbstractCommands {
    private static final Logger LOGGER = LogManager.getLogger(MCRIView2Commands.class);

    /**
     * meta command to tile all images of all derivates.
     * @return list of commands to execute.
     */
    // tile images
    @MCRCommand(syntax = "tile images of all derivates",
        help = "tiles all images of all derivates with a supported image type as main document",
        order = 40)
    public static List<String> tileAll() {
        return forAllDerivates(TILE_DERIVATE_TILES_COMMAND_SYNTAX);
    }

    /**
     * meta command to check (and repair) tiles of all images of all derivates.
     * @return list of commands to execute.
     */
    @MCRCommand(syntax = "check tiles of all derivates",
        help = "checks if all images have valid iview2 files and start tiling if not",
        order = 10)
    public static List<String> checkAll() {
        return forAllDerivates(CHECK_TILES_OF_DERIVATE_COMMAND_SYNTAX);
    }

    private static List<String> forAllDerivates(String batchCommandSyntax) {
        List<String> ids = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
        List<String> cmds = new ArrayList<>(ids.size());
        for (String id : ids) {
            cmds.add(MessageFormat.format(batchCommandSyntax, id));
        }
        return cmds;
    }

    /**
     * meta command to tile all images of derivates of a project.
     * @return list of commands to execute.
     */
    @MCRCommand(syntax = "tile images of derivates of project {0}",
        help = "tiles all images of derivates of a project with a supported image type as main document",
        order = 41)
    public static List<String> tileAllOfProject(String project) {
        return forAllDerivatesOfProject(TILE_DERIVATE_TILES_COMMAND_SYNTAX, project);
    }

    /**
     * meta command to check (and repair) tiles of all images of derivates of a project.
     * @return list of commands to execute.
     */
    @MCRCommand(syntax = "check tiles of derivates of project {0}",
        help = "checks if all images have valid iview2 files and start tiling if not",
        order = 11)
    public static List<String> checkAllOfProject(String project) {
        return forAllDerivatesOfProject(CHECK_TILES_OF_DERIVATE_COMMAND_SYNTAX, project);
    }

    private static List<String> forAllDerivatesOfProject(String batchCommandSyntax, String project) {
        List<String> ids = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
        List<String> cmds = new ArrayList<>(ids.size());
        for (String id : ids) {
            if (id.startsWith(project)) {
                cmds.add(MessageFormat.format(batchCommandSyntax, id));
            }
        }
        return cmds;
    }

    /**
     * meta command to tile all images of derivates of an object .
     * @param objectID a object ID
     * @return list of commands to execute.
     */
    @MCRCommand(syntax = "tile images of object {0}",
        help = "tiles all images of derivates of object {0} with a supported image type as main document",
        order = 50)
    public static List<String> tileDerivatesOfObject(String objectID) {
        return forAllDerivatesOfObject(objectID, TILE_DERIVATE_TILES_COMMAND_SYNTAX);
    }

    private static final String TILE_DERIVATE_TILES_COMMAND_SYNTAX = "tile images of derivate {0}";

    /**
     * meta command to tile all images of this derivate.
     * @param derivateID a derivate ID
     * @return list of commands to execute.
     */
    @MCRCommand(syntax = TILE_DERIVATE_TILES_COMMAND_SYNTAX,
        help = "tiles all images of derivate {0} with a supported image type as main document",
        order = 60)
    public static List<String> tileDerivate(String derivateID) throws IOException {
        return forAllImages(derivateID, TILE_IMAGE_COMMAND_SYNTAX);
    }

    private static final String CHECK_TILES_OF_DERIVATE_COMMAND_SYNTAX = "check tiles of derivate {0}";

    /**
     * meta command to check (and repair) all tiles of all images of this derivate.
     * @param derivateID a derivate ID
     * @return list of commands to execute.
     */
    @MCRCommand(syntax = CHECK_TILES_OF_DERIVATE_COMMAND_SYNTAX,
        help = "checks if all images of derivate {0} with a supported image type as main document have valid iview2 files and start tiling if not ",
        order = 20)
    public static List<String> checkTilesOfDerivate(String derivateID) throws IOException {
        return forAllImages(derivateID, CHECK_TILES_OF_IMAGE_COMMAND_SYNTAX);
    }

    private static List<String> forAllImages(String derivateID, String batchCommandSyntax) throws IOException {
        if (!MCRIView2Tools.isDerivateSupported(derivateID)) {
            LOGGER.info("Skipping tiling of derivate {} as it's main file is not supported by IView2.", derivateID);
            return null;
        }
        MCRPath derivateRoot = MCRPath.getPath(derivateID, "/");

        if (!Files.exists(derivateRoot)) {
            throw new MCRException("Derivate " + derivateID + " does not exist or is not a directory!");
        }

        List<MCRPath> supportedFiles = getSupportedFiles(derivateRoot);
        return supportedFiles.stream()
            .map(image -> MessageFormat.format(batchCommandSyntax, derivateID,
                image.getOwnerRelativePath()))
            .collect(Collectors.toList());
    }

    private static final String CHECK_TILES_OF_IMAGE_COMMAND_SYNTAX = "check tiles of image {0} {1}";

    @MCRCommand(syntax = "fix dead tile jobs", help = "Deletes entries for files which dont exist anymore!")
    public static void fixDeadEntries() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<MCRTileJob> allTileJobQuery = em.createNamedQuery("MCRTileJob.all", MCRTileJob.class);
        List<MCRTileJob> tiles = allTileJobQuery.getResultList();
        tiles.stream()
            .filter(tj -> {
                MCRPath path = MCRPath.getPath(tj.getDerivate(), tj.getPath());
                return !Files.exists(path);
            })
            .peek(tj -> LOGGER.info("Delete TileJob {}:{}", tj.getDerivate(), tj.getPath()))
            .forEach(em::remove);
    }

    /**
     * checks and repairs tile of this derivate.
     * @param derivate derivate ID
     * @param absoluteImagePath absolute path to image file
     */
    @MCRCommand(syntax = CHECK_TILES_OF_IMAGE_COMMAND_SYNTAX,
        help = "checks if tiles a specific file identified by its derivate {0} and absolute path {1} are valid or generates new one",
        order = 30)
    public static void checkImage(String derivate, String absoluteImagePath) throws IOException {
        Path iviewFile = MCRImage.getTiledFile(MCRIView2Tools.getTileDir(), derivate, absoluteImagePath);
        //file checks
        if (!Files.exists(iviewFile)) {
            LOGGER.warn("IView2 file does not exist: {}", iviewFile);
            tileImage(derivate, absoluteImagePath);
            return;
        }
        MCRTiledPictureProps props;
        try {
            props = MCRTiledPictureProps.getInstanceFromFile(iviewFile);
        } catch (Exception e) {
            LOGGER.warn("Error while reading image metadata. Recreating tiles.", e);
            tileImage(derivate, absoluteImagePath);
            return;
        }
        if (props == null) {
            LOGGER.warn("Could not get tile metadata");
            tileImage(derivate, absoluteImagePath);
            return;
        }
        ZipFile iviewImage;
        try {
            iviewImage = new ZipFile(iviewFile.toFile());
            validateZipFile(iviewImage);
        } catch (Exception e) {
            LOGGER.warn("Error while reading Iview2 file: {}", iviewFile, e);
            tileImage(derivate, absoluteImagePath);
            return;
        }
        try (FileSystem fs = MCRIView2Tools.getFileSystem(iviewFile)) {
            Path iviewFileRoot = fs.getRootDirectories().iterator().next();
            //structure and metadata checks
            int tilesCount = iviewImage.size() - 1; //one for metadata
            if (props.getTilesCount() != tilesCount) {
                LOGGER.warn("Metadata tile count does not match stored tile count: {}", iviewFile);
                tileImage(derivate, absoluteImagePath);
                return;
            }
            int x = props.getWidth();
            int y = props.getHeight();
            if (MCRImage.getTileCount(x, y) != tilesCount) {
                LOGGER.warn("Calculated tile count does not match stored tile count: {}", iviewFile);
                tileImage(derivate, absoluteImagePath);
                return;
            }
            try {
                ImageReader imageReader = MCRIView2Tools.getTileImageReader();
                @SuppressWarnings("unused")
                BufferedImage thumbnail = MCRIView2Tools.getZoomLevel(iviewFileRoot, props, imageReader, 0);
                int maxX = (int) Math.ceil((double) props.getWidth() / MCRImage.getTileSize());
                int maxY = (int) Math.ceil((double) props.getHeight() / MCRImage.getTileSize());
                LOGGER.debug(MessageFormat.format("Image size:{0}x{1}, tiles:{2}x{3}", props.getWidth(),
                    props.getHeight(), maxX, maxY));
                try {
                    @SuppressWarnings("unused")
                    BufferedImage sampleTile = MCRIView2Tools.readTile(iviewFileRoot, imageReader,
                        props.getZoomlevel(), maxX - 1, 0);
                } finally {
                    imageReader.dispose();
                }
            } catch (IOException | JDOMException e) {
                LOGGER.warn("Could not read thumbnail of {}", iviewFile, e);
                tileImage(derivate, absoluteImagePath);
            }
        }
    }

    private static void validateZipFile(ZipFile iviewImage) throws IOException {
        Enumeration<? extends ZipEntry> entries = iviewImage.entries();
        CRC32 crc = new CRC32();
        byte[] data = new byte[4096];
        int read;
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            try (InputStream is = iviewImage.getInputStream(entry)) {
                while ((read = is.read(data, 0, data.length)) != -1) {
                    crc.update(data, 0, read);
                }
            }
            if (entry.getCrc() != crc.getValue()) {
                throw new IOException("CRC32 does not match for entry: " + entry.getName());
            }
            crc.reset();
        }
    }

    private static final String TILE_IMAGE_COMMAND_SYNTAX = "tile image {0} {1}";

    /**
     * Tiles this image.
     * @param derivate derivate ID
     * @param absoluteImagePath absolute path to image file
     */
    @MCRCommand(syntax = TILE_IMAGE_COMMAND_SYNTAX,
        help = "tiles a specific file identified by its derivate {0} and absolute path {1}",
        order = 70)
    public static void tileImage(String derivate, String absoluteImagePath) {
        MCRTileJob job = new MCRTileJob();
        job.setDerivate(derivate);
        job.setPath(absoluteImagePath);
        MCRTilingQueue.getInstance().offer(job);
        startMasterTilingThread();
    }

    /**
     * Tiles this {@link MCRPath}
     */
    public static void tileImage(MCRPath file) throws IOException {
        if (MCRIView2Tools.isFileSupported(file)) {
            MCRTileJob job = new MCRTileJob();
            job.setDerivate(file.getOwner());
            job.setPath(file.getOwnerRelativePath());
            MCRTilingQueue.getInstance().offer(job);
            LOGGER.info("Added to TilingQueue: {}", file);
            startMasterTilingThread();
        }
    }

    private static void startMasterTilingThread() {
        if (!MCRImageTiler.isRunning()) {
            LOGGER.info("Starting Tiling thread.");
            final Thread tiling = new Thread(MCRImageTiler.getInstance());
            tiling.start();
        }
    }

    /**
     * Deletes all image tiles.
     */
    @MCRCommand(syntax = "delete all tiles", help = "removes all tiles of all derivates", order = 80)
    public static void deleteAllTiles() throws IOException {
        Path storeDir = MCRIView2Tools.getTileDir();
        Files.walkFileTree(storeDir, MCRRecursiveDeleter.instance());
        MCRTilingQueue.getInstance().clear();
    }

    /**
     * Deletes all image tiles of derivates of this object.
     * @param objectID a object ID
     */
    @MCRCommand(syntax = "delete tiles of object {0}",
        help = "removes tiles of a specific file identified by its object ID {0}",
        order = 90)
    public static List<String> deleteDerivateTilesOfObject(String objectID) {
        return forAllDerivatesOfObject(objectID, DEL_DERIVATE_TILES_COMMAND_SYNTAX);
    }

    private static List<String> forAllDerivatesOfObject(String objectID, String batchCommandSyntax) {
        MCRObjectID mcrobjid;
        try {
            mcrobjid = MCRObjectID.getInstance(objectID);
        } catch (Exception e) {
            LOGGER.error("The object ID {} is wrong", objectID);
            return null;
        }
        List<MCRObjectID> derivateIds = MCRMetadataManager.getDerivateIds(mcrobjid, 0, TimeUnit.MILLISECONDS);
        if (derivateIds == null) {
            LOGGER.error("Object does not exist: {}", mcrobjid);
        }
        ArrayList<String> cmds = new ArrayList<>(derivateIds.size());
        for (MCRObjectID derId : derivateIds) {
            cmds.add(MessageFormat.format(batchCommandSyntax, derId));
        }
        return cmds;
    }

    private static final String DEL_DERIVATE_TILES_COMMAND_SYNTAX = "delete tiles of derivate {0}";

    /**
     * Deletes all image tiles of this derivate.
     * @param derivateID a derivate ID
     */
    @MCRCommand(syntax = DEL_DERIVATE_TILES_COMMAND_SYNTAX,
        help = "removes tiles of a specific file identified by its derivate ID {0}",
        order = 100)
    public static void deleteDerivateTiles(String derivateID) throws IOException {
        Path derivateDir = MCRImage.getTiledFile(MCRIView2Tools.getTileDir(), derivateID, null);
        Files.walkFileTree(derivateDir, MCRRecursiveDeleter.instance());
        MCRTilingQueue.getInstance().remove(derivateID);
    }

    /**
     * Deletes all image tiles of this derivate.
     * @param derivate derivate ID
     * @param absoluteImagePath absolute path to image file
     */
    @MCRCommand(syntax = "delete tiles of image {0} {1}",
        help = "removes tiles of a specific file identified by its derivate ID {0} and absolute path {1}",
        order = 110)
    public static void deleteImageTiles(String derivate, String absoluteImagePath) throws IOException {
        Path tileFile = MCRImage.getTiledFile(MCRIView2Tools.getTileDir(), derivate, absoluteImagePath);
        deleteFileAndEmptyDirectories(tileFile);
        int removed = MCRTilingQueue.getInstance().remove(derivate, absoluteImagePath);
        LOGGER.info("removed tiles from {} images", removed);
    }

    private static void deleteFileAndEmptyDirectories(Path file) throws IOException {
        if (Files.isRegularFile(file)) {
            Files.delete(file);
        }
        if (Files.isDirectory(file)) {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(file)) {
                for (@SuppressWarnings("unused")
                Path entry : directoryStream) {
                    return;
                }
                Files.delete(file);
            }
        }
        Path parent = file.getParent();
        if (parent != null && parent.getNameCount() > 0) {
            deleteFileAndEmptyDirectories(parent);
        }
    }

    private static List<MCRPath> getSupportedFiles(MCRPath rootNode) throws IOException {
        final ArrayList<MCRPath> files = new ArrayList<>();
        SimpleFileVisitor<Path> test = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Objects.requireNonNull(file);
                Objects.requireNonNull(attrs);
                if (MCRIView2Tools.isFileSupported(file)) {
                    files.add(MCRPath.toMCRPath(file));
                }
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(rootNode, test);
        return files;
    }

}
