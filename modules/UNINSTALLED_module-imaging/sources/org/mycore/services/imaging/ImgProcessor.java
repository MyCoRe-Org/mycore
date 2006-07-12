package org.mycore.services.imaging;

import java.awt.Dimension;
import java.io.InputStream;
import java.io.OutputStream;

public interface ImgProcessor {
	/**
	 * @param input
	 * @param newWidth
	 * @param output
	 */
	void resizeFitWidth(InputStream input, int newWidth, OutputStream output);
	
	/**
	 * @param input
	 * @param newHeight
	 * @param output
	 */
	void resizeFitHeight(InputStream input, int newHeight, OutputStream output);
	
	/**
	 * @param input
	 * @param newWidth
	 * @param newHeight
	 * @param output
	 */
	void resize(InputStream input, int newWidth, int newHeight, OutputStream output);
	
	/**
	 * @param input
	 * @param scaleFactor
	 * @param output
	 */
	void scale(InputStream input, float scaleFactor, OutputStream output);
	
	/**
	 * @param input
	 * @param xTopPos
	 * @param yTopPos
	 * @param boundWidth
	 * @param boundHeight
	 * @param scaleFactor
	 * @param output
	 */
	void scaleROI(InputStream input, int xTopPos, int yTopPos, int boundWidth, int boundHeight, float scaleFactor, OutputStream output);
	
	/**
	 * @return
	 */
	float getScaleFactor();
	
	/**
	 * @param jpegQuality
	 */
	void setJpegQuality(float jpegQuality);
	
	/**
	 * @param tileWidth
	 * @param tileHeight
	 */
	void setTileSize(int tileWidth, int tileHeight);
	
	/**
	 * @param encoder
	 */
	void useEncoder(int encoder);
	
	/**
	 * @param input
	 * @param output
	 * @param encoder
	 */
	void encode(InputStream input, OutputStream output, int encoder);
	
	/**
	 * @return
	 * @throws Exception
	 */
	Dimension getOrigSize() throws Exception;
	
	/**
	 * @return
	 * @throws Exception
	 */
	Dimension getCurrentSize() throws Exception;
	
	/**
	 * @param input
	 * @return
	 */
	Dimension getImageSize(InputStream input);
	//void setWatermark(boolean set);
	//void useImgAsWatermark();
}
