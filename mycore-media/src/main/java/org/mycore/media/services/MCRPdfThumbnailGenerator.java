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

package org.mycore.media.services;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDDestinationOrAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.mycore.datamodel.niofs.MCRPath;

public class MCRPdfThumbnailGenerator implements MCRThumbnailGenerator {

    private static final Pattern MATCHING_MIMETYPE = Pattern.compile("^application/pdf");

    @Override
    public boolean matchesFileType(String mimeType, MCRPath path) {
        return MATCHING_MIMETYPE.matcher(mimeType).matches();
    }

    @Override
    public Optional<BufferedImage> getThumbnail(MCRPath path, int size) throws IOException {
        try (InputStream fileIS = Files.newInputStream(path); PDDocument pdf = PDDocument.load(fileIS)) {
            final PDPage page = resolveOpenActionPage(pdf);
            float pdfWidth = page.getCropBox().getWidth();
            float pdfHeight = page.getCropBox().getHeight();
            final int newWidth = pdfWidth > pdfHeight ? (int) Math.ceil(size * pdfWidth / pdfHeight) : size;
            final float scale = newWidth / pdfWidth;

            PDFRenderer pdfRenderer = new PDFRenderer(pdf);
            BufferedImage pdfRender = pdfRenderer.renderImage(pdf.getPages().indexOf(page), scale);
            int imageType = MCRThumbnailUtils.getImageType(pdfRender);
            if (imageType == BufferedImage.TYPE_BYTE_BINARY || imageType == BufferedImage.TYPE_BYTE_GRAY) {
                BufferedImage thumbnail = new BufferedImage(pdfRender.getWidth(), pdfRender.getHeight(),
                    imageType);
                Graphics g = thumbnail.getGraphics();
                g.drawImage(pdfRender, 0, 0, null);
                g.dispose();
                return Optional.of(thumbnail);
            }
            return Optional.of(pdfRender);
        }
    }

    private PDPage resolveOpenActionPage(PDDocument pdf) throws IOException {
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
}
