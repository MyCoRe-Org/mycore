/*
 * $Id$
 * $Revision: 5697 $ $Date: 19.10.2009 $
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

package org.mycore.iview2.services;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.log4j.Logger;
import org.jdom.JDOMException;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.iview2.MCRImage;
import org.mycore.services.iview2.MCRTiledPictureProps;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRIView2Tools {

    private static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    private static String SUPPORTED_CONTENT_TYPE = CONFIG.getString("MCR.Module-iview2.SupportedContentTypes", "");

    private static File TILE_DIR = new File(MCRIview2Props.getProperty("DirectoryForTiles"));

    private static Logger LOGGER = Logger.getLogger(MCRIView2Tools.class);

    /**
     * @return directory for tiles
     */
    public static File getTileDir() {
        return TILE_DIR;
    }

    public static String getSupportedMainFile(String derivateID) {
        try {
            MCRDerivate deriv = MCRMetadataManager.retrieveMCRDerivate(new MCRObjectID(derivateID));
            String nameOfMainFile = deriv.getDerivate().getInternals().getMainDoc();
            // verify support
            if (nameOfMainFile != null && !nameOfMainFile.equals("")) {
                MCRFile mainFile = getMCRFile(derivateID, nameOfMainFile);
                if (mainFile != null && isFileSupported(mainFile))
                    return mainFile.getAbsolutePath();
            }
        } catch (Exception e) {
            LOGGER.warn("Could not get main file of derivate.", e);
        }
        return "";
    }

    public static File getFile(String derivateID, String absolutePath) {
        MCRFile mcrFile = getMCRFile(derivateID, absolutePath);
        return getFile(mcrFile);
    }

    public static String getFilePath(String derivateID, String absolutePath) {
        MCRFile mcrFile = getMCRFile(derivateID, absolutePath);
        return mcrFile.getStorageID();
    }

    public static MCRFile getMCRFile(String derivateID, String absolutePath) {
        MCRDirectory root = (MCRDirectory) MCRFilesystemNode.getRootNode(derivateID);
        if (root == null)
            throw new MCRPersistenceException("Could not get root node of derivate " + derivateID);
        // get main file
        MCRFile mainFile = (MCRFile) root.getChildByPath(absolutePath);
        return mainFile;
    }

    public static boolean isDerivateSupported(String derivateID) {
        return getSupportedMainFile(derivateID).length() > 0;
    }

    public static boolean isFileSupported(MCRFile file) {
        return SUPPORTED_CONTENT_TYPE.indexOf(file.getContentTypeID()) > -1;
    }

    public static boolean isTiled(MCRFile file) {
        File tiledFile = MCRImage.getTiledFile(getTileDir(), file.getOwnerID(), file.getAbsolutePath());
        return tiledFile.exists();
    }

    public static BufferedImage getZoomLevel(File iviewFile, int zoomLevel) throws IOException, JDOMException {
        ZipFile iviewImage = new ZipFile(iviewFile);
        Graphics graphics = null;
        ImageReader reader = getTileImageReader();
        try {
            if (zoomLevel == 0) {
                return readTile(iviewImage, reader, 0, 0, 0);
            }
            MCRTiledPictureProps imageProps = MCRTiledPictureProps.getInstance(iviewFile);
            if (zoomLevel < 0 || zoomLevel > imageProps.getZoomlevel()) {
                throw new IndexOutOfBoundsException("Zoom level " + zoomLevel + " is not in range 0 - " + imageProps.getZoomlevel());
            }
            double zoomFactor = Math.pow(2, (imageProps.getZoomlevel() - zoomLevel));
            int maxX = (int) Math.ceil((imageProps.getWidth() / zoomFactor) / MCRImage.getTileSize());
            int maxY = (int) Math.ceil((imageProps.getHeight() / zoomFactor) / MCRImage.getTileSize());
            LOGGER.info(MessageFormat.format("Image size:{0}x{1}, tiles:{2}x{3}", imageProps.getWidth(), imageProps.getHeight(), maxX, maxY));
            BufferedImage sampleTile = readTile(iviewImage, reader, zoomLevel, maxX - 1, 0);
            int xDim = ((maxX - 1) * MCRImage.getTileSize() + sampleTile.getWidth());
            int yDim = ((maxY - 1) * MCRImage.getTileSize() + readTile(iviewImage, reader, zoomLevel, 0, maxY - 1).getHeight());
            BufferedImage resultImage = new BufferedImage(xDim, yDim, sampleTile.getType());
            graphics = resultImage.getGraphics();
            for (int x = 0; x < maxX; x++) {
                for (int y = 0; y < maxY; y++) {
                    BufferedImage tile = readTile(iviewImage, reader, zoomLevel, x, y);
                    graphics.drawImage(tile, x * MCRImage.getTileSize(), y * MCRImage.getTileSize(), null);
                }
            }
            return resultImage;
        } finally {
            iviewImage.close();
            if (graphics != null)
                graphics.dispose();
            reader.dispose();
        }
    }

    private static ImageReader getTileImageReader() {
        return ImageIO.getImageReadersByMIMEType("image/jpeg").next();
    }

    private static BufferedImage readTile(ZipFile iviewImage, ImageReader imageReader, int zoomLevel, int x, int y) throws IOException {
        String tileName = MessageFormat.format("{0}/{1}/{2}.jpg", zoomLevel, y, x);
        ZipEntry tile = iviewImage.getEntry(tileName);
        if (tile != null) {
            InputStream zin = iviewImage.getInputStream(tile);
            try {
                ImageInputStream iis = ImageIO.createImageInputStream(zin);
                imageReader.setInput(iis, false);
                BufferedImage image = imageReader.read(0);
                imageReader.reset();
                iis.close();
                return image;
            } finally {
                zin.close();
            }
        } else {
            LOGGER.warn("Did not find " + tileName + " in " + iviewImage.getName());
            return null;
        }
    }

    static File getFile(MCRFile image) {
        String storageID = image.getStorageID();
        String storeID = image.getStoreID();
        String baseDirName = MCRConfiguration.instance().getString("MCR.IFS.ContentStore." + storeID + ".URI");
        File file = new File(baseDirName + File.separatorChar + storageID);
        return file;
    }

}
