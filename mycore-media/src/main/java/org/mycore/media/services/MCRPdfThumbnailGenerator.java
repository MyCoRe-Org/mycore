package org.mycore.media.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.mycore.datamodel.niofs.MCRPath;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Optional;
import java.util.regex.Pattern;

public class MCRPdfThumbnailGenerator implements MCRThumbnailGenerator {

    private static final Pattern MATCHING_MIMETYPE = Pattern.compile("^application/pdf");

    @Override
    public boolean matchesFileType(String mimeType, MCRPath path) {
        return MATCHING_MIMETYPE.matcher(mimeType).matches();
    }

    @Override
    public Optional<BufferedImage> getThumbnail(MCRPath path, int size) throws IOException {
        InputStream fileIS = Files.newInputStream(path.toPhysicalPath());
        try (PDDocument pdf = PDDocument.load(fileIS)) {
            float pdfWidth =  pdf.getPage(0).getCropBox().getWidth();
            float pdfHeight =  pdf.getPage(0).getCropBox().getHeight();
            final int newWidth = pdfWidth > pdfHeight ? (int) Math.ceil(size * pdfWidth / pdfHeight) : size;
            final float scale = newWidth / pdfWidth;

            PDFRenderer pdfRenderer = new PDFRenderer(pdf);
            BufferedImage pdfRender = pdfRenderer.renderImage(0, scale);
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
}
