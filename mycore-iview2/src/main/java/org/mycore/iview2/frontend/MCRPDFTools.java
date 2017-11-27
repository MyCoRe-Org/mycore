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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.mycore.common.content.MCRContent;
import org.mycore.tools.MCRPNGTools;

/**
 * @author Thomas Scheffler (yagee)
 */
class MCRPDFTools implements AutoCloseable {

    static final int PDF_DEFAULT_DPI = 72; //from private org.apache.pdfbox.pdmodel.PDPage.DEFAULT_USER_SPACE_UNIT_DPI

    private static final Color TRANSPARENT_WHITE = new Color(255, 255, 255, 0);

    private static Logger LOGGER = LogManager.getLogger(MCRPDFTools.class);

    private MCRPNGTools pngTools;

    public MCRPDFTools() {
        this.pngTools = new MCRPNGTools();
    }

    static BufferedImage getThumbnail(Path pdfFile, int thumbnailSize, boolean centered) throws IOException {
        InputStream fileIS = Files.newInputStream(pdfFile);
        try (PDDocument pdf = PDDocument.load(fileIS)) {
            PDFRenderer pdfRenderer = new PDFRenderer(pdf);
            BufferedImage level1Image = pdfRenderer.renderImage(0);
            int imageType = BufferedImage.TYPE_INT_ARGB;

            if (!centered) {
                return level1Image;
            }
            final double width = level1Image.getWidth();
            final double height = level1Image.getHeight();
            LOGGER.info("new PDFBox: {}x{}", width, height);
            LOGGER.info("temporary image dimensions: {}x{}", width, height);
            final int newWidth = width < height ? (int) Math.ceil(thumbnailSize * width / height) : thumbnailSize;
            final int newHeight = width < height ? thumbnailSize : (int) Math.ceil(thumbnailSize * height / width);
            //if centered make thumbnailSize x thumbnailSize image
            final BufferedImage bicubic = new BufferedImage(centered ? thumbnailSize : newWidth,
                centered ? thumbnailSize : newHeight, imageType);
            LOGGER.info("target image dimensions: {}x{}", bicubic.getWidth(), bicubic.getHeight());
            final Graphics2D bg = bicubic.createGraphics();
            bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            int x = centered ? (thumbnailSize - newWidth) / 2 : 0;
            int y = centered ? (thumbnailSize - newHeight) / 2 : 0;
            if (x != 0 && y != 0) {
                LOGGER.warn("Writing at position {},{}", x, y);
            }
            bg.drawImage(level1Image, x, y, x + newWidth, y + newHeight, 0, 0, (int) Math.ceil(width),
                (int) Math.ceil(height), null);
            bg.dispose();
            return bicubic;
        }
    }

    MCRContent getThumnail(Path pdfFile, BasicFileAttributes attrs, int thumbnailSize, boolean centered)
        throws IOException {
        BufferedImage thumbnail = MCRPDFTools.getThumbnail(pdfFile, thumbnailSize, centered);
        MCRContent pngContent = pngTools.toPNGContent(thumbnail);
        BasicFileAttributes fattrs = attrs != null ? attrs : Files.readAttributes(pdfFile, BasicFileAttributes.class);
        pngContent.setLastModified(fattrs.lastModifiedTime().toMillis());
        return pngContent;
    }

    @Override
    public void close() throws Exception {
        this.pngTools.close();
    }

}
