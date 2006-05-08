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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileNodeServlet;

public class MCRImageService extends Thread{
	public MCRImageService(){
		start();
	}
	
	public void run(){
		while ( ! isInterrupted() )
	    {
	      try
	      {
	        Thread.sleep( 1 );
	      }
	      catch ( InterruptedException e )
	      {
	      interrupt();
	    }
	  }
	}
	
	public final int THUMBNAIL = 0;
	public final int IMAGE = 1;
	
	protected float scaleFactor = 0.0F;
	protected boolean USE_CACHE = false;
	protected int format = -1;
	
	
	public void useCache(boolean useCache){
		USE_CACHE = useCache;
	}
	
	private static Logger LOGGER = Logger.getLogger(MCRFileNodeServlet.class.getName());
	
	// Image getter methods

	
	// fit Width
	public void getImage(MCRFile image, int fitWidth, OutputStream output){
		if (format == -1)
			format = IMAGE;
		
		getImage(image, fitWidth, 0, output);
	}
	
	// get thumbnails
	public void getThumbnail(MCRFile image, int thumbWidth, int thumbHeight, OutputStream output){
		LOGGER.debug("**************************************************");
		LOGGER.debug("Get thumbnail: " + thumbWidth + "x" + thumbHeight);
		LOGGER.debug("**************************************************");
		
		if (format == -1)
			format = THUMBNAIL;
		
		getImage(image, thumbWidth, thumbHeight, output);
		
	}
	
	// fitScreen (Width x Height)
	public void getImage(MCRFile image, int fitWidth, int fitHeight, OutputStream output){
		if (format == -1)
			format = IMAGE;
		Dimension size = new Dimension();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		try {
			if (getImageFromCacheTo(bos, image, fitWidth, fitHeight)){
				LOGGER.debug("**************************************************");
				LOGGER.debug("Get " + ((format == THUMBNAIL)? "Thumbnail": "Image") + " from cache successful.");
				LOGGER.debug("**************************************************");
			}
			else{
				MCRImageProcessor imageProcessor = new MCRImageProcessor(image.getContentAsInputStream());
				imageProcessor.scale(fitWidth,fitHeight);
				imageProcessor.jpegEncodeTo(bos, 0.5F);
				scaleFactor = imageProcessor.getScaleFactor();
				size = imageProcessor.getOrigSize();
				LOGGER.debug("**************************************************");
				LOGGER.debug("Get " + ((format == THUMBNAIL)? "Thumbnail": "Image") + " from processor successful.");
				LOGGER.debug("**************************************************");
				setInCacheFrom(bos, image, fitWidth, fitHeight);
				setSizeInCache(image, size);
			}
			bos.writeTo(output);
			output.close();
			bos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		LOGGER.debug("**************************************************");
		LOGGER.debug("Set " + ((format == THUMBNAIL)? "Thumbnail": "Image") + " in cache successful.");
		LOGGER.debug("**************************************************");
	}
	
	// use scale factor
	public void getImage(MCRFile image, float scaleFactor, OutputStream output)  {
		LOGGER.debug("**************************************************");
		LOGGER.debug("Get image with scale factor: " + scaleFactor);
		LOGGER.debug("**************************************************");
		
		if (format == -1)
			format = IMAGE;

		try {
			MCRImageProcessor imageProcessor = new MCRImageProcessor(image.getContentAsInputStream());
			imageProcessor.scale(scaleFactor);
		
			imageProcessor.jpegEncodeTo(output, 0.5F);
			output.close();
		} catch (java.io.IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.scaleFactor = scaleFactor;
	}
	
	public void setUseCache(boolean useCache){
	}

	public float getScaleFactor() {
		return scaleFactor;
	}

	protected void setInCacheFrom(ByteArrayOutputStream output, MCRFile origImage, int thumbWidth, int thumbHeight){
		LOGGER.debug("**************************************************");
		LOGGER.debug("setInCacheFrom - use cache? " + USE_CACHE);
		LOGGER.debug("**************************************************");
		if (USE_CACHE == true){
			LOGGER.debug("**************************************************");
			LOGGER.debug("Set " + ((format == THUMBNAIL)? "Thumbnail": "Image") + " in cache.");
			LOGGER.debug("**************************************************");
			
			MCRIFSConnector imageCache = new MCRIFSConnector();
			if (format == THUMBNAIL)
				imageCache.setThumbnail(origImage.getOwnerID(), origImage.getAbsolutePath(), thumbWidth, thumbHeight, 
						new ByteArrayInputStream(output.toByteArray()));
			/*else if (format == IMAGE)
				imageCache.setImage(origImage.getOwnerID(), origImage.getAbsolutePath(), thumbWidth, thumbHeight, output);*/
		}
	}
	
	protected void setSizeInCache(MCRFile origImage, Dimension size){
		new MCRIFSConnector().setimgSize(origImage.getOwnerID(), origImage.getAbsolutePath(), size);
	}
	
	protected boolean getImageFromCacheTo(ByteArrayOutputStream output, MCRFile origImage, int thumbWidth, int thumbHeight){
		if (USE_CACHE == true){
			LOGGER.debug("**************************************************");
			LOGGER.debug("Trying get " + ((format == THUMBNAIL)? "Thumbnail": "Image") + " from cache.");
			LOGGER.debug("**************************************************");
			
			MCRIFSConnector imageCache = new MCRIFSConnector();
			if (format == THUMBNAIL){
				return imageCache.getThumbnail(origImage.getOwnerID(), origImage.getAbsolutePath(), thumbWidth, thumbHeight, output);
			}
			else
				return false;
		}
		else
			return USE_CACHE;
	}
	
	protected boolean getImageFromCacheTo(ByteArrayOutputStream output, MCRFile origImage, float scaleFactor){
		if (USE_CACHE == true){
			LOGGER.debug("**************************************************");
			LOGGER.debug("Trying get " + ((format == THUMBNAIL)? "Thumbnail": "Image") + " from cache.");
			LOGGER.debug("**************************************************");
			
			MCRIFSConnector imageCache = new MCRIFSConnector();
			if (format == THUMBNAIL){
				return imageCache.getImage(origImage.getOwnerID(), origImage.getAbsolutePath(), scaleFactor, output);
			}
			else
				return false;
		}
		else
			return USE_CACHE;
	}
}