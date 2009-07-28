package org.mycore.services.imaging.JAI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.media.jai.PlanarImage;

import org.apache.log4j.Logger;
import org.mycore.services.imaging.JAI.imgOperation.MCRJAIImageOp;
import org.mycore.services.imaging.JAI.imgOperation.MCRJAIOperationList;

public class MCRJAIManipBean {
	private static Logger LOGGER = Logger.getLogger(MCRJAIManipBean.class);
	private MCRJAIEncoder encoder;
	private MCRJAIImageReader reader;
	
	protected MCRJAIOperationList imgManipList = new MCRJAIOperationList();
	
	public MCRJAIManipBean() {
	    this.reader = new MCRJAIImgMemReader();
        this.encoder = new MCRJAIJPEGEnc();
    }
	
	public MCRJAIManipBean(MCRJAIImageReader reader, MCRJAIEncoder encoder) {
	    this.reader = reader;
	    this.encoder = encoder;
	}
	
	public void addManipOp(MCRJAIImageOp imgOp){
	    imgManipList.addOp(imgOp);
	}
	
	public void manipAndPost(InputStream imgStream, OutputStream out) throws IOException{
	    PlanarImage image = reader.readImage(imgStream);
	    PlanarImage outImage = imgManipList.executeOp(image);
	    encoder.encode(outImage, out);
	}
}
