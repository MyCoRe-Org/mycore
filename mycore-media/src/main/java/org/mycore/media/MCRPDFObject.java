package org.mycore.media;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.jdom2.Element;

public class MCRPDFObject extends MCRMediaObject {
    protected int numPages;

    protected int width;

    protected int height;

    public MCRPDFObject() {
        type = MCRMediaObject.MediaType.TEXT;
    }

    public int getNumPages() {
        return numPages;
    }

    public void setNumPages(int numPages) {
        this.numPages = numPages;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public long getMaxSeekPosition() {
        return numPages;
    }

    public boolean hasThumbnailSupport() {
        return true;
    }

    public int[] getScaledSize(int maxWidth, int maxHeight, boolean keepAspect) {
        int scaledSize[] = new int[2];

        maxWidth = (maxWidth == 0 ? 256 : maxWidth);
        maxHeight = (maxHeight == 0 ? 256 : maxHeight);

        if (keepAspect) {
            float scaleFactor = (maxWidth >= maxHeight ? Float.intBitsToFloat(maxWidth) / Float.intBitsToFloat(width)
                : Float
                    .intBitsToFloat(maxHeight) / Float.intBitsToFloat(height));

            scaledSize[0] = Math.round(width * scaleFactor);
            scaledSize[1] = Math.round(height * scaleFactor);
        } else {
            scaledSize[0] = maxWidth;
            scaledSize[1] = maxHeight;
        }

        return scaledSize;
    }

    /**
     * Take a Snapshot from PDFObject.
     * 
     * @param media
     *              the MediaObject
     * @param seek
     *              position to take a snapshot
     * @param maxWidth
     *              maximum output width
     * @param maxHeight
     *              maximum output height
     * @param keepAspect
     *              set to keep aspect ratio
     */
    @SuppressWarnings("unchecked")
    public synchronized byte[] getThumbnail(MCRMediaObject media, long seek, int maxWidth, int maxHeight,
        boolean keepAspect)
        throws Exception {
        byte[] imageInByte = null;

        PDDocument pdf = PDDocument.load(new File(media.folderName + media.fileName));

        try {
            PDPageTree pages = pdf.getDocumentCatalog().getPages();
            PDFRenderer pdfRenderer = new PDFRenderer(pdf);

            BufferedImage image = pdfRenderer.renderImageWithDPI((int) seek, 96, ImageType.RGB);

            int[] scaledSize = ((MCRPDFObject) media).getScaledSize(maxWidth, maxHeight, keepAspect);

            BufferedImage resized = new BufferedImage(scaledSize[0], scaledSize[1], image.getType());
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.drawImage(image, 0, 0, scaledSize[0], scaledSize[1], 0, 0, image.getWidth(), image.getHeight(), null);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            baos.flush();
            imageInByte = baos.toByteArray();
            baos.close();
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        } finally {
            pdf.close();
        }

        return imageInByte;
    }

    /**
     * Output metadata as XML.
     * 
     * @return an JDOM Element
     */
    public Element toXML() {
        Element xml;

        xml = new Element("media");
        xml.setAttribute("type", "text");
        createElement(xml, "@mimeType", mimeType);
        createElement(xml, "@numPages", String.valueOf(numPages));
        createElement(xml, "@width", String.valueOf(width));
        createElement(xml, "@height", String.valueOf(height));

        if (!XMLwithoutFileInfo) {
            Element file = new Element("file");
            file.addContent(fileName);
            createElement(file, "@size", String.valueOf(fileSize));
            createElement(file, "@path", folderName);
            xml.addContent(file);
        }

        if (tags != null)
            xml.addContent(tags.toXML());

        return xml;
    }

    /**
     * Builds PDFObject from XML.
     * 
     * @return the PDFObject
     */
    public static MCRPDFObject buildFromXML(Element xml) {
        MCRPDFObject pdf = new MCRPDFObject();

        pdf.numPages = Integer.parseInt(getXMLValue(xml, "@numPages"));
        pdf.width = Integer.parseInt(getXMLValue(xml, "@width"));
        pdf.height = Integer.parseInt(getXMLValue(xml, "@height"));

        pdf.fileSize = Long.parseLong(getXMLValue(xml, "file/@size", "0"));
        pdf.folderName = getXMLValue(xml, "file/@path");
        pdf.fileName = getXMLValue(xml, "file");

        Element elmTags = xml.getChild("tags");
        if (elmTags != null)
            pdf.tags = MCRMediaTagObject.buildFromXML(elmTags);

        return pdf;
    }
}
