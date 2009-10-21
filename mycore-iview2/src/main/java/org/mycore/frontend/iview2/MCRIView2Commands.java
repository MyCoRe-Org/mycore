/*
 * $Id$
 * $Revision: 5697 $ $Date: 20.10.2009 $
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

package org.mycore.frontend.iview2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRXMLTableManager;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;
import org.mycore.services.iview2.MCRIView2Tools;
import org.mycore.services.iview2.MCRImageTiling;
import org.mycore.services.iview2.MCRIview2Props;
import org.mycore.services.iview2.MCRTileJob;
import org.mycore.services.iview2.MCRTilingQueue;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRIView2Commands extends MCRAbstractCommands {
    private static final String CMD_CLASS = MCRIView2Commands.class.getCanonicalName() + ".";

    private static final MCRTilingQueue TILE_QUEUE = MCRTilingQueue.getInstance();

    private static final Logger LOGGER = Logger.getLogger(MCRIView2Commands.class);

    public MCRIView2Commands() {
        command.add(new MCRCommand("tile images of all derivates", CMD_CLASS + "tileAll",
                "tiles all images of all derivates with a supported image type as main document"));
        command.add(new MCRCommand("tile images of derivate {0}", CMD_CLASS + "tileDerivate String",
                "tiles all images of derivate {0} with a supported image type as main document"));
        command.add(new MCRCommand("tile image {0} {1}", CMD_CLASS + "tileImage String String",
                "tiles a specific file identified by its derivate {0} and absolute path {1}"));
        command.add(new MCRCommand("delete all tiles", CMD_CLASS + "deleteAllTiles", "removes all tiles of all derivates"));
        command.add(new MCRCommand("delete tiles of derivate {0}", CMD_CLASS + "deleteDerivateTiles String",
                "removes tiles of a specific file identified by its absolute path {0}"));
        command.add(new MCRCommand("delete tiles of image {0} {1}", CMD_CLASS + "deleteImageTiles String String",
                "removes tiles of a specific file identified by its derivate {0} and absolute path {1}"));
    }

    public static List<String> tileAll() {
        List<String> ids = MCRXMLTableManager.instance().listIDsOfType("derivate");
        List<String> cmds = new ArrayList<String>(ids.size());
        for (String id : ids) {
            cmds.add("tile images of derivate " + id);
        }
        return cmds;
    }

    public static List<String> tileDerivate(String derivateID) {
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
        String baseCmd = "tile image " + derivateID + " ";
        for (MCRFile image : supportedFiles) {
            returns.add(baseCmd + image.getAbsolutePath());
        }
        return returns;
    }

    public static void tileImage(String derivate, String absoluteImagePath) {
        MCRTileJob job = new MCRTileJob();
        job.setDerivate(derivate);
        job.setPath(absoluteImagePath);
        TILE_QUEUE.offer(job);
        startMasterTilingThread();
    }

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
        if (!MCRImageTiling.isRunning()) {
            LOGGER.info("Starting Tiling thread.");
            final Thread tiling = new Thread(MCRImageTiling.getInstance());
            tiling.start();
        }
    }

    public static void deleteAllTiles() {
        File storeDir = new File(MCRIview2Props.getProperty("DirectoryForTiles"));
        for (File sub : storeDir.listFiles()) {
            if (sub.isFile())
                sub.delete();
            else
                deleteDirectory(sub);
        }
        TILE_QUEUE.clear();
    }

    public static void deleteDerivateTiles(String derivateID) {
        //TODO: delete tile directory
        TILE_QUEUE.remove(derivateID);
    }

    public static void deleteImageTiles(String derivate, String absoluteImagePath) {
        //TODO: delete tile
        int removed = TILE_QUEUE.remove(derivate, absoluteImagePath);
        LOGGER.info("removed tiles from " + removed + " images");
    }

    private static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
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

}
