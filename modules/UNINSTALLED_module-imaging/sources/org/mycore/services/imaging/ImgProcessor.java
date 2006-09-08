package org.mycore.services.imaging;

import java.awt.Dimension;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An image processor (short ImgProcesor) represent an object with the ability to manipulate image data. The image data is passed as InputStream.
 * The edited image data is encoded at least in JPEG or TIFF and passed as OutputStream.
 * There are two modes how the processor work.
 * The first mode is when you want to perform only one image operation. So you can use the methods which has input and output parameter.
 * The second mode is when you want to perform several image operation on the same image like "scale, encode, scale, encode,...". 
 * So first you have to load the image into the processor and then you can perform any image operation.
 * @author chi
 *
 */
public interface ImgProcessor {
	/**
	 * Scales the image to the new width in due proportion and encode it depending on the encoder setting 
	 * of the ImgProcessor. Default encoder is JPEG.
	 * </br>
	 * @param input the image data as InputStream
	 * @param newWidth the new width
	 * @param output the scaled and encoded image as OutputStream
	 */
	void resizeFitWidth(InputStream input, int newWidth, OutputStream output);
	
	
	/**
	 * Scales the image to the new height and encode it depending on the encoder setting 
	 * of the ImgProcessor. Default encoder is JPEG.
	 * </br>
	 * @param input the image data as InputStream
	 * @param newHeight the new height
	 * @param output the scaled and encoded image as OutputStream
	 */
	void resizeFitHeight(InputStream input, int newHeight, OutputStream output);
	
	
	/**
	 * Scales the image to fit into the new width and height in due proportion and encode it depending on the encoder setting 
	 * of the ImgProcessor. Default encoder is JPEG.
	 * </br>
	 * @param input the image data as InputStream
	 * @param newWidth the new width to fit in
	 * @param newHeight the new height to fit in
	 * @param output the scaled and encoded image as OutputStream
	 */
	void resize(InputStream input, int newWidth, int newHeight, OutputStream output);
	
	
	/**
	 * Scales the image with the given scale factor in due proportion and encode it depending on the encoder setting 
	 * of the ImgProcessor. Default encoder is JPEG.
	 * </br>
	 * @param input the image data as InputStream
	 * @param scaleFactor the scale factor for width and height
	 * @param output the scaled and encoded image as OutputStream
	 */
	void scale(InputStream input, float scaleFactor, OutputStream output);
	
	
	/**
	 * Scales the image with the given scale factor in due proportion cut off a region of interest and encode it depending on the encoder setting 
	 * of the ImgProcessor. Default encoder is JPEG.
	 * </br>
	 * @param input the image data as InputStream
	 * @param xTopPos the x-coordinate of the top left position of the region of interest
	 * @param yTopPos the y-coordinate of the top left position of the region of interest
	 * @param boundWidth the width of the region of interest
	 * @param boundHeight the height of the region of interest
	 * @param scaleFactor the scale factor for width and height of the image
	 * @param output the scaled and encoded region of interest as OutputStream
	 */
	void scaleROI(InputStream input, int xTopPos, int yTopPos, int boundWidth, int boundHeight, float scaleFactor, OutputStream output);
	
	
	/**
	 * Returns the scale factor used for the accomplished scale operation.
	 * </br>
	 * @return float value of the scale factor
	 */
	float getScaleFactor();
	
	
	/**
	 * Return the JPEG quality setting of the image processor.
	 * @return float value of the JPEG quality
	 * </br>
	 */
	float getJpegQuality();
	
	
	/**
	 * Set the JPEG quality of the image processor.
	 * </br>
	 * @param jpegQuality the float value of the JPEG quality
	 */
	void setJpegQuality(float jpegQuality);
	
	
	/**
	 * Set the tile width and height for a TIFF image, which is used in the encoding process.
	 * </br>
	 * @param tileWidth
	 * @param tileHeight
	 */
	void setTileSize(int tileWidth, int tileHeight);
	
	
	/**
	 * Set which encoder to use in the encoding process.
	 * </br>
	 * @param encoder the encoder should be a static field variable of the impleming class e.g. ImplementedClass.JPEG
	 * @throws Exception if you enter a number which is not in the encoder list
	 */
	void useEncoder(int encoder) throws Exception;
	
	
	/**
	 * Change the format of an image.
	 * @param input the image data as InputStream
	 * @param output the image in a new format
	 * @param encoder the encoder should be a static field variable of the impleming class e.g. ImplementedClass.JPEG
	 * @throws Exception if you enter a number which is not in the encoder list
	 */
	void encode(InputStream input, OutputStream output, int encoder) throws Exception;
	
	
	/**
	 * Returns the original image size of the image currently loades in the processor.
	 * @return Dimension of the image
	 * @throws Exception if no image is loaded in the processor
	 */
	Dimension getOrigSize() throws Exception;
	
	
	/**
	 * Returns the size of the edited image. The current size change after every scale operation.
	 * @return Dimension of the edited image
	 * @throws Exception if no image is loaded in the processor
	 */
	Dimension getCurrentSize() throws Exception;
	
	
	/**
	 * Returns the size of the image which is passed as InputStream.
	 * @param input the image data as InputStream
	 * @return Dimension of the image
	 */
	Dimension getImageSize(InputStream input);
	
