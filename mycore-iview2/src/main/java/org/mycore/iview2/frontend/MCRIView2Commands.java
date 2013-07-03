/*
 * $Id$
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

package org.mycore.iview2.frontend;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.ws.Endpoint;

import org.apache.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;
import org.mycore.imagetiler.MCRImage;
import org.mycore.imagetiler.MCRTiledPictureProps;
import org.mycore.iview2.services.MCRIView2Tools;
import org.mycore.iview2.services.MCRImageTiler;
import org.mycore.iview2.services.MCRTileJob;
import org.mycore.iview2.services.MCRTilingQueue;
import org.mycore.iview2.services.webservice.MCRIView2RemoteFunctions;

/**
 * Provides commands for Image Viewer.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRIView2Commands extends MCRAbstractCommands {
    private static final String CMD_CLASS = MCRIView2Commands.class.getCanonicalName() + ".";

    // check tiles
    private static final MCRCommand CHECK_ALL_IMAGES_COMMAND = new MCRCommand("check tiles of all derivates", CMD_CLASS
            + "checkAll", "checks if all images have valid iview2 files and start tiling if not");

    private static final MCRCommand CHECK_TILES_OF_DERIVATE_COMMAND = new MCRCommand(
            "check tiles of derivate {0}",
            CMD_CLASS + "checkTilesOfDerivate String",
            "checks if all images of derivate {0} with a supported image type as main document have valid iview2 files and start tiling if not ");

    private static final MCRCommand CHECK_TILES_OF_IMAGE_COMMAND = new MCRCommand("check tiles of image {0} {1}",
            CMD_CLASS + "checkImage String String",
            "checks if tiles a specific file identified by its derivate {0} and absolute path {1} are valid or generates new one");

    // tile images
    private static final MCRCommand TILE_ALL_IMAGES_COMMAND = new MCRCommand("tile images of all derivates", CMD_CLASS
            + "tileAll", "tiles all images of all derivates with a supported image type as main document");

    private static final MCRCommand TILE_OBJECT_TILES_COMMAND = new MCRCommand("tile images of object {0}", CMD_CLASS
            + "tileDerivatesOfObject String",
            "tiles all images of derivates of object {0} with a supported image type as main document");

    private static final MCRCommand TILE_DERIVATE_TILES_COMMAND = new MCRCommand("tile images of derivate {0}",
            CMD_CLASS + "tileDerivate String",
            "tiles all images of derivate {0} with a supported image type as main document");

    private static final MCRCommand TILE_IMAGE_COMMAND = new MCRCommand("tile image {0} {1}", CMD_CLASS
            + "tileImage String String", "tiles a specific file identified by its derivate {0} and absolute path {1}");

    // delete tiles
    private static final MCRCommand DEL_ALL_TILES_COMMAND = new MCRCommand("delete all tiles", CMD_CLASS
            + "deleteAllTiles", "removes all tiles of all derivates");

    private static final MCRCommand DEL_OBJECT_TILES_COMMAND = new MCRCommand("delete tiles of object {0}", CMD_CLASS
            + "deleteDerivateTilesOfObject String", "removes tiles of a specific file identified by its object ID {0}");

    private static final MCRCommand DEL_DERIVATE_TILES_COMMAND = new MCRCommand("delete tiles of derivate {0}",
            CMD_CLASS + "deleteDerivateTiles String",
            "removes tiles of a specific file identified by its derivate ID {0}");

    private static final MCRCommand DEL_IMAGE_TILES_COMMAND = new MCRCommand("delete tiles of image {0} {1}", CMD_CLASS
            + "deleteImageTiles String String",
            "removes tiles of a specific file identified by its derivate ID {0} and absolute path {1}");

    // webservices
    private static final MCRCommand START_TILE_WEBSERVICE_COMMAND = new MCRCommand(
            "start tile webservice on {0}",
            CMD_CLASS + "startTileWebService String",
            "start a tile web service on adress {0}, e.g. 'http//localhost:8084/tileService', and stopping any other running service");

    private static final MCRCommand STOP_TILE_WEBSERVICE_COMMAND = new MCRCommand("stop tile webservice", CMD_CLASS
            + "stopTileWebService", "stops the tile web service");

    private static final MCRTilingQueue TILE_QUEUE = MCRTilingQueue.getInstance();

    private static final Logger LOGGER = Logger.getLogger(MCRIView2Commands.class);

    private static Endpoint tileService;

    public MCRIView2Commands() {
        addCommand(CHECK_ALL_IMAGES_COMMAND);
        addCommand(CHECK_TILES_OF_DERIVATE_COMMAND);
        addCommand(CHECK_TILES_OF_IMAGE_COMMAND);
        addCommand(TILE_ALL_IMAGES_COMMAND);
        addCommand(TILE_OBJECT_TILES_COMMAND);
        addCommand(TILE_DERIVATE_TILES_COMMAND);
        addCommand(TILE_IMAGE_COMMAND);
        addCommand(DEL_ALL_TILES_COMMAND);
        addCommand(DEL_OBJECT_TILES_COMMAND);
        addCommand(DEL_DERIVATE_TILES_COMMAND);
        addCommand(DEL_IMAGE_TILES_COMMAND);
        addCommand(START_TILE_WEBSERVICE_COMMAND);
        addCommand(STOP_TILE_WEBSERVICE_COMMAND);
    }

    /**
     * meta command to tile all images of all derivates.
     * @return list of commands to execute.
     */
    public static List<String> tileAll() {
        return forAllDerivates(TILE_DERIVATE_TILES_COMMAND);
    }

    /**
     * meta command to check (and repair) tiles of all images of all derivates.
     * @return list of commands to execute.
     */
    public static List<String> checkAll() {
        return forAllDerivates(CHECK_TILES_OF_DERIVATE_COMMAND);
    }

    private static List<String> forAllDerivates(MCRCommand command) {
        List<String> ids = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
        List<String> cmds = new ArrayList<String>(ids.size());
        for (String id : ids) {
            cmds.add(MessageFormat.format(command.getSyntax(), id));
        }
        return cmds;
    }

    /**
     * meta command to tile all images of derivates of an object .
     * @param objectID a object ID
     * @return list of commands to execute.
     */
    public static List<String> tileDerivatesOfObject(String objectID) {
        return forAllDerivatesOfObject(objectID, TILE_DERIVATE_TILES_COMMAND);
    }

    /**
     * meta command to tile all images of this derivate.
     * @param derivateID a derivate ID
     * @return list of commands to execute.
     */
    public static List<String> tileDerivate(String derivateID) {
        return forAllImages(derivateID, TILE_IMAGE_COMMAND);
    }

    /**
     * meta command to check (and repair) all tiles of all images of this derivate.
     * @param derivateID a derivate ID
     * @return list of commands to execute.
     */
    public static List<String> checkTilesOfDerivate(String derivateID) {
        return forAllImages(derivateID, CHECK_TILES_OF_IMAGE_COMMAND);
    }

    private static List<String> forAllImages(String derivateID, MCRCommand command) {
        if (!MCRIView2Tools.isDerivateSupported(derivateID)) {
            LOGGER.info("Skipping tiling of derivate " + derivateID + " as it's main file is not supported by IView2.");
            return null;
        }
        List<String> returns = new ArrayList<String>();
        MCRDirectory derivate = null;

        MCRFilesystemNode node = MCRFilesystemNode.getRootNode(derivateID);

        if (node == null || !(node instanceof MCRDirectory))
            throw new MCRException("Derivate " + derivateID + " does not exist or is not a directory!");
        derivate = (MCRDirectory) node;

        List<MCRFile> supportedFiles = getSupportedFiles(derivate);
        for (MCRFile image : supportedFiles) {
            returns.add(MessageFormat.format(command.getSyntax(), derivateID, image.getAbsolutePath()));
        }
        return returns;
    }

    /**
     * checks and repairs tile of this {@link MCRFile}
     * @param derivate derivate ID
     * @param absoluteImagePath absolute path to image file
     */
    public static void checkImage(String derivate, String absoluteImagePath) {
        File iviewFile = MCRImage.getTiledFile(MCRIView2Tools.getTileDir(), derivate, absoluteImagePath);
        //file checks
        if (!iviewFile.exists()) {
            LOGGER.warn("IView2 file does not exist: " + iviewFile.getAbsolutePath());
            tileImage(derivate, absoluteImagePath);
            return;
        }
        MCRTiledPictureProps props;
        try {
            props = MCRTiledPictureProps.getInstance(iviewFile);
        } catch (Exception e) {
            LOGGER.warn("Error while reding image metadata. Recreating tiles.", e);
            tileImage(derivate, absoluteImagePath);
            return;
        }
        ZipFile iviewImage;
        try {
            iviewImage = new ZipFile(iviewFile);
            validateZipFile(iviewImage);
        } catch (Exception e) {
            LOGGER.warn("Error while reading Iview2 file: " + iviewFile.getAbsolutePath(), e);
            tileImage(derivate, absoluteImagePath);
            return;
        }
        //structure and metadata checks
        int tilesCount = iviewImage.size() - 1; //one for metadata
        if (props.getTilesCount() != tilesCount) {
            LOGGER.warn("Metadata tile count does not match stored tile count: " + iviewFile.getAbsolutePath());
            tileImage(derivate, absoluteImagePath);
            return;
        }
        int x = props.getWidth();
        int y = props.getHeight();
        if (MCRImage.getTileCount(x, y) != tilesCount) {
            LOGGER.warn("Calculated tile count does not match stored tile count: " + iviewFile.getAbsolutePath());
            tileImage(derivate, absoluteImagePath);
            return;
        }
    }

    private static void validateZipFile(ZipFile iviewImage) throws IOException {
        Enumeration<? extends ZipEntry> entries = iviewImage.entries();
        CRC32 crc = new CRC32();
        byte[] data = new byte[4096];
        int read;
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            InputStream is = iviewImage.getInputStream(entry);
            try {
                while ((read = is.read(data, 0, data.length)) != -1) {
                    crc.update(data, 0, read);
                }
            } finally {
                is.close();
            }
            if (entry.getCrc() != crc.getValue()) {
                throw new IOException("CRC32 does not match for entry: " + entry.getName());
            }
            crc.reset();
        }
    }

    /**
     * Tiles this {@link MCRFile}.
     * @param derivate derivate ID
     * @param absoluteImagePath absolute path to image file
     */
    public static void tileImage(String derivate, String absoluteImagePath) {
        MCRTileJob job = new MCRTileJob();
        job.setDerivate(derivate);
        job.setPath(absoluteImagePath);
        TILE_QUEUE.offer(job);
        startMasterTilingThread();
    }

    /**
     * Tiles this {@link MCRFile}
     * @param file
     */
    public static void tileImage(MCRFile file) {
        if (MCRIView2Tools.isFileSupported(file)) {
            MCRTileJob job = new MCRTileJob();
            job.setDerivate(file.getOwnerID());
            job.setPath(file.getAbsolutePath());
            TILE_QUEUE.offer(job);
            LOGGER.info("Added to TilingQueue: " + file.getID() + " " + file.getAbsolutePath());
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
    public static void deleteAllTiles() {
        File storeDir = MCRIView2Tools.getTileDir();
        for (File sub : storeDir.listFiles()) {
            if (sub.isFile())
                sub.delete();
            else
                deleteDirectory(sub);
        }
        TILE_QUEUE.clear();
    }

    /**
     * Deletes all image tiles of derivates of this object.
     * @param objectID a object ID
     */
    public static List<String> deleteDerivateTilesOfObject(String objectID) {
        return forAllDerivatesOfObject(objectID, DEL_DERIVATE_TILES_COMMAND);
    }

    private static List<String> forAllDerivatesOfObject(String objectID, MCRCommand batchCommand) {
        MCRObjectID mcrobjid;
        try {
            mcrobjid = MCRObjectID.getInstance(objectID);
        } catch (Exception e) {
            LOGGER.error("The object ID " + objectID + " is wrong");
            return null;
        }
        List<MCRObjectID> derivateIds = MCRMetadataManager.getDerivateIds(mcrobjid, 0, TimeUnit.MILLISECONDS);
        if (derivateIds == null) {
            LOGGER.error("Object does not exist: " + mcrobjid);
        }
        ArrayList<String> cmds = new ArrayList<>(derivateIds.size());
        for (MCRObjectID derId : derivateIds) {
            cmds.add(MessageFormat.format(batchCommand.getSyntax(), derId));
        }
        return cmds;
    }

    /**
     * Deletes all image tiles of this derivate.
     * @param derivateID a derivate ID
     */
    public static void deleteDerivateTiles(String derivateID) {
        File derivateDir = MCRImage.getTiledFile(MCRIView2Tools.getTileDir(), derivateID, null);
        deleteDirectory(derivateDir);
        TILE_QUEUE.remove(derivateID);
    }

    /**
     * Deletes all image tiles of this {@link MCRFile}
     * @param derivate derivate ID
     * @param absoluteImagePath absolute path to image file
     */
    public static void deleteImageTiles(String derivate, String absoluteImagePath) {
        File tileFile = MCRImage.getTiledFile(MCRIView2Tools.getTileDir(), derivate, absoluteImagePath);
        deleteFileAndEmptyDirectories(tileFile);
        int removed = TILE_QUEUE.remove(derivate, absoluteImagePath);
        LOGGER.info("removed tiles from " + removed + " images");
    }

    private static void deleteFileAndEmptyDirectories(File file) {
        File parent = file.getParentFile();
        if (file.exists())
            file.delete();
        if (parent != null && parent.isDirectory() && parent.list().length == 0)
            deleteFileAndEmptyDirectories(parent);
    }

    private static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return (path.delete());
    }

    private static List<MCRFile> getSupportedFiles(MCRDirectory rootNode) {
        ArrayList<MCRFile> files = new ArrayList<MCRFile>();
        MCRFilesystemNode[] nodes = rootNode.getChildren();
        for (MCRFilesystemNode node : nodes) {
            if (node instanceof MCRDirectory) {
                MCRDirectory dir = (MCRDirectory) node;
                files.addAll(getSupportedFiles(dir));
            } else {
                MCRFile file = (MCRFile) node;
                if (MCRIView2Tools.isFileSupported(file)) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    /**
     * Starts tile webservice ({@link MCRIView2RemoteFunctions}) on this address.
     * @param address a URI to bind web service to
     */
    public static void startTileWebService(String address) {
        stopTileWebService();
        tileService = Endpoint.publish(address, new MCRIView2RemoteFunctions());
    }

    /**
     * Stops web service started by {@link #startTileWebService(String)}.
     */
    public static void stopTileWebService() {
        if (tileService == null || !tileService.isPublished()) {
            LOGGER.info("Currently there is no tiling service running");
            return;
        }
        LOGGER.info("Closing web service.");
        tileService.stop();
    }

}
