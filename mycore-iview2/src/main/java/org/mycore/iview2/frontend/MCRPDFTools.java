/*
 * $Id$
 * $Revision: 5697 $ $Date: Mar 31, 2014 $
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.pdfbox.io.RandomAccess;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdfviewer.PageDrawer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.mycore.common.content.MCRContent;
import org.mycore.tools.MCRPNGTools;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
class MCRPDFTools implements AutoCloseable {

    static final int PDF_DEFAULT_DPI = 72; //from private org.apache.pdfbox.pdmodel.PDPage.DEFAULT_USER_SPACE_UNIT_DPI

    private static final Color TRANSPARENT_WHITE = new Color(255, 255, 255, 0);

    private static Logger LOGGER = Logger.getLogger(MCRPDFTools.class);

    private MCRPNGTools pngTools;

    public MCRPDFTools() {
        this.pngTools = new MCRPNGTools();
    }

    static BufferedImage getThumbnail(File pdfFile, int thumbnailSize, boolean centered) throws IOException {
        PDDocument pdf = getPDDocument(pdfFile);
        BufferedImage level1Image;
        int imageType = BufferedImage.TYPE_INT_ARGB;
        try {
            PDDocumentCatalog documentCatalog = pdf.getDocumentCatalog();
            @SuppressWarnings("unchecked")
            List<PDPage> pages = documentCatalog.getAllPages();

            PDPage page = (PDPage) pages.get(0);
            page.findCropBox();
            PDRectangle renderBox = page.getCropBox(); //may not be available but is prefered
            if (renderBox == null) {
                renderBox = page.getTrimBox(); //should always be available
            }
            Dimension dimension = renderBox.createDimension();
            Dimension targetDimension = scaleDimension(dimension, thumbnailSize);
            level1Image = renderPage(page, renderBox, targetDimension, BufferedImage.TYPE_INT_RGB);
        } finally {
            pdf.close();
        }
        if (!centered) {
            return level1Image;
        }
        final double width = level1Image.getWidth();
        final double height = level1Image.getHeight();
        LOGGER.info("temporary image dimensions: " + width + "x" + height);
        final int newWidth = width < height ? (int) Math.ceil(thumbnailSize * width / height) : thumbnailSize;
        final int newHeight = width < height ? thumbnailSize : (int) Math.ceil(thumbnailSize * height / width);
        //if centered make thumbnailSize x thumbnailSize image
        final BufferedImage bicubic = new BufferedImage(centered ? thumbnailSize : newWidth, centered ? thumbnailSize
            : newHeight, imageType);
        LOGGER.info("target image dimensions: " + bicubic.getWidth() + "x" + bicubic.getHeight());
        final Graphics2D bg = bicubic.createGraphics();
        bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        int x = centered ? (thumbnailSize - newWidth) / 2 : 0;
        int y = centered ? (thumbnailSize - newHeight) / 2 : 0;
        if (x != 0 && y != 0) {
            LOGGER.warn("Writing at position " + x + "," + y);
        }
        bg.drawImage(level1Image, x, y, x + newWidth, y + newHeight, 0, 0, (int) Math.ceil(width),
            (int) Math.ceil(height), null);
        bg.dispose();
        return bicubic;
    }

    private static PDDocument getPDDocument(File pdfFile) throws IOException {
        InputStream input = Channels.newInputStream(FileChannel.open(pdfFile.toPath(), StandardOpenOption.READ));
        RandomAccess rafi = new RandomAccessBuffer();
        PDFParser parser = new PDFParser(input, rafi);
        parser.parse();
        return parser.getPDDocument();
    }

    private static Dimension scaleDimension(Dimension source, int maxDim) {
        double scale = (source.height < source.width) ? maxDim / source.getWidth() : maxDim / source.getHeight();
        Dimension returns = new Dimension();
        returns.setSize(source.getWidth() * scale, source.getHeight() * scale);
        return returns;
    }

    private static BufferedImage renderPage(PDPage page, PDRectangle renderBox, Dimension targetDimension, int imageType)
        throws IOException {
        BufferedImage retval = null;
        Dimension sourceDimension = new Dimension();
        sourceDimension.setSize(renderBox.getWidth(), renderBox.getHeight());
        double scaling = Math.max(targetDimension.getHeight() / sourceDimension.getHeight(), targetDimension.getWidth()
            / sourceDimension.getWidth());
        int rotationAngle = page.findRotation();
        // normalize the rotation angle
        if (rotationAngle < 0) {
            rotationAngle += 360;
        } else if (rotationAngle >= 360) {
            rotationAngle -= 360;
        }
        // swap width and height
        if (rotationAngle == 90 || rotationAngle == 270) {
            retval = new BufferedImage(targetDimension.height, targetDimension.width, imageType);
        } else {
            retval = new BufferedImage(targetDimension.width, targetDimension.height, imageType);
        }
        Graphics2D graphics = (Graphics2D) retval.getGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setBackground(TRANSPARENT_WHITE);
        graphics.clearRect(0, 0, retval.getWidth(), retval.getHeight());
        if (rotationAngle != 0) {
            int translateX = 0;
            int translateY = 0;
            switch (rotationAngle) {
                case 90:
                    translateX = retval.getWidth();
                    break;
                case 270:
                    translateY = retval.getHeight();
                    break;
                case 180:
                    translateX = retval.getWidth();
                    translateY = retval.getHeight();
                    break;
                default:
                    break;
            }
            graphics.translate(translateX, translateY);
            graphics.rotate((float) Math.toRadians(rotationAngle));
        }
        graphics.scale(scaling, scaling);
        PageDrawer drawer = new PageDrawer();
        drawer.drawPage(graphics, page, sourceDimension);
        drawer.dispose();
        graphics.dispose();
        return retval;

    }

    MCRContent getThumnail(File pdfFile, int thumbnailSize, boolean centered) throws IOException {
        BufferedImage thumbnail = MCRPDFTools.getThumbnail(pdfFile, thumbnailSize, centered);
        MCRContent pngContent = pngTools.toPNGContent(thumbnail);
        pngContent.setLastModified(pdfFile.lastModified());
        return pngContent;
    }

    @Override
    public void close() throws Exception {
        this.pngTools.close();
    }

}
