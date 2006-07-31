package org.mycore.services.imaging;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.media.jai.BorderExtender;
import javax.media.jai.InterpolationBicubic2;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

import org.apache.log4j.Logger;
import org.mycore.frontend.cli.MCRClassificationCommands;
import org.mycore.services.tilecachetool.TCTool;

import com.sun.media.jai.codec.FileCacheSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codec.MemoryCacheSeekableStream;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.TIFFEncodeParam;

public class MCRImgProcessor implements ImgProcessor {
	private static Logger LOGGER = Logger.getLogger(MCRClassificationCommands.class.getName());
	private PlanarImage image = null;

	protected float scaleFactor = 0;

	private float jpegQuality = 0.5F;

	private Dimension origSize = null;

	private int tileWidth = 480;

	private int tileHeight = 480;
	
	private int useEncoder = JPEG_ENC;

	static public final int JPEG_ENC = 0;

	static public final int TIFF_ENC = 1;
	
	static public final int FIT_WIDTH = 0;
	
	static public final int FIT_HEIGHT = 0;
	
//	static TCTool tctool = new TCTool();

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

	public void useEncoder(int Encoder) {
		useEncoder = Encoder;
	}

	public void resizeFitWidth(InputStream input, int newWidth, OutputStream output) {
		image = loadmageMEMCache(input);

		if (newWidth != origSize.width)
			image = fitWidth(image, newWidth);

		if (useEncoder == JPEG_ENC)
			jpegEncode(image, output, jpegQuality);
		else if (useEncoder == TIFF_ENC)
			tiffEncode(image, output, true, tileWidth, tileHeight);
	}

	public void resizeFitHeight(InputStream input, int newHeight, OutputStream output) {
		image = loadmageMEMCache(input);

		if (newHeight != origSize.height)
			image = fitHeight(image, newHeight);

		if (useEncoder == JPEG_ENC)
			jpegEncode(image, output, jpegQuality);
		else if (useEncoder == TIFF_ENC)
			tiffEncode(image, output, true, tileWidth, tileHeight);
	}

