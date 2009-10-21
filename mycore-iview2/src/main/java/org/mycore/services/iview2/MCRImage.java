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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRFile;

import com.sun.media.jai.codec.MemoryCacheSeekableStream;

public class MCRImage {

    private File imageFile;

    private File outputDirectory;

    private BufferedImage waterMarkFile;

    private static Logger LOGGER = Logger.getLogger(MCRImage.class);

    BufferedImage image;

    private static final int TILE_SIZE = 256;

    private int xmlTiles = 0;

    private int xmlWidth;

    private int xmlHeight;

    private int xmlScales;

    public static String xmlPath;

    private MCRTiledPictureProps picProps = new MCRTiledPictureProps();

    public MCRImage(MCRFile image) {
        if (image != null) {
            this.imageFile = getFile(image);
            MCRConfiguration.instance();
            LOGGER.info("MCRImage initialized");
        }
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
                    writeTile(image, x, y, z);
                }
            }
            if (z > 0)
                image = scaleBufferedImage(image);
        }
        writeMetaData();

        picProps.width = xmlWidth;
        picProps.height = xmlHeight;
        picProps.zoomlevel = xmlScales;
        picProps.countTiles = xmlTiles;
        return picProps;
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
        if (false) {
            LOGGER.info("Reading image: " + imageFile);
            image = readAnImage(imageFile);
            if (image == null)
                image = ImageIO.read(imageFile);
        } else {
            //			if (true) {
            //				LOGGER.info("Reading image: " + imageFile);
            //				image = getMemImage(imageFile);
            //			} else {
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
            //			}
        }

        LOGGER.info("Done loading image: " + image);
        return image;
    }

    private BufferedImage getMemImage(File imageFile) throws FileNotFoundException {
        MemoryCacheSeekableStream stream = new MemoryCacheSeekableStream(new BufferedInputStream(new FileInputStream(imageFile)));
        final RenderedOp create = JAI.create("stream", stream);
        return create.getAsBufferedImage();
    }

    private BufferedImage readAnImage(File imageFile2) throws FileNotFoundException, IOException {
        FileImageInputStream is = new FileImageInputStream(imageFile2);
        for (Iterator<ImageReader> it = ImageIO.getImageReaders(is); it.hasNext();) {
            ImageReader ir = it.next();
            ir.setInput(is, true);
            /*
             * String[] metadataFormatNames =
             * ir.getStreamMetadata().getMetadataFormatNames(); DOMBuilder
             * builder = new DOMBuilder(); XMLOutputter xout = new
             * XMLOutputter(Format.getPrettyFormat()); for (String formatName :
             * metadataFormatNames) { Node node =
             * ir.getStreamMetadata().getAsTree(formatName); Element e =
             * builder.build((org.w3c.dom.Element) node); xout.output(e,
             * System.out); } IIOMetadata meta = ir.getImageMetadata(0);
             * metadataFormatNames = meta.getMetadataFormatNames(); for (String
             * formatName : metadataFormatNames) { if
             * (formatName.startsWith("com_sun_media_imageio_plugins_tiff_image"))
             * continue; Node node = meta.getAsTree(formatName); Element e =
             * builder.build((org.w3c.dom.Element) node); xout.output(e,
             * System.out); }
             */return ir.read(0);
            // it.next().getStreamMetadata().getAsTree(formatName)
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
        xmlHeight = image.getHeight();
        xmlWidth = image.getWidth();
        xmlScales = zoomLevel;
        return zoomLevel;
    }

    private void writeTile(BufferedImage image, int x, int y, int zoom) {
        File zDirectory = new File(outputDirectory, String.valueOf(zoom));
        File yDirectory = new File(zDirectory, String.valueOf(y));

        int tileWidth = image.getWidth() - TILE_SIZE * x;
        int tileHeight = image.getHeight() - TILE_SIZE * y;
        if (tileWidth > TILE_SIZE)
            tileWidth = TILE_SIZE;
        if (tileHeight > TILE_SIZE)
            tileHeight = TILE_SIZE;
        if (tileWidth != 0 && tileHeight != 0) {

            if (!yDirectory.exists())
                yDirectory.mkdirs();

            BufferedImage tile = image.getSubimage(x * TILE_SIZE, y * TILE_SIZE, tileWidth, tileHeight);

            //tile = addWatermark(scaleBufferedImage(tile));		
            JAI.create("filestore", tile, new File(yDirectory, x + ".jpg").getAbsolutePath(), "JPEG");
            xmlTiles++;
        }
    }

    public void setPath(String path) {
        xmlPath = path;
    }

    public void writeMetaData() {
        File txt = new File(outputDirectory + "/ausmasze.xml");
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(txt));
            bw.write("<?xml version='1.0' encoding='utf-8'?>");
            bw.newLine();
            bw.write("<properties>");
            bw.newLine();
            bw.write("<tiles>" + Integer.toString(xmlTiles) + "</tiles>");
            bw.newLine();
            bw.write("<width>" + Integer.toString(xmlWidth) + "</width>");
            bw.newLine();
            bw.write("<height>" + Integer.toString(xmlHeight) + "</height>");
            bw.newLine();
            bw.write("<zoomlevel>" + Integer.toString(xmlScales) + "</zoomlevel>");
            bw.newLine();
            bw.write("<path>" + xmlPath + "</path>");
            bw.newLine();
            bw.write("</properties>");
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
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

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

}