	// separated Methods
	/**
	 * Loads an image into the processor.
	 * @param input the image data as InputStream
	 */
	void loadImage(InputStream input);
	
	
	/**
	 * Check if an image loaded into the processor has the correct tile size. The "correct" size is the size set in the processor. 
	 * Default is 480 x 480.
	 * @return true if the image has the correct tile size
	 * @throws Exception if no image is loaded in the processor
	 */
	boolean hasCorrectTileSize() throws Exception;
	
	
	/**
	 * Scales the image to the new width in due proportion.  An image should be loaded into the processor before using this method.
	 * </br>
	 * @param newWidth the new width
	 * @throws Exception if no image is loaded in the processor
	 */
	void resizeFitWidth(int newWidth) throws Exception;
	
	
	/**
	 * Scales the image to the new height in due proportion.  An image should be loaded into the processor before using this method.
	 * </br>
	 * @param newHeight the new height
	 * @throws Exception if no image is loaded in the processor
	 */
	void resizeFitHeight(int newHeight) throws Exception;
	
	
	/**
	 * Scales the image to fit into the new width and height in due proportion. An image should be loaded into the processor 
	 * before using this method.
	 * </br>
	 * @param newWidth new width to fit in
	 * @param newHeight new height to fit in
	 * @throws Exception if no image is loaded in the processor
	 */
	void resize(int newWidth, int newHeight) throws Exception;
	
	
	/**
	 * Scales the image with the given scale factor in due proportion.
	 * @param scaleFactor the scale factor for width and height of the image
	 * @throws Exception if no image is loaded in the processor
	 */
	void scale(float scaleFactor) throws Exception;
	
	/**
	 * Scales the image with the given scale factor in due proportion cut off a region of interest and encode it depending on the encoder setting 
	 * of the ImgProcessor. Default encoder is JPEG.
	 * </br>
	 * @param xTopPos the x-coordinate of the top left position of the region of interest
	 * @param yTopPos the y-coordinate of the top left position of the region of interest
	 * @param boundWidth the width of the region of interest
	 * @param boundHeight the height of the region of interest
	 * @param scaleFactor the scale factor for width and height of the image
	 * @throws Exception if no image is loaded in the processor
	 */
	void scaleROI(int xTopPos, int yTopPos, int boundWidth, int boundHeight, float scaleFactor) throws Exception;
	
	
	/**
	 * Encode the loaded image as a JPEG image to a given OutputStream.
	 * @param output the encoded image
	 * @throws Exception if no image is loaded in the processor
	 */
	void jpegEncode(OutputStream output) throws Exception;
	
	
	/**
	 * Encode the loaded image as a TIFF image to a given OutputStream.
	 * @param output the encodes image
	 * @throws Exception if no image is loaded in the processor
	 */
	void tiffEncode(OutputStream output) throws Exception;
}