	public void resize(InputStream input, int newWidth, int newHeight, OutputStream output) {
		image = loadmageMEMCache(input);

		if (newWidth != origSize.width && newHeight != origSize.height)
			try {
				image = resizeImage(image, newWidth, newHeight, true);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		if (useEncoder == JPEG_ENC)
			jpegEncode(image, output, jpegQuality);
		else if (useEncoder == TIFF_ENC)
			tiffEncode(image, output, true, tileWidth, tileHeight);
	}

	public void scale(InputStream input, float scaleFactor, OutputStream output) {
		image = loadmageMEMCache(input);

		if (scaleFactor != 1)
			image = scaleImage(image, scaleFactor);

		if (useEncoder == JPEG_ENC)
			jpegEncode(image, output, jpegQuality);
		else if (useEncoder == TIFF_ENC)
			tiffEncode(image, output, true, tileWidth, tileHeight);
	}
	
	public void scaleROI(InputStream input, int xTopPos, int yTopPos, int boundWidth, int boundHeight, int fitDirection, OutputStream output) {
		image = loadmageMEMCache(input);
		
		if (fitDirection == FIT_WIDTH){
			scaleFactor = (float)boundWidth / (float)origSize.width;
		}
		else{
			scaleFactor = (float)boundHeight / (float)origSize.height;
		}
		
		scaleROI(input, xTopPos, yTopPos, boundWidth, boundHeight, scaleFactor, output);
	}
	
	public void scaleROI(InputStream input, int xTopPos, int yTopPos, int boundWidth, int boundHeight, float scaleFactor, OutputStream output) {
		if (image == null)
			image = loadmageMEMCache(input);
		
		LOGGER.debug("********************************");
		LOGGER.debug("* Loading Image succesfull!");
		LOGGER.debug("********************************");
		
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

		if (useEncoder == JPEG_ENC)
			jpegEncode(image, output, jpegQuality);
		else if (useEncoder == TIFF_ENC)
			tiffEncode(image, output, true, tileWidth, tileHeight);
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
		PlanarImage image = loadmageMEMCache(input);
		return new Dimension(image.getWidth(), image.getHeight());
	}

	public void encode(InputStream input, OutputStream output, int encoder) {
		useEncoder(encoder);
		resize(input, origSize.width, origSize.height, output);
	}

	//****************************************************************************
	
	public void loadImage(InputStream input) {
		image = loadmageMEMCache(input);
	}

	public boolean hasCorrectTileSize() {
		boolean hasCorrectSize = false;
		
		if (image.getNumXTiles() > 1 && image.getNumYTiles() >1 && image.getTileWidth() == 480 && image.getTileHeight() == 480)
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
				image = resizeImage(image, newWidth, newHeight, true);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	public void scale(float scaleFactor) {
		if (scaleFactor != 1)
			image = scaleImage(image, scaleFactor);
	}

	public void scaleROI(int xTopPos, int yTopPos, int boundWidth, int boundHeight, int fitDirection) {
		if (fitDirection == FIT_WIDTH){
			scaleFactor = (float)boundWidth / (float)origSize.width;
		}
		else{
			scaleFactor = (float)boundHeight / (float)origSize.height;
		}
		
		scaleROI(xTopPos, yTopPos, boundWidth, boundHeight, scaleFactor);
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

	public void tiffEncode(OutputStream output) {
		tiffEncode(image, output, true, tileWidth, tileHeight);
	}
	
	
	// End: Interface implementation

	/** ************************************************************************ */
	// Image operation using JAI
	public PlanarImage loadImageFileCache(InputStream input) {
		SeekableStream seekStream = SeekableStream.wrapInputStream(input, true);
		PlanarImage image = JAI.create("stream", seekStream);
		origSize.width = image.getWidth();
		origSize.height = image.getHeight();
		LOGGER.info("******************************************");
		LOGGER.info("* File loading successfull - FileCache");
		LOGGER.info("* origSize.width: " + origSize.width);
		LOGGER.info("* origSize.height: " + origSize.height);
		LOGGER.info("******************************************");
		this.image = image;
		return image;
	}
	
	private PlanarImage loadmageMEMCache(InputStream input) {
		PlanarImage image = JAI.create("stream", new MemoryCacheSeekableStream(input));
		origSize.width = image.getWidth();
		origSize.height = image.getHeight();
		LOGGER.info("******************************************");
		LOGGER.info("* File loading successfull - MEM");
		LOGGER.info("* origSize.width: " + origSize.width);
		LOGGER.info("* origSize.height: " + origSize.height);
		LOGGER.info("******************************************");
		return image;
	}

	private PlanarImage crop(PlanarImage img, Point topCorner, Dimension boundary) {
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

	private PlanarImage resizeImage(PlanarImage img, int newWidth, int newHeight, boolean keepProportion) throws Exception {
		if (newWidth <= 0 || newHeight <= 0)
			throw new Exception("newWidth and newHeight should be > 0!");

		int origWidth = img.getWidth();
		int origHeight = img.getHeight();
		float xScale = (float) newWidth / (float) origWidth;
		float yScale = (float) newHeight / (float) origHeight;

		if (keepProportion)
			if (yScale < xScale)
				xScale = yScale;
			else
				yScale = xScale;

		return scaleImage(img, xScale);
	}

	// Encoding Part
	private void jpegEncode(PlanarImage imgInput, OutputStream output, float encodeQuality) {
		// Encode JPEG
		JPEGEncodeParam jpegParam = new JPEGEncodeParam();
		jpegParam.setQuality(0.5F);
		ImageEncoder enc = ImageCodec.createImageEncoder("JPEG", output, jpegParam);
		try {
			enc.encode(imgInput);
			output.close();
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
			output.close();
		} catch (Exception e) {
			// TODO change Exeption
			System.out.println("IOException at TIFF encoding..");
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

	

}
