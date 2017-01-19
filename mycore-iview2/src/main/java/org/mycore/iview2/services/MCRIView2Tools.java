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

package org.mycore.iview2.services;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRAbstractFileStore;
import org.mycore.datamodel.niofs.MCRContentTypes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.imagetiler.MCRImage;
import org.mycore.imagetiler.MCRTiledPictureProps;

/**
 * Tools class with common methods for IView2.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRIView2Tools {

    private static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    public static final String CONFIG_PREFIX = "MCR.Module-iview2.";

    private static String SUPPORTED_CONTENT_TYPE = CONFIG.getString(CONFIG_PREFIX + "SupportedContentTypes", "");

    private static Path TILE_DIR = Paths.get(MCRIView2Tools.getIView2Property("DirectoryForTiles"));

    private static Logger LOGGER = LogManager.getLogger(MCRIView2Tools.class);

    /**
     * @return directory for tiles
     */
    public static Path getTileDir() {
        return TILE_DIR;
    }

    /**
     * @param derivateID
     *            ID of derivate
     * @return empty String or absolute path to main file of derivate if file is supported.
     */
    public static String getSupportedMainFile(String derivateID) {
        try {
            MCRDerivate deriv = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(derivateID));
            String nameOfMainFile = deriv.getDerivate().getInternals().getMainDoc();
            // verify support
            if (nameOfMainFile != null && !nameOfMainFile.equals("")) {
                MCRPath mainFile = MCRPath.getPath(derivateID, '/' + nameOfMainFile);
                if (mainFile != null && isFileSupported(mainFile))
                    return mainFile.getRoot().relativize(mainFile).toString();
            }
        } catch (Exception e) {
            LOGGER.warn("Could not get main file of derivate.", e);
        }
        return "";
    }

    /**
     * @param derivateID
     *            ID of derivate
     * @return true if {@link #getSupportedMainFile(String)} is not an empty String.
     */
    public static boolean isDerivateSupported(String derivateID) {
        if (derivateID == null || derivateID.trim().length() == 0) {
            return false;
        }

        return getSupportedMainFile(derivateID).length() > 0;
    }

    /**
     * @param file
     *            image file
     * @return if content type is in property <code>MCR.Module-iview2.SupportedContentTypes</code>
     * @see MCRContentTypes#probeContentType(Path)
     */
    public static boolean isFileSupported(Path file) throws IOException {
        return file == null ? false : SUPPORTED_CONTENT_TYPE.contains(MCRContentTypes.probeContentType(file));
    }

    /**
     * @return true if the file is supported, false otherwise
     */
    public static boolean isFileSupported(String filename) {
        return SUPPORTED_CONTENT_TYPE.contains(MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(
                filename.toLowerCase(Locale.ROOT)));
    }

    /**
     * Checks for a given derivate id whether all files in that derivate are tiled.
     * 
     * @return true if all files in belonging to the derivate are tiled, false otherwise
     */
    public static boolean isCompletelyTiled(String derivateId) throws IOException {
        if (!MCRMetadataManager.exists(MCRObjectID.getInstance(derivateId))) {
            return false;
        }
        MCRPath derivatePath = MCRPath.getPath(derivateId, "/");
        TileCompleteFileVisitor tileCompleteFileVisitor = new TileCompleteFileVisitor();
        try {
            Files.walkFileTree(derivatePath, tileCompleteFileVisitor);
        } catch (Exception ex) {
            LOGGER.warn("Could not check tile status of derivate", ex);
            return false;
        }
        return tileCompleteFileVisitor.isTiled();
    }

    private static class TileCompleteFileVisitor extends SimpleFileVisitor<Path> {

        private boolean isTiled = true;

        public boolean isTiled() {
            return isTiled;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            MCRPath mcrFile = MCRPath.toMCRPath(file);
            if (isFileSupported(mcrFile)) {
                if (!MCRIView2Tools.isTiled(mcrFile)) {
                    isTiled = false;
                    return FileVisitResult.TERMINATE;
                }
            }
            return super.visitFile(file, attrs);
        }
    }

    /**
     * @param file
     *            image file
     * @return true if {@link MCRImage#getTiledFile(Path, String, String)} exists
     * @see #getTileDir()
     */
    public static boolean isTiled(MCRPath file) {
        Path tiledFile = MCRImage.getTiledFile(getTileDir(), file.getOwner(), file.subpathComplete().toString());
        return Files.exists(tiledFile);
    }

    /**
     * combines image tiles of specified zoomLevel to one image.
     * 
     * @param iviewFile
     *            .iview2 file
     * @param zoomLevel
     *            the zoom level where 0 is thumbnail size
     * @return a combined image
     * @throws IOException
     *             any IOException while reading tiles
     * @throws JDOMException
     *             if image properties could not be parsed.
     */
    public static BufferedImage getZoomLevel(Path iviewFile, int zoomLevel) throws IOException, JDOMException {
        ImageReader reader = getTileImageReader();
        try (FileSystem zipFileSystem = getFileSystem(iviewFile)) {
            Path iviewFileRoot = zipFileSystem.getRootDirectories().iterator().next();
            MCRTiledPictureProps imageProps = MCRTiledPictureProps.getInstanceFromDirectory(iviewFileRoot);
            if (zoomLevel < 0 || zoomLevel > imageProps.getZoomlevel()) {
                throw new IndexOutOfBoundsException("Zoom level " + zoomLevel + " is not in range 0 - " + imageProps.getZoomlevel());
            }
            return getZoomLevel(iviewFileRoot, imageProps, reader, zoomLevel);
        } finally {
            reader.dispose();
        }
    }

    /**
     * combines image tiles of specified zoomLevel to one image.
     * 
     * @param iviewFileRoot
     *            root directory of .iview2 file
     * @param imageProperties
     *            imageProperties, if available or null
     * @param zoomLevel
     *            the zoom level where 0 is thumbnail size
     * @return a combined image
     * @throws IOException
     *             any IOException while reading tiles
     * @throws JDOMException
     *             if image properties could not be parsed.
     */
    public static BufferedImage getZoomLevel(final Path iviewFileRoot, final MCRTiledPictureProps imageProperties,
            final ImageReader reader, final int zoomLevel) throws IOException, JDOMException {
        if (zoomLevel == 0) {
            return readTile(iviewFileRoot, reader, 0, 0, 0);
        }
        MCRTiledPictureProps imageProps = imageProperties == null ? MCRTiledPictureProps.getInstanceFromDirectory(iviewFileRoot)
                : imageProperties;
        double zoomFactor = Math.pow(2, (imageProps.getZoomlevel() - zoomLevel));
        int maxX = (int) Math.ceil((imageProps.getWidth() / zoomFactor) / MCRImage.getTileSize());
        int maxY = (int) Math.ceil((imageProps.getHeight() / zoomFactor) / MCRImage.getTileSize());
        LOGGER.debug(MessageFormat.format("Image size:{0}x{1}, tiles:{2}x{3}", imageProps.getWidth(), imageProps.getHeight(), maxX, maxY));
        int imageType = getImageType(iviewFileRoot, reader, zoomLevel, 0, 0);
        int xDim = ((maxX - 1) * MCRImage.getTileSize() + readTile(iviewFileRoot, reader, zoomLevel, maxX - 1, 0).getWidth());
        int yDim = ((maxY - 1) * MCRImage.getTileSize() + readTile(iviewFileRoot, reader, zoomLevel, 0, maxY - 1).getHeight());
        BufferedImage resultImage = new BufferedImage(xDim, yDim, imageType);
        Graphics graphics = resultImage.getGraphics();
        try {
            for (int x = 0; x < maxX; x++) {
                for (int y = 0; y < maxY; y++) {
                    BufferedImage tile = readTile(iviewFileRoot, reader, zoomLevel, x, y);
                    graphics.drawImage(tile, x * MCRImage.getTileSize(), y * MCRImage.getTileSize(), null);
                }
            }
            return resultImage;
        } finally {
            graphics.dispose();
        }
    }

    public static FileSystem getFileSystem(Path iviewFile) throws IOException {
        URI uri = URI.create("jar:" + iviewFile.toUri().toString());
        try {
            return FileSystems.newFileSystem(uri, Collections.<String, Object> emptyMap(), MCRIView2Tools.class.getClassLoader());
        } catch (FileSystemAlreadyExistsException exc) {
            // block until file system is closed
            try {
                FileSystem fileSystem = FileSystems.getFileSystem(uri);
                while (fileSystem.isOpen()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ie) {
                        // get out of here
                        throw new IOException(ie);
                    }
                }
            } catch (FileSystemNotFoundException fsnfe) {
                // seems closed now -> do nothing and try to return the file system again
                LOGGER.debug("Filesystem not found", fsnfe);
            }
            return getFileSystem(iviewFile);
        }
    }

    public static ImageReader getTileImageReader() {
        return ImageIO.getImageReadersByMIMEType("image/jpeg").next();
    }

    public static BufferedImage readTile(Path iviewFileRoot, ImageReader imageReader, int zoomLevel, int x, int y) throws IOException {
        String tileName = MessageFormat.format("{0}/{1}/{2}.jpg", zoomLevel, y, x);
        Path tile = iviewFileRoot.resolve(tileName);
        if (Files.exists(tile)) {
            try (SeekableByteChannel fileChannel = Files.newByteChannel(tile)) {
                ImageInputStream iis = ImageIO.createImageInputStream(fileChannel);
                if (iis == null) {
                    throw new IOException("Could not acquire ImageInputStream from SeekableByteChannel: " + tile);
                }
                imageReader.setInput(iis, true);
                BufferedImage image = imageReader.read(0);
                imageReader.reset();
                iis.close();
                return image;
            }
        } else {
            throw new NoSuchFileException(iviewFileRoot.toString(), tileName, null);
        }
    }

    public static int getImageType(Path iviewFileRoot, ImageReader imageReader, int zoomLevel, int x, int y) throws IOException {
        String tileName = MessageFormat.format("{0}/{1}/{2}.jpg", zoomLevel, y, x);
        Path tile = iviewFileRoot.resolve(tileName);
        if (Files.exists(tile)) {
            try (SeekableByteChannel fileChannel = Files.newByteChannel(tile)) {
                ImageInputStream iis = ImageIO.createImageInputStream(fileChannel);
                if (iis == null) {
                    throw new IOException("Could not acquire ImageInputStream from SeekableByteChannel: " + tile);
                }
                imageReader.setInput(iis, true);
                int imageType = MCRImage.getImageType(imageReader);
                imageReader.reset();
                iis.close();
                return imageType;
            }
        } else {
            throw new NoSuchFileException(iviewFileRoot.toString(), tileName, null);
        }
    }

    /**
     * short for {@link MCRIView2Tools}{@link #getIView2Property(String, String)} defaultProp = null
     */
    public static String getIView2Property(String propName) {
        return getIView2Property(propName, null);
    }

    /**
     * short for <code>MCRConfiguration.instance().getString("MCR.Module-iview2." + propName, defaultProp);</code>
     * 
     * @param propName
     *            any suffix
     * @return null or property value
     */
    public static String getIView2Property(String propName, String defaultProp) {
        return MCRConfiguration.instance().getString(CONFIG_PREFIX + propName, defaultProp);
    }

    /**
     * Calculates the url to the image viewer displaying the given file.
     * 
     * @param file
     *            the file to display
     * @return the url to the image viewer displaying given file unless {@link MCRIView2Tools#isFileSupported(Path)}
     *         returns <code>false</code> in this case <code>null</code> is returned
     * @see MCRIView2Tools#isFileSupported(Path)
     */
    public static String getViewerURL(MCRPath file) throws URISyntaxException, IOException {
        if (!MCRIView2Tools.isFileSupported(file)) {
            return null;
        }
        MCRObjectID mcrObjectID = MCRMetadataManager.getObjectId(MCRObjectID.getInstance(file.getOwner()), 10, TimeUnit.SECONDS);
        String params = MCRXMLFunctions.encodeURIPath(MessageFormat.format("jumpback=true&maximized=true&page={0}&derivate={1}",
                file.subpathComplete(), file.getOwner()));
        String url = MessageFormat.format("{0}receive/{1}?{2}", MCRFrontendUtil.getBaseURL(), mcrObjectID, params);

        return url;
    }

    public static String getFilePath(String derID, String derPath) throws IOException {
        MCRPath mcrPath = MCRPath.getPath(derID, derPath);
        Path physicalPath = mcrPath.toPhysicalPath();
        for (FileStore fs : mcrPath.getFileSystem().getFileStores()) {
            if (fs instanceof MCRAbstractFileStore) {
                Path basePath = ((MCRAbstractFileStore) fs).getBaseDirectory();
                if (physicalPath.startsWith(basePath)) {
                    return basePath.relativize(physicalPath).toString();
                }
            }
        }
        return physicalPath.toString();
    }
}
