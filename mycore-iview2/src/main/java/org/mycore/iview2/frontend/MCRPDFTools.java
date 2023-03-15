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
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDDestinationOrAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.mycore.common.content.MCRContent;
import org.mycore.tools.MCRPNGTools;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRPDFTools implements AutoCloseable {

    static final int PDF_DEFAULT_DPI = 72; // from private org.apache.pdfbox.pdmodel.PDPage.DEFAULT_USER_SPACE_UNIT_DPI

    private static final Logger LOGGER = LogManager.getLogger(MCRPDFTools.class);

    private final MCRPNGTools pngTools;

    MCRPDFTools() {
        this.pngTools = new MCRPNGTools();
    }

    /**
     * The old method did not take the thumbnail size into account, if centered =
     * false;
     *
     * @see #getThumbnail(int, Path, boolean)
     */
    @Deprecated
    public static BufferedImage getThumbnail(Path pdfFile, int thumbnailSize, boolean centered) throws IOException {
        return getThumbnail(-1, pdfFile, false);
    }

    /**
     * This method returns a Buffered Image as thumbnail if an initial page was set,
     * it will be return - if not the first page
     *
     * @param thumbnailSize - the size: size = max(width, height) 
     *                        a size &lt; 0 will return the original size and centered parameter will be ignored
     * @param pdfFile       - the file from which the thumbnail will be taken
     * @param centered      - if true, a square (thumbnail with same width and
     *                      height) will be returned
     * @return a BufferedImage as thumbnail
     *
     * @throws IOException
     */
    public static BufferedImage getThumbnail(int thumbnailSize, Path pdfFile, boolean centered) throws IOException {
        InputStream fileIS = Files.newInputStream(pdfFile);
        try (PDDocument pdf = PDDocument.load(fileIS)) {
            PDFRenderer pdfRenderer = new PDFRenderer(pdf);
            final PDPage page = resolveOpenActionPage(pdf);
            int pageIndex = pdf.getPages().indexOf(page);
            if (pageIndex < 0) {
                // Iterating per page and use equals does also not work in the case I had, so fall back to first page
                LOGGER.warn("Could not resolve initial page, using first page.");
                pageIndex = 0;
            }
            BufferedImage level1Image = pdfRenderer.renderImage(pageIndex);
            if (thumbnailSize < 0) {
                return level1Image;
            }
            return scalePage(thumbnailSize, centered, level1Image);
        }
    }

    private static BufferedImage scalePage(int thumbnailSize, boolean centered, BufferedImage level1Image) {
        int imageType = BufferedImage.TYPE_INT_ARGB;
        final double width = level1Image.getWidth();
        final double height = level1Image.getHeight();
        LOGGER.info("new PDFBox: {}x{}", width, height);
        LOGGER.info("temporary image dimensions: {}x{}", width, height);
        final int newWidth = calculateNewWidth(thumbnailSize, width, height);
        final int newHeight = calculateNewHeight(thumbnailSize, width, height);
        // if centered make thumbnailSize x thumbnailSize image
        final BufferedImage bicubicScaledPage = new BufferedImage(centered ? thumbnailSize : newWidth,
            centered ? thumbnailSize : newHeight, imageType);
        LOGGER.info("target image dimensions: {}x{}", bicubicScaledPage.getWidth(), bicubicScaledPage.getHeight());
        final Graphics2D bg = bicubicScaledPage.createGraphics();
        try {
            bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            int x = centered ? (thumbnailSize - newWidth) / 2 : 0;
            int y = centered ? (thumbnailSize - newHeight) / 2 : 0;
            if (x != 0 && y != 0) {
                LOGGER.warn("Writing at position {},{}", x, y);
            }
            bg.drawImage(level1Image, x, y, x + newWidth, y + newHeight, 0, 0, (int) Math.ceil(width),
                (int) Math.ceil(height), null);
        } finally {
            bg.dispose();
        }
        return bicubicScaledPage;
    }

    private static int calculateNewHeight(int thumbnailSize, double width, double height) {
        return width < height ? thumbnailSize : (int) Math.ceil(thumbnailSize * height / width);
    }

    private static int calculateNewWidth(int thumbnailSize, double width, double height) {
        return width < height ? (int) Math.ceil(thumbnailSize * width / height) : thumbnailSize;
    }

    /**
     *
     * @param pdf - the pdf document
     * @return
     * @throws IOException
     *
     * @see org.mycore.media.services.MCRPdfThumbnailGenerator
     */
    private static PDPage resolveOpenActionPage(PDDocument pdf) throws IOException {
        PDDestinationOrAction openAction = pdf.getDocumentCatalog().getOpenAction();

        if (openAction instanceof PDActionGoTo) {
            final PDDestination destination = ((PDActionGoTo) openAction).getDestination();
            if (destination instanceof PDPageDestination) {
                openAction = destination;
            }
        }

        if (openAction instanceof PDPageDestination) {
            final PDPageDestination namedDestination = (PDPageDestination) openAction;
            final PDPage pdPage = namedDestination.getPage();
            if (pdPage != null) {
                return pdPage;
            } else {
                int pageNumber = namedDestination.getPageNumber();
                if (pageNumber != -1) {
                    return pdf.getPage(pageNumber);
                }
            }
        }

        return pdf.getPage(0);
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
