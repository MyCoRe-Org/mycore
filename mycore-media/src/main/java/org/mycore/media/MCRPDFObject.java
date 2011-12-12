package org.mycore.media;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.jdom.Element;

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
            float scaleFactor = (maxWidth >= maxHeight ? Float.intBitsToFloat(maxWidth) / Float.intBitsToFloat(width) : Float
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
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public synchronized byte[] getThumbnail(MCRMediaObject media, long seek, int maxWidth, int maxHeight, boolean keepAspect)
            throws Exception {
        try {
            byte[] imageInByte = null;

            PDDocument pdf = PDDocument.load(new File(media.folderName + media.fileName));

            if (!pdf.isEncrypted()) {
                List<PDPage> pages = pdf.getDocumentCatalog().getAllPages();

                PDPage page = (PDPage) pages.get((int) seek);

                BufferedImage image = page.convertToImage(BufferedImage.TYPE_INT_RGB, 96);

                int[] scaledSize = ((MCRPDFObject) media).getScaledSize(maxWidth, maxHeight, keepAspect);

                BufferedImage resized = new BufferedImage(scaledSize[0], scaledSize[1], image.getType());
                Graphics2D g = resized.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(image, 0, 0, scaledSize[0], scaledSize[1], 0, 0, image.getWidth(), image.getHeight(), null);
                g.dispose();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(resized, "png", baos);
                baos.flush();
                imageInByte = baos.toByteArray();
                baos.close();
            }

            pdf.close();

            return imageInByte;
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Output metadata as XML.
     * 
     * @param withRoot
     *                  complete output or only stream info
     * @return an JDOM Element
     */
    public Element toXML(boolean withRoot) {
        Element xml;

        xml = new Element("media");
        xml.setAttribute("type", "text");
        createElement(xml, "@mimeType", mimeType);
        createElement(xml, "@numPages", String.valueOf(numPages));

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
     * @param xml
     * @return the PDFObject
     */
    public static MCRPDFObject buildFromXML(Element xml) {
        MCRPDFObject pdf = new MCRPDFObject();

        pdf.numPages = Integer.parseInt(getXMLValue(xml, "@numPages"));

        pdf.fileSize = Long.parseLong(getXMLValue(xml, "file/@size", "0"));
        pdf.folderName = getXMLValue(xml, "file/@path");
        pdf.fileName = getXMLValue(xml, "file");
        
        Element elmTags = xml.getChild("tags");
        if (elmTags != null)
            pdf.tags = MCRMediaTagObject.buildFromXML(elmTags);

        return pdf;
    }
}
