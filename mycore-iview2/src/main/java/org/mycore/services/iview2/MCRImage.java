/**
 * $RCSfile: MCRImage.java,v $
 * $Revision: 1.0 $ $Date: 09.10.2008 08:37:05 $
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.services.iview2;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRFile;

import com.sun.media.jai.codec.MemoryCacheSeekableStream;

public class MCRImage {

    private File imageFile;

    private BufferedImage waterMarkFile;

    private static Logger LOGGER = Logger.getLogger(MCRImage.class);

    BufferedImage image;

    private static final int TILE_SIZE = 256;

    private AtomicInteger imageTilesCount = new AtomicInteger();

    private int imageWidth;

    private int imageHeight;

    private int imageZoomLevels;

    private String derivate;

    private String imagePath;

    public MCRImage(MCRFile image) {
        this.imageFile = getFile(image);
        this.derivate = image.getOwnerID();
        this.imagePath = image.getAbsolutePath();
        LOGGER.info("MCRImage initialized");
    }

    private File getFile(MCRFile image2) {
        String storageID = image2.getStorageID();
        String storeID = image2.getStoreID();
        String baseDirName = MCRConfiguration.instance().getString("MCR.IFS.ContentStore." + storeID + ".URI");
        File file = new File(baseDirName + File.separatorChar + storageID);
        return file;
    }

    /*public void addWaterMark(File image) {
    	this.waterMarkFile = image;
    }*/

    public MCRTiledPictureProps tile() throws IOException {
        //the absolute Path is the docportal-directory, therefore the path "../mycore/..."
        //waterMarkFile = ImageIO.read(new File(MCRIview2Props.getProperty("Watermark")));	
        File iviewFile = getTiledFile(derivate, imagePath);
        iviewFile.getParentFile().mkdirs();
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(iviewFile));
        try {
            BufferedImage image = loadImage();
            int zoomLevels = getZoomLevels(image);
            LOGGER.info("Will generate " + zoomLevels + " zoom levels.");
            for (int z = zoomLevels; z >= 0; z--) {
                LOGGER.info("Generating zoom level " + z);
                //image = reformatImage(scale(image));
                LOGGER.info("Writing out tiles..");

                int getMaxTileY = (int) Math.ceil(image.getHeight() / TILE_SIZE);
                int getMaxTileX = (int) Math.ceil(image.getWidth() / TILE_SIZE);
                for (int y = 0; y <= getMaxTileY; y++) {
                    for (int x = 0; x <= getMaxTileX; x++) {
                        writeTile(zout, image, x, y, z);
                    }
                }
                if (z > 0)
                    image = scaleBufferedImage(image);
            }
            writeMetaData(zout);
            MCRTiledPictureProps picProps = new MCRTiledPictureProps();
            picProps.width = imageWidth;
            picProps.height = imageHeight;
            picProps.zoomlevel = imageZoomLevels;
            picProps.countTiles = imageTilesCount.get();
            return picProps;
        } finally {
            zout.close();
        }
    }

    private BufferedImage scaleBufferedImage(BufferedImage image) {
        LOGGER.info("Scaling image...");
        BufferedImage scaled;
        int width = image.getWidth() / 2;
        int height = image.getHeight() / 2;
        if (image.getType() == 0) {
            if (image.getColorModel().getPixelSize() > 8) {
                scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            } else {
                scaled = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            }
        } else {
            scaled = new BufferedImage(width, height, image.getType());
        }
        scaled.createGraphics().drawImage(image, 0, 0, width, height, null);
        LOGGER.info("Scaling done: " + width + "x" + height);
        return scaled;
    }

    private BufferedImage loadImage() throws IOException {
        BufferedImage image;
        RenderedOp render = getImage(imageFile);
        LOGGER.info("Converting to BufferedImage");
        // handle images with 32 and more bits
        if (render.getColorModel().getPixelSize() > 24) {
            // convert to 24 bit
            LOGGER.info("Converting image to 24 bit color depth");
            image = new BufferedImage(render.getWidth(), render.getHeight(), BufferedImage.TYPE_INT_RGB);
            image.createGraphics().drawImage(render.getAsBufferedImage(), 0, 0, render.getWidth(), render.getHeight(), null);
        } else {
            image = render.getAsBufferedImage();
        }
        LOGGER.info("Done loading image: " + image);
        return image;
    }

    @SuppressWarnings("unused")
    private BufferedImage getMemImage(File imageFile) throws FileNotFoundException {
        MemoryCacheSeekableStream stream = new MemoryCacheSeekableStream(new BufferedInputStream(new FileInputStream(imageFile)));
        final RenderedOp create = JAI.create("stream", stream);
        return create.getAsBufferedImage();
    }

    @SuppressWarnings("unused")
    private BufferedImage readAnImage(File imageFile2) throws FileNotFoundException, IOException {
        FileImageInputStream is = new FileImageInputStream(imageFile2);
        for (Iterator<ImageReader> it = ImageIO.getImageReaders(is); it.hasNext();) {
            ImageReader ir = it.next();
            ir.setInput(is, true);
            return ir.read(0);
        }
        return null;
    }

    private RenderedOp getImage(File imageFile) {
        LOGGER.info("Reading image: " + imageFile);
        RenderedOp render;
        render = JAI.create("fileload", imageFile.getAbsolutePath());
        return render;
    }

    private int getZoomLevels(RenderedImage image) {
        int maxDim = image.getHeight() > image.getWidth() ? image.getHeight() : image.getWidth();
        int zoomLevel = 0;
        while (maxDim > TILE_SIZE) {
            zoomLevel++;
            maxDim = maxDim / 2;
        }
        imageHeight = image.getHeight();
        imageWidth = image.getWidth();
        imageZoomLevels = zoomLevel;
        return zoomLevel;
    }

    private void writeTile(ZipOutputStream zout, BufferedImage image, int x, int y, int zoom) throws IOException {

        int tileWidth = image.getWidth() - TILE_SIZE * x;
        int tileHeight = image.getHeight() - TILE_SIZE * y;
        if (tileWidth > TILE_SIZE)
            tileWidth = TILE_SIZE;
        if (tileHeight > TILE_SIZE)
            tileHeight = TILE_SIZE;
        if (tileWidth != 0 && tileHeight != 0) {
            ZipEntry ze = new ZipEntry(new StringBuilder(Integer.toString(zoom)).append('/').append(y).append('/').append(x).append(".jpg")
                    .toString());
            try {
                zout.putNextEntry(ze);
                BufferedImage tile = image.getSubimage(x * TILE_SIZE, y * TILE_SIZE, tileWidth, tileHeight);
                ImageWriter imageWriter = ImageIO.getImageWritersBySuffix("jpeg").next();
                JPEGImageWriteParam imageWriteParam = new JPEGImageWriteParam(Locale.getDefault());
                try {
                    imageWriteParam.setProgressiveMode(JPEGImageWriteParam.MODE_DEFAULT);
                } catch (UnsupportedOperationException e) {
                    LOGGER.warn("Your JPEG encoder does not support progressive JPEGs.");
                }
                imageWriteParam.setCompressionMode(JPEGImageWriteParam.MODE_EXPLICIT);
                imageWriteParam.setCompressionQuality(0.8f);
                ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(zout);
                imageWriter.setOutput(imageOutputStream);
                //tile = addWatermark(scaleBufferedImage(tile));		
                IIOImage iioImage=new IIOImage(tile, null, null);
                imageWriter.write(null, iioImage, imageWriteParam);
                imageWriter.dispose();
                //close imageOutputStream after disposing imageWriter or else application will hang
                imageOutputStream.close();
                imageTilesCount.incrementAndGet();
            } finally {
                zout.closeEntry();
            }
        }
    }

    private void writeMetaData(ZipOutputStream zout) throws IOException {
        ZipEntry ze = new ZipEntry("imageinfo.xml");
        zout.putNextEntry(ze);
        try {
            Element rootElement = new Element("imageinfo");
            Document imageInfo = new Document(rootElement);
            rootElement.setAttribute("derivate", derivate);
            rootElement.setAttribute("path", imagePath);
            rootElement.setAttribute("tiles", imageTilesCount.toString());
            rootElement.setAttribute("width", Integer.toString(imageWidth));
            rootElement.setAttribute("height", Integer.toString(imageHeight));
            rootElement.setAttribute("zoomLevel", Integer.toString(imageZoomLevels));
            XMLOutputter xout = new XMLOutputter(Format.getCompactFormat());
            xout.output(imageInfo, zout);
        } finally {
            zout.closeEntry();
        }
    }

    public BufferedImage addWatermark(BufferedImage image) {
        if (image.getWidth() >= waterMarkFile.getWidth() && image.getHeight() >= waterMarkFile.getHeight()) {
            int randx = (int) (Math.random() * (image.getWidth() - waterMarkFile.getWidth()));
            int randy = (int) (Math.random() * (image.getHeight() - waterMarkFile.getHeight()));
            image.createGraphics().drawImage(waterMarkFile, randx, randy, waterMarkFile.getWidth(), waterMarkFile.getHeight(), null);
        }
        return image;
    }

    /**
     * returns a {@link File} object of the .iview2 file or the derivate folder.
     * @param derivate derivateID
     * @param imagePath absolute image path or <code>null</code>
     * @return tile directory of derivate if <code>imagePath</code> is null or the tile file (.iview2)
     */
    public static File getTiledFile(String derivate, String imagePath) {
        File tileDir = new File(MCRIview2Props.getProperty("DirectoryForTiles"));
        String[] idParts = derivate.split("_");
        for (int i = 0; i < idParts.length - 1; i++) {
            tileDir = new File(tileDir, idParts[i]);
        }
        String lastPart = idParts[idParts.length - 1];
        if (lastPart.length() > 3) {
            tileDir = new File(tileDir, lastPart.substring(lastPart.length() - 4, lastPart.length() - 2));
            tileDir = new File(tileDir, lastPart.substring(lastPart.length() - 2, lastPart.length()));
        } else {
            tileDir = new File(tileDir, lastPart);
        }
        tileDir = new File(tileDir, derivate);
        if (imagePath == null)
            return tileDir;
        String relPath = imagePath.substring(0, imagePath.lastIndexOf('.')) + ".iview2";
        return new File(tileDir.getAbsolutePath() + "/" + relPath);
    }
}
