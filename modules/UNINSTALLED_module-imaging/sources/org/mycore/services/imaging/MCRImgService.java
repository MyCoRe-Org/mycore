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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileNodeServlet;

public class MCRImgService{
	
	
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

	// fit to Width x Heigth, even Thumbnail
	public void getImage(MCRFile image, int newWidth, int newHeight, OutputStream output) throws IOException{
		ImgProcessor processor = new MCRImgProcessor();
		
		if (USE_CACHE = true){
			CacheManager cache = new MCRImgCacheManager();
			MCRConfiguration config = MCRConfiguration.instance();
			InputStream input = null;
			
	    	int thumbWidth = Integer.parseInt(config.getString("MCR.Module-iview.thumbnail.size.width"));
	    	int thumbHeight = Integer.parseInt(config.getString("MCR.Module-iview.thumbnail.size.height"));
	    	
			if (cache.existInCache(image)){
				if (newWidth == thumbWidth && newHeight == thumbHeight)
		    		cache.getImage(image, MCRImgCacheManager.THUMB, output);
		    	else if (newWidth <= 1024 && newHeight <= 768){
		    		//	get the small cached version
		    		input = cache.getImageAsInputStream(image, MCRImgCacheManager.CACHE);
		    		
		    		processor.resize(input, newWidth, newHeight, output);
		    	}
		    	else{
		    		processor.resize(image.getContentAsInputStream(), newWidth, newHeight, output);
		    	}
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get image " + image.getName() + " from Cache!");
				LOGGER.debug("*********************************************");
			}
			else{
				processor.resize(image.getContentAsInputStream(), newWidth, newHeight, output);
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get image " + image.getName() + " from Processor!");
				LOGGER.debug("*********************************************");
			}
		}
		else
			processor.resize(image.getContentAsInputStream(), newWidth, newHeight, output);
			
		scaleFactor = processor.getScaleFactor();
	}
	
	public void getImage(MCRFile image, int xTopPos, int yTopPos, int boundWidth, int boundHeight, float scaleFactor, OutputStream output) throws IOException{
		ImgProcessor processor = new MCRImgProcessor();
		
		if (USE_CACHE = true){
			CacheManager cache = new MCRImgCacheManager();
			InputStream input = null;
			
			int origWidth = cache.getImgWidth(image);
			int origHeight = cache.getImgHeight(image);
			
			int resWidth = (int)(scaleFactor * origWidth);
			int resHeight = (int)(scaleFactor * origHeight);
			
			Stopwatch timer = new Stopwatch();
			
			if (cache.existInCache(image)){
				
		    	if (resWidth <= 1024 && resHeight <= 768){
		    		// get the small cached version
		    		timer.reset();
		    		timer.start();
		    		input = cache.getImageAsInputStream(image, MCRImgCacheManager.CACHE);
		    		timer.stop();
		    		LOGGER.info("getAsInput: " + timer.getElapsedTime());
		    		
		    		processor.scaleROI(input, xTopPos, yTopPos, boundWidth, boundHeight, scaleFactor, output);
		    	}
		    	else if (resWidth <= origWidth && resHeight <= origHeight){
		    		// get the orig cached version
		    		timer.reset();
		    		timer.start();
		    		input = cache.getImageAsInputStream(image, MCRImgCacheManager.ORIG);
		    		timer.stop();
		    		LOGGER.info("getAsInput: " + timer.getElapsedTime());
		    		
		    		processor.scaleROI(input, xTopPos, yTopPos, boundWidth, boundHeight, scaleFactor, output);
		    	}
		    	else{
		    		processor.scaleROI(image.getContentAsInputStream(), xTopPos, yTopPos, boundWidth, boundHeight, scaleFactor, output);
		    	}
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get image " + image.getName() + " from Processor!");
				LOGGER.debug("*********************************************");
			}
			else{
				processor.scaleROI(image.getContentAsInputStream(), xTopPos, yTopPos, boundWidth, boundHeight, scaleFactor, output);
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get image " + image.getName() + " from Processor!");
				LOGGER.debug("*********************************************");
			}
		}
		
		this.scaleFactor = processor.getScaleFactor();
	}
	
	public void getImage(MCRFile image, int newWidth, OutputStream output) throws IOException{
		ImgProcessor processor = new MCRImgProcessor();
		
		if (USE_CACHE = true){
			CacheManager cache = new MCRImgCacheManager();
			InputStream input = null;
			
			int origWidth = cache.getImgWidth(image);
			
			if (cache.existInCache(image)){
				
		    	if (newWidth <= 1024){
		    		// get the small cached version
		    		input = cache.getImageAsInputStream(image, MCRImgCacheManager.CACHE);
		    		
		    		processor.resizeFitWidth(input, newWidth, output);
		    	}
		    	else if (newWidth <= origWidth){
		    		// get the orig cached version
		    		input = cache.getImageAsInputStream(image, MCRImgCacheManager.ORIG);
		    		
		    		processor.resizeFitWidth(input, newWidth, output);
		    	}
		    	else{
		    		processor.resizeFitWidth(image.getContentAsInputStream(), newWidth, output);
		    	}
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get image " + image.getName() + " from Processor!");
				LOGGER.debug("*********************************************");
			}
			else{
				processor.resizeFitWidth(image.getContentAsInputStream(), newWidth, output);
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get image " + image.getName() + " from Processor!");
				LOGGER.debug("*********************************************");
			}
		}
	}
	
	

	public float getScaleFactor() {
		return scaleFactor;
	}

}