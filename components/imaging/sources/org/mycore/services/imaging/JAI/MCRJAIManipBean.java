package org.mycore.services.imaging.JAI;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

import org.apache.log4j.Logger;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codec.MemoryCacheSeekableStream;

public class MCRJAIManipBean {
	private static Logger LOGGER = Logger.getLogger(MCRJAIManipBean.class);
	
	protected MCRJAIOperationList imgManipList = new MCRJAIOperationList();
	
	public void addManipOp(MCRJAIImageOp imgOp){
	    imgManipList.addOp(imgOp);
	}
	
	public void manipAndPost(InputStream imgStream, OutputStream out) throws IOException{
	    PlanarImage image = readAsStreamInRAM(imgStream);
	    PlanarImage outImage = imgManipList.executeOp(image);
	    saveAsJPEG(outImage, out);
	}
	
	public void saveAsJPEG(RenderedImage image, OutputStream out) throws IOException {
	    JPEGEncodeParam param = new JPEGEncodeParam();
	    ImageEncoder encoder = ImageCodec.createImageEncoder("JPEG", out, param);
	    encoder.encode(image);
	    out.close();
	}
	
	public PlanarImage readAsStreamInRAM(InputStream input) {
	    MemoryCacheSeekableStream stream = new MemoryCacheSeekableStream(input);
	    return JAI.create("stream", stream);
	}
}
