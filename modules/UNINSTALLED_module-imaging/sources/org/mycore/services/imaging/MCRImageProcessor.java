// ============================================== 
//  												
// Module-Imaging 1.0, 05-2006  		
// +++++++++++++++++++++++++++++++++++++			
//  												
// Andreas Trappe 	- idea, concept
// Chi Vu Huu		- concept, development
//
// $Revision$ $Date$ 
// ============================================== 

package org.mycore.services.imaging;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBicubic2;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codec.MemoryCacheSeekableStream;

public class MCRImageProcessor {
	private Stopwatch timer = new Stopwatch();
	protected RenderedOp image = null;
	protected Dimension origSize = null;
	protected float computedScaleFactor = 0;
	
	public MCRImageProcessor(InputStream input){
		image = initImage(input);
		origSize = getCurrentSize();
	}
	
	public void setImage(InputStream input){
		image = initImage(input);
	}
	
	public void zoomIn(InputStream input, float f, OutputStream output){
		RenderedOp imgInput = initImage(input);
		
		timer.reset();
		timer.start();
		imgInput = reformatImage(imgInput, new Dimension(imgInput.getWidth()/2,imgInput.getHeight()/2));
		timer.stop();
		System.out.println("reformatImage time: " + timer.getElapsedTime());
		
//		imgInput = imgInput.c
	}
	
	public Dimension getOrigSize(){
		return origSize;
	}
	
	public Dimension getCurrentSize(){
		return new Dimension(image.getWidth(), image.getHeight());
	}
	
	// Load Image from InputStream
	private RenderedOp initImage(InputStream input) {
		// Long version
		/*MemoryCacheSeekableStream seekableImage = new MemoryCacheSeekableStream(input);
		RenderedOp imgInput = JAI.create("stream",seekableImage);
		return imgInput;*/
		
		// Short Version
		return JAI.create("stream", new MemoryCacheSeekableStream(input));
	}
	
	public void scale(int fitWidth, int fitHeight){
		scale(computeScaleFactor(new Dimension(fitWidth, fitHeight)));
	}
	
	public void scale(float scaleFactor){
		image = scale(scaleFactor, scaleFactor, image);
	}
	
	private RenderedOp scale(float xScalefactor, float yScalefactor, RenderedOp imgInput) {
		// Scale Image
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(imgInput); // The source image
		pb.add(xScalefactor); // The xScale
		pb.add(yScalefactor); // The yScale
		pb.add(0.0F); // The x translation
		pb.add(0.0F); // The y translation
		pb.add(new InterpolationBicubic2(3)); // The interpolation

		// the scale operation with JAI
		return (RenderedOp)JAI.create("scale", pb, noBorder());
	}
	
	public void crop(Point middlePoint, Dimension bound){
		int widthHalf = bound.width/2;
		int heightHalf = bound.height/2;
		Point topCorner = new Point(middlePoint.x - widthHalf, middlePoint.y - heightHalf);
		
		if (topCorner.x <0)
			topCorner.x = middlePoint.x - topCorner.x - widthHalf;
		if (topCorner.y <0)
			topCorner.y = middlePoint.y - topCorner.y - heightHalf;
		
		if (middlePoint.x + widthHalf > origSize.width)
			topCorner.x = origSize.width - bound.width; 
		if (middlePoint.x + heightHalf > origSize.width)
			topCorner.y = origSize.height - bound.height; 
		
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(image);
		pb.add((float)topCorner.x); // The xScale
		pb.add((float)topCorner.y); // The yScale
		pb.add((float)bound.width); // The x translation
		pb.add((float)bound.height); // The y translation
		
		image = (RenderedOp)JAI.create("crop", pb, noBorder());
		
	}
	
	public void jpegEncodeTo(OutputStream output, float encodeQuality)  {
		jpegEncode(image, output, encodeQuality);
	}
	
	private void jpegEncode(RenderedOp imgInput, OutputStream output, float encodeQuality) {
		// Encode JPEG
		JPEGEncodeParam jpegParam = new JPEGEncodeParam();
		jpegParam.setQuality(0.5F);
		ImageEncoder enc = ImageCodec.createImageEncoder("JPEG", output, jpegParam);
		
		try {
			enc.encode(imgInput);
			output.close();
		} catch (Exception e) {
			//TODO change Exeption
			System.out.println("IOException at encoding..");
		}
	}
	
	private RenderingHints noBorder() {
		BorderExtender extender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
		RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,extender);
		return hints;
	}
	
	private RenderedOp reformatImage(RenderedOp img, Dimension tileDim) {
		int tileWidth = tileDim.width;
		int tileHeight = tileDim.height;
		ImageLayout tileLayout = new ImageLayout(img);
		tileLayout.setTileWidth(tileWidth);
		tileLayout.setTileHeight(tileHeight);

		HashMap map = new HashMap();
		map.put(JAI.KEY_IMAGE_LAYOUT, tileLayout);
		map.put(JAI.KEY_INTERPOLATION, Interpolation
				.getInstance(Interpolation.INTERP_BICUBIC));
		RenderingHints tileHints = new RenderingHints(map);

		ParameterBlock pb = new ParameterBlock();
		pb.addSource(img);
		
		timer.reset();
		timer.start();
		RenderedOp image = JAI.create("format", pb, tileHints);
		timer.stop();
		System.out.println("reformatImage inner time: " + timer.getElapsedTime());
		return image;
	}
	
	public float getScaleFactor(){
		return computedScaleFactor;
	}
	
	private float computeScaleFactor(Dimension size){
		if (size.height == 0)
			computedScaleFactor = (float)size.width/(float)origSize.width;
		else if (size.width == 0)
			computedScaleFactor = (float)size.height/(float)origSize.height;
		else {
			float s1 = (float)size.width/(float)origSize.width;
			float s2 = (float)size.height/(float)origSize.height;
			computedScaleFactor = (s1 < s2)? s1 : s2;
		}
					
		return computedScaleFactor;
	}
}
