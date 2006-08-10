package org.mycore.services.imaging;

import java.awt.Dimension;
import java.io.InputStream;
import java.io.OutputStream;

public interface ImgProcessor {
	void resizeFitWidth(InputStream input, int newWidth, OutputStream output);
	void resizeFitHeight(InputStream input, int newHeight, OutputStream output);
	void resize(InputStream input, int newWidth, int newHeight, OutputStream output);
	void scale(InputStream input, float scaleFactor, OutputStream output);
	void scaleROI(InputStream input, int xTopPos, int yTopPos, int boundWidth, int boundHeight, float scaleFactor, OutputStream output);
	float getScaleFactor();
	float getJpegQuality();
	void setJpegQuality(float jpegQuality);
	void setTileSize(int tileWidth, int tileHeight);
	void useEncoder(int encoder) throws Exception;
	void encode(InputStream input, OutputStream output, int encoder) throws Exception;
	void createText(String text, int width, int height, OutputStream output);
	Dimension getOrigSize() throws Exception;
	Dimension getCurrentSize() throws Exception;
	Dimension getImageSize(InputStream input);
	//void setWatermark(boolean set);
	//void useImgAsWatermark();
	
	// separated Methods
	void loadImage(InputStream input);
	boolean hasCorrectTileSize();
	void resizeFitWidth(int newWidth);
	void resizeFitHeight(int newHeight);
	void resize(int newWidth, int newHeight);
	void scale(float scaleFactor);
	void scaleROI(int xTopPos, int yTopPos, int boundWidth, int boundHeight, float scaleFactor);
	void jpegEncode(OutputStream output);
	void tiffEncode(OutputStream output);
}
