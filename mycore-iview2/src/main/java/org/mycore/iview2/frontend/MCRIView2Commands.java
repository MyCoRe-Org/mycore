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

import java.awt.image.BufferedImage;
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

import javax.imageio.ImageReader;
import javax.xml.ws.Endpoint;

import org.apache.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
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

@MCRCommandGroup(name="MCR IView2 Tile Commands")
public class MCRIView2Commands extends MCRAbstractCommands {
    private static final MCRTilingQueue TILE_QUEUE = MCRTilingQueue.getInstance();

    private static final Logger LOGGER = Logger.getLogger(MCRIView2Commands.class);

    private static Endpoint tileService;

    /**
     * meta command to tile all images of all derivates.
     * @return list of commands to execute.
     */
    // tile images
    @MCRCommand(syntax="tile images of all derivates", help="tiles all images of all derivates with a supported image type as main document", order=40)
    public static List<String> tileAll() {
        return forAllDerivates(TILE_DERIVATE_TILES_COMMAND_SYNTAX);
    }

    /**
     * meta command to check (and repair) tiles of all images of all derivates.
     * @return list of commands to execute.
     */
    @MCRCommand(syntax="check tiles of all derivates", help="checks if all images have valid iview2 files and start tiling if not", order=10)
    public static List<String> checkAll() {
        return forAllDerivates(CHECK_TILES_OF_DERIVATE_COMMAND_SYNTAX);
    }

    private static List<String> forAllDerivates(String batchCommandSyntax) {
        List<String> ids = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
        List<String> cmds = new ArrayList<String>(ids.size());
        for (String id : ids) {
            cmds.add(MessageFormat.format(batchCommandSyntax, id));
        }
        return cmds;
    }

    /**
     * meta command to tile all images of derivates of an object .
     * @param objectID a object ID
     * @return list of commands to execute.
     */
    @MCRCommand(syntax="tile images of object {0}", help="tiles all images of derivates of object {0} with a supported image type as main document", order=50)
    public static List<String> tileDerivatesOfObject(String objectID) {
        return forAllDerivatesOfObject(objectID, TILE_DERIVATE_TILES_COMMAND_SYNTAX);
    }

    private static final String TILE_DERIVATE_TILES_COMMAND_SYNTAX = "tile images of derivate {0}";
    /**
     * meta command to tile all images of this derivate.
     * @param derivateID a derivate ID
     * @return list of commands to execute.
     */
    @MCRCommand(syntax=TILE_DERIVATE_TILES_COMMAND_SYNTAX, help="tiles all images of derivate {0} with a supported image type as main document", order=60)
    public static List<String> tileDerivate(String derivateID) {
        return forAllImages(derivateID, TILE_IMAGE_COMMAND_SYNTAX);
    }

    private static final String  CHECK_TILES_OF_DERIVATE_COMMAND_SYNTAX = "check tiles of derivate {0}";
    /**
     * meta command to check (and repair) all tiles of all images of this derivate.
     * @param derivateID a derivate ID
     * @return list of commands to execute.
     */
    @MCRCommand(syntax=CHECK_TILES_OF_DERIVATE_COMMAND_SYNTAX, help="checks if all images of derivate {0} with a supported image type as main document have valid iview2 files and start tiling if not ", order=20)

    public static List<String> checkTilesOfDerivate(String derivateID) {
        return forAllImages(derivateID, CHECK_TILES_OF_IMAGE_COMMAND_SYNTAX);
    }

    private static List<String> forAllImages(String derivateID, String batchCommandSyntax) {
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
            returns.add(MessageFormat.format(batchCommandSyntax, derivateID, image.getAbsolutePath()));
        }
        return returns;
    }

    
    private static final String CHECK_TILES_OF_IMAGE_COMMAND_SYNTAX = "check tiles of image {0} {1}";
    
    /**
     * checks and repairs tile of this {@link MCRFile}
     * @param derivate derivate ID
     * @param absoluteImagePath absolute path to image file
     */
    @MCRCommand(syntax=CHECK_TILES_OF_IMAGE_COMMAND_SYNTAX, help="checks if tiles a specific file identified by its derivate {0} and absolute path {1} are valid or generates new one", order=30)
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
        try {
            @SuppressWarnings("unused")
            BufferedImage thumbnail = MCRIView2Tools.getZoomLevel(iviewFile, 0);
            int maxX = (int) Math.ceil((double) props.getWidth() / MCRImage.getTileSize());
            int maxY = (int) Math.ceil((double) props.getHeight() / MCRImage.getTileSize());
            LOGGER.debug(MessageFormat.format("Image size:{0}x{1}, tiles:{2}x{3}", props.getWidth(), props.getHeight(),
                maxX, maxY));
            ImageReader imageReader = MCRIView2Tools.getTileImageReader();
            try {
                @SuppressWarnings("unused")
                BufferedImage sampleTile = MCRIView2Tools.readTile(iviewImage, imageReader, props.getZoomlevel(),
                    maxX - 1, 0);
            } finally {
                imageReader.dispose();
            }
        } catch (IOException | JDOMException e) {
            LOGGER.warn("Could not read thumbnail of " + iviewFile.getAbsolutePath(), e);
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

    private static final String TILE_IMAGE_COMMAND_SYNTAX = "tile image {0} {1}";
    /**
     * Tiles this {@link MCRFile}.
     * @param derivate derivate ID
     * @param absoluteImagePath absolute path to image file
     */
    @MCRCommand(syntax=TILE_IMAGE_COMMAND_SYNTAX, help="tiles a specific file identified by its derivate {0} and absolute path {1}", order=70)
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
    @MCRCommand(syntax="delete all tiles", help="removes all tiles of all derivates", order=80)
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
    @MCRCommand(syntax="delete tiles of object {0}", help="removes tiles of a specific file identified by its object ID {0}", order=90)
    public static List<String> deleteDerivateTilesOfObject(String objectID) {
        return forAllDerivatesOfObject(objectID, DEL_DERIVATE_TILES_COMMAND_SYNTAX);
    }

    private static List<String> forAllDerivatesOfObject(String objectID, String batchCommandSyntax) {
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
            cmds.add(MessageFormat.format(batchCommandSyntax, derId));
        }
        return cmds;
    }

    private static final String DEL_DERIVATE_TILES_COMMAND_SYNTAX = "delete tiles of derivate {0}";
    /**
     * Deletes all image tiles of this derivate.
     * @param derivateID a derivate ID
     */
    @MCRCommand(syntax=DEL_DERIVATE_TILES_COMMAND_SYNTAX, help="removes tiles of a specific file identified by its derivate ID {0}", order=100)
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
    @MCRCommand(syntax="delete tiles of image {0} {1}", help="removes tiles of a specific file identified by its derivate ID {0} and absolute path {1}", order=110)
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
    @MCRCommand(syntax="start tile webservice on {0}", help="start a tile web service on address {0}, e.g. 'http//localhost:8084/tileService', and stopping any other running service", order=120)

    public static void startTileWebService(String address) {
        stopTileWebService();
        tileService = Endpoint.publish(address, new MCRIView2RemoteFunctions());
    }

    /**
     * Stops web service started by {@link #startTileWebService(String)}.
     */
    @MCRCommand(syntax="stop tile webservice", help="stops the tile web service", order=130)
    public static void stopTileWebService() {
        if (tileService == null || !tileService.isPublished()) {
            LOGGER.info("Currently there is no tiling service running");
            return;
        }
        LOGGER.info("Closing web service.");
        tileService.stop();
    }

}
