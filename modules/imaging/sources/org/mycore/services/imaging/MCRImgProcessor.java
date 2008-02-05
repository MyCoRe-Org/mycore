package org.mycore.services.imaging;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.media.jai.BorderExtender;
import javax.media.jai.InterpolationBicubic2;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

import org.apache.log4j.Logger;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codec.MemoryCacheSeekableStream;
import com.sun.media.jai.codec.PNGEncodeParam;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.TIFFEncodeParam;

/**
 * This implementation of ImgProcessor is responsible for the manipulation of
 * image data. It offers methods for scaling and croping of an image and a two
 * encoding possibility (JPEG, TIFF). For TIFF-Encoding ones can set the tile
 * size, default tile size is 480.<br>
 * 
 * @version 1.00 8/08/2006<br>
 * @author Vu Huu Chi
 * @linkplain
 * 
 */

public class MCRImgProcessor implements ImgProcessor {
    private static Logger LOGGER = Logger.getLogger(MCRImgProcessor.class.getName());

    private PlanarImage image = null;

    protected float scaleFactor = 0;

    private float jpegQuality = 0.5F;

    private Dimension origSize = null;

    private int tileWidth = 480;

    private int tileHeight = 480;

    private int useEncoder = JPEG_ENC;

    private boolean transparent = false;

    private String fileFormat = "";

    /**
     * The JPEG encoder
     */
    static public final int JPEG_ENC = 0;

    /**
     * The TIFF encoder
     */
    static public final int TIFF_ENC = 1;

    /**
     * The PNG encoder
     */
    static public final int PNG_ENC = 2;

    MCRImgProcessor() {
        origSize = new Dimension(0, 0);

    }

    // Interface ImgProcessor
    public float getJpegQuality() {
        return jpegQuality;
    }

    public void setJpegQuality(float jpegQuality) {
        this.jpegQuality = jpegQuality;
    }

    public void setTileSize(int tileWidth, int tileHeight) {
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
    }

    public void useEncoder(int Encoder) throws Exception {
        if (Encoder == JPEG_ENC || Encoder == TIFF_ENC || Encoder == PNG_ENC)
            useEncoder = Encoder;
        else
            throw new Exception("MCRImgProcessor.useEncoder accept only MCRImgProcessor.JPEG_ENC or MCRImgProcessor.TIFF_ENC as parameter!");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.imaging.ImgProcessor#resizeFitWidth(java.io.InputStream,
     *      int, java.io.OutputStream)
     */
    public void resizeFitWidth(InputStream input, int newWidth, OutputStream output) {
        image = loadImageMEMCache(input);

        if (newWidth != origSize.width)
            image = fitWidth(image, newWidth);

        encode(output);
    }

    public void resizeFitHeight(InputStream input, int newHeight, OutputStream output) {
        image = loadImageMEMCache(input);

        if (newHeight != origSize.height)
            image = fitHeight(image, newHeight);

        encode(output);
    }

    public void resize(InputStream input, int newWidth, int newHeight, OutputStream output) {
        image = loadImageMEMCache(input);
        // loadImageIO(input);

        if (newWidth != origSize.width && newHeight != origSize.height)
            try {
                image = resizeImage(image, newWidth, newHeight);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        encode(output);
    }

    public void scale(InputStream input, float scaleFactor, OutputStream output) {
        image = loadImageMEMCache(input);

        // if (scaleFactor != 1 || useEncoder == PNG_ENC)
        if (scaleFactor != 1)
            image = scaleImage(image, scaleFactor);

        encode(output);
    }

    public void scaleROI(InputStream input, int xTopPos, int yTopPos, int boundWidth, int boundHeight, float scaleFactor, OutputStream output) {
        image = loadImageMEMCache(input);
        // loadImageIO(input);

        LOGGER.debug("Loading Image succesfull!");

        // Point scaleTopCorner = new Point((int) (xTopPos / scaleFactor), (int)
        // (yTopPos / scaleFactor));
        Point scaleTopCorner = new Point(xTopPos, yTopPos);
        Dimension scaleBoundary = new Dimension((int) (boundWidth / scaleFactor), (int) (boundHeight / scaleFactor));

        if (scaleBoundary.width > origSize.width)
            scaleBoundary.width = origSize.width;

        if (scaleBoundary.height > origSize.height)
            scaleBoundary.height = origSize.height;

        LOGGER.debug("MCRImgProcessor - scaleROI#");
        LOGGER.debug("ScaleFactor: " + scaleFactor);
        LOGGER.debug("scaleTopCorner: " + scaleTopCorner);
        LOGGER.debug("scaleBoundary.width: " + scaleBoundary.width);
        LOGGER.debug("scaleBoundary.height: " + scaleBoundary.height);
        LOGGER.debug("origSize.width: " + origSize.width);
        LOGGER.debug("origSize.height: " + origSize.height);

        if (scaleBoundary.width < origSize.width || scaleBoundary.height < origSize.height)
            image = crop(image, scaleTopCorner, scaleBoundary);

        // if (scaleFactor != 1 || useEncoder == PNG_ENC) {
        // if (scaleFactor != 1) {
        image = scaleImage(image, scaleFactor);
        // }

        encode(output);
    }

    public float getScaleFactor() {
        return scaleFactor;
    }

    public Dimension getOrigSize() throws Exception {
        if (image == null)
            throw new Exception("No loaded image in " + this.getClass().getName() + "!");
        return origSize;
    }

    public Dimension getCurrentSize() throws Exception {
        if (image == null)
            throw new Exception("No loaded image in " + this.getClass().getName() + "!");
        return new Dimension(image.getWidth(), image.getHeight());
    }

    public Dimension getImageSize(InputStream input) {
        PlanarImage image = loadImageMEMCache(input);
        return new Dimension(image.getWidth(), image.getHeight());
    }

    public void encode(InputStream input, OutputStream output, int encoder) throws Exception {
        useEncoder(encoder);
        resize(input, origSize.width, origSize.height, output);
    }

    // ****************************************************************************

    public void loadImage(InputStream input) {
        image = loadImageMEMCache(input);
        // loadImageIO(input);
    }

    public PlanarImage loadImageIO(InputStream input) {
        PlanarImage image = null;
        if (input == null)
            LOGGER.debug("Loading a NULL image.. not good!");

        try {
            BufferedImage buffImage = ImageIO.read(input);
            if (buffImage.getTransparency() != BufferedImage.OPAQUE) {
                LOGGER.debug("Loading a transparent image..");
                setTransparent(true);
            } else
                LOGGER.debug("Loading a opague image..");
            image = PlanarImage.wrapRenderedImage(buffImage);
        } catch (NullPointerException e) {
            LOGGER.debug("Loading with imageIO failed, trying JAI instead.");
            try {
                input.reset();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            image = loadImageMEMCache(input);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return image;
    }

    public boolean hasCorrectTileSize() {
        boolean hasCorrectSize = false;

        if (image.getNumXTiles() > 1 && image.getNumYTiles() > 1 && image.getTileWidth() == tileWidth && image.getTileHeight() == tileHeight)
            hasCorrectSize = true;

        return hasCorrectSize;
    }

    public void resizeFitWidth(int newWidth) {
        if (newWidth != origSize.width)
            image = fitWidth(image, newWidth);
    }

    public void resizeFitHeight(int newHeight) {
        if (newHeight != origSize.height)
            image = fitHeight(image, newHeight);
    }

    public void resize(int newWidth, int newHeight) {
        if (newWidth != origSize.width && newHeight != origSize.height)
            try {
                image = resizeImage(image, newWidth, newHeight);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }

    public void scale(float scaleFactor) {
        if (scaleFactor != 1)
            image = scaleImage(image, scaleFactor);
    }

    public void scaleROI(int xTopPos, int yTopPos, int boundWidth, int boundHeight, float scaleFactor) {
        Point scaleTopCorner = new Point((int) (xTopPos / scaleFactor), (int) (yTopPos / scaleFactor));
        Dimension scaleBoundary = new Dimension((int) (boundWidth / scaleFactor), (int) (boundHeight / scaleFactor));

        if (scaleBoundary.width > origSize.width)
            scaleBoundary.width = origSize.width;

        if (scaleBoundary.height > origSize.height)
            scaleBoundary.height = origSize.height;

        if (scaleBoundary.width < origSize.width || scaleBoundary.height < origSize.height)
            image = crop(image, scaleTopCorner, scaleBoundary);

        if (scaleFactor != 1) {
            image = scaleImage(image, scaleFactor);
        }
    }

    public void jpegEncode(OutputStream output) {
        jpegEncode(image, output, jpegQuality);
    }

    public void encodeIO(OutputStream output, String type) {
        // jpegEncode(image, output, jpegQuality);
        try {
            ImageIO.write(image, type, output);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void tiffEncode(OutputStream output) {
        tiffEncode(image, output, true, tileWidth, tileHeight);
    }

    public void pngEncode(OutputStream output) {
        pngEncode(image, output, true);
    }

    // End: Interface implementation

    /** ************************************************************************ */
    // Image operation using JAI
    /**
     * loadImageFileCache - load an input stream of image data into to JAI. JAI
     * use a cache in form of a file on the HDD for the image data. This is
     * slower than using the RAM but for large files it's the only solution.
     * 
     * @param input
     * @return PlanarImage
     */
    public PlanarImage loadImageFileCache(InputStream input) {
        SeekableStream stream = SeekableStream.wrapInputStream(input, true);
        String[] decodeArray = ImageCodec.getDecoderNames(stream);

        if (decodeArray.length == 1 && (decodeArray[0].equals("png") || decodeArray[0].equals("gif"))) {
            // if (decodeArray.length == 1 && (decodeArray[0].equals("png"))) {
            try {
                useEncoder(PNG_ENC);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        image = JAI.create("stream", stream);

        origSize.width = image.getWidth();
        origSize.height = image.getHeight();
        LOGGER.info("File loading successfull - FileCache");
        LOGGER.info("origSize.width: " + origSize.width);
        LOGGER.info("origSize.height: " + origSize.height);
        return image;
    }

    private PlanarImage loadImageMEMCache(InputStream input) {
        MemoryCacheSeekableStream stream = new MemoryCacheSeekableStream(input);
        String[] decodeArray = ImageCodec.getDecoderNames(stream);

        if (decodeArray.length == 1 && (decodeArray[0].equals("png") || decodeArray[0].equals("gif"))) {
            // if (decodeArray.length == 1 && (decodeArray[0].equals("png"))) {
            try {
                useEncoder(PNG_ENC);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        image = JAI.create("stream", stream);

        origSize.width = image.getWidth();
        origSize.height = image.getHeight();
        LOGGER.info("Loading MEM Cache");
        LOGGER.info("origSize.width: " + origSize.width);
        LOGGER.info("origSize.height: " + origSize.height);
        return image;
    }

    private PlanarImage crop(PlanarImage img, Point topCorner, Dimension boundary) {
        LOGGER.debug("croping at " + topCorner + " with dimension " + boundary);
        Dimension origSize = new Dimension(img.getWidth(), img.getHeight());

        if (topCorner.x < 0)
            topCorner.x = 0;
        if (topCorner.y < 0)
            topCorner.y = 0;

        if (topCorner.x + boundary.width > origSize.width)
            topCorner.x = origSize.width - boundary.width;
        if (topCorner.y + boundary.height > origSize.height)
            topCorner.y = origSize.height - boundary.height;

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);
        pb.add((float) topCorner.x); // The xScale
        pb.add((float) topCorner.y); // The yScale
        pb.add((float) boundary.width); // The x translation
        pb.add((float) boundary.height); // The y translation

        return JAI.create("crop", pb, noBorder());

    }

    private PlanarImage scaleImage(PlanarImage img, float scaleFactor) {
        LOGGER.debug("scaling using factor: " + scaleFactor);
        setScaleFactor(scaleFactor);
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img); // The source image
        pb.add(scaleFactor); // The xScale
        pb.add(scaleFactor); // The yScale
        pb.add(0.0F); // The x translation
        pb.add(0.0F); // The y translation
        pb.add(new InterpolationBicubic2(3)); // The interpolation
        return JAI.create("scale", pb, noBorder());
    }

    private PlanarImage fitWidth(PlanarImage img, int newWidth) {
        int origWidth = img.getWidth();
        float xScale = (float) newWidth / (float) origWidth;
        return scaleImage(img, xScale);
    }

    private PlanarImage fitHeight(PlanarImage img, int newHeight) {
        int origHeight = img.getHeight();
        float yScale = (float) newHeight / (float) origHeight;
        return scaleImage(img, yScale);
    }

    private PlanarImage resizeImage(PlanarImage img, int newWidth, int newHeight) throws Exception {
        if (newWidth <= 0 || newHeight <= 0)
            throw new Exception("newWidth and newHeight should be > 0!");

        float scaleFactor = 1;

        LOGGER.debug("MCRImgProcessor - resizeImage             *");
        LOGGER.debug("newWidth: " + newWidth);
        LOGGER.debug("newHeight: " + newHeight);
        LOGGER.debug("origSize.width: " + origSize.width);
        LOGGER.debug("origSize.height: " + origSize.height);

        if (newWidth != origSize.width || newHeight != origSize.height) {
            LOGGER.debug("MCRImgProcessor - resizeImage");
            LOGGER.debug("in IF");
            int origWidth = img.getWidth();
            int origHeight = img.getHeight();
            float xScale = (float) newWidth / (float) origWidth;
            float yScale = (float) newHeight / (float) origHeight;
            scaleFactor = (yScale < xScale) ? yScale : xScale;
        }

        return scaleImage(img, scaleFactor);
    }

    // Encoding Part
    public void encode(OutputStream output, int encoder) {
        if (useEncoder != PNG_ENC)
            try {
                useEncoder(encoder);
            } catch (Exception e) {
                e.printStackTrace();
            }
        else
            setTransparent(false);

        encode(output, transparent);
    }

    public void encode(OutputStream output) {
        encode(output, transparent);
    }

    private void encode(OutputStream output, boolean transparent) {
        if (transparent) {
            LOGGER.debug("Image is transparent.");
            useEncoder = PNG_ENC;
        } else
            LOGGER.debug("Image is opague.");

        if (useEncoder == JPEG_ENC) {
            LOGGER.debug("MCRImgProcessor - encode");
            LOGGER.debug("JPEG_ENC");

            jpegEncode(image, output, jpegQuality);
        } else if (useEncoder == TIFF_ENC) {
            LOGGER.debug("MCRImgProcessor - encode");
            LOGGER.debug("TIFF_ENC");
            tiffEncode(image, output, true, tileWidth, tileHeight);
        } else if (useEncoder == PNG_ENC) {
            LOGGER.debug("MCRImgProcessor - encode");
            LOGGER.debug("PNG_ENC");
            // pngEncode(image, output, transparent);
            encodeIO(output, "png");
        }
    }

    private void jpegEncode(PlanarImage imgInput, OutputStream output, float encodeQuality) {
        // Encode JPEG
        JPEGEncodeParam jpegParam = new JPEGEncodeParam();
        jpegParam.setQuality(jpegQuality);
        ImageEncoder enc = ImageCodec.createImageEncoder("JPEG", output, jpegParam);
        try {
            enc.encode(imgInput);
            // output.close();
        } catch (Exception e) {
            // TODO change Exeption
            System.out.println("IOException at JPEG encoding..");
            e.printStackTrace();
        }
    }

    private void tiffEncode(PlanarImage imgInput, OutputStream output, boolean writeTiled, int tileWidth, int tileHeight) {
        // Encode TIFF
        TIFFEncodeParam tiffParam = new TIFFEncodeParam();

        if (writeTiled) {
            tiffParam.setTileSize(tileWidth, tileHeight);
            tiffParam.setWriteTiled(writeTiled);
        }

        ImageEncoder enc = ImageCodec.createImageEncoder("TIFF", output, tiffParam);
        try {
            enc.encode(imgInput);
            // output.close();
        } catch (Exception e) {
            // TODO change Exeption
            System.out.println("IOException at TIFF encoding..");
            e.printStackTrace();
        }
    }

    private void pngEncode(PlanarImage imgInput, OutputStream output, boolean transparent) {
        // Encode PNG
        PNGEncodeParam.RGB pngParam = new PNGEncodeParam.RGB();

        if (!transparent) {
            pngParam.unsetTransparency();
        }

        ImageEncoder enc = ImageCodec.createImageEncoder("png", output, pngParam);
        try {
            enc.encode(imgInput);
            // output.close();
        } catch (Exception e) {
            // TODO change Exeption
            System.out.println("IOException at PNG encoding..");
            e.printStackTrace();
        }
    }

    private RenderingHints noBorder() {
        BorderExtender extender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
        RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, extender);
        return hints;
    }

    public void setScaleFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public boolean isTransparent() {
        return transparent;
    }

    public void setTransparent(boolean transparent) {
        this.transparent = transparent;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }
}
