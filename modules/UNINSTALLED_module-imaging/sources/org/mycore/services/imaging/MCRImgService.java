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

public class MCRImgService {

	public final int THUMBNAIL = 0;

	public final int IMAGE = 1;

	protected float scaleFactor = 0.0F;

	protected boolean USE_CACHE = false;

	protected int format = -1;

	private Stopwatch timer = new Stopwatch();

	public MCRImgService(){
		MCRConfiguration config = MCRConfiguration.instance();
		USE_CACHE = (new Boolean(config.getString("MCR.Module-iview.useCache"))).booleanValue();
		LOGGER.debug("*********************************************");
		LOGGER.debug("* MCRImgService use Cache? " + USE_CACHE);
		LOGGER.debug("* MCRImgService use Cache Prop? " + config.getString("MCR.Module-iview.useCache"));
		LOGGER.debug("*********************************************");
	}
	
	private static Logger LOGGER = Logger.getLogger(MCRFileNodeServlet.class.getName());

	// Image getter methods

	// fit to Width x Heigth, even Thumbnail
	public void getImage(MCRFile image, int newWidth, int newHeight, OutputStream output) throws IOException {
		ImgProcessor processor = new MCRImgProcessor();
		
		boolean outputFilled = false;

		if (USE_CACHE) {
			LOGGER.debug("*********************************************");
			LOGGER.debug("* Get image Width x Height - use Cache");
			LOGGER.debug("*********************************************");

			CacheManager cache = new MCRImgCacheManager();
			MCRConfiguration config = MCRConfiguration.instance();
			InputStream input = null;
			
			int thumbWidth = Integer.parseInt(config.getString("MCR.Module-iview.thumbnail.size.width"));
			int thumbHeight = Integer.parseInt(config.getString("MCR.Module-iview.thumbnail.size.height"));
			
			int cacheWidth = Integer.parseInt(config.getString("MCR.Module-iview.cache.size.width"));
			int cacheHeight = Integer.parseInt(config.getString("MCR.Module-iview.cache.size.height"));
			
			if (cache.isLocked(image)) {
				LOGGER.debug("****************************************");
				LOGGER.debug("* Create Lock Message!");
				LOGGER.debug("****************************************");
				processor.createText("Cache wird generiert", newWidth, newHeight, output);
				outputFilled = true;
			}
			else if (newWidth == thumbWidth && newHeight == thumbHeight && cache.existInCache(image, MCRImgCacheManager.THUMB)) {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Thumbnail from ImgCache for " + image.getName());
				LOGGER.debug("*********************************************");
				
				cache.getImage(image, MCRImgCacheManager.THUMB, output);
				outputFilled = true;
			} else if ((newWidth <= cacheWidth || newHeight <= cacheHeight) && cache.existInCache(image, MCRImgCacheManager.CACHE)) {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Cache from ImgCache for " + image.getName());
				LOGGER.debug("*********************************************");

				// get the small cached version
				input = cache.getImageAsInputStream(image, MCRImgCacheManager.CACHE);
			} else if (cache.existInCache(image, MCRImgCacheManager.ORIG)) {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Orig from ImgCache for " + image.getName());
				LOGGER.debug("*********************************************");

				input = cache.getImageAsInputStream(image, MCRImgCacheManager.ORIG);

			} else {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Orig from IFS for " + image.getName());
				LOGGER.debug("*********************************************");
				input = image.getContentAsInputStream();
			}
			
			if (!outputFilled) {
				processor.resize(input, newWidth, newHeight, output);
			}
		} else {
			LOGGER.debug("*********************************************");
			LOGGER.debug("* Get image Width x Height - use Processor");
			LOGGER.debug("*********************************************");
			processor.resize(image.getContentAsInputStream(), newWidth, newHeight, output);
		}

		scaleFactor = processor.getScaleFactor();
	}
	
	// fitToWidth
	public void getImage(MCRFile image, int xTopPos, int yTopPos, int boundWidth, int boundHeight, OutputStream output) throws IOException {
		ImgProcessor processor = new MCRImgProcessor();
		
		float scaleHelp = 1;
		LOGGER.debug("*********************************************");
		LOGGER.debug("* Get image ROI fit width!!!!");
		LOGGER.debug("*********************************************");
		
		if (USE_CACHE) {
			LOGGER.debug("*********************************************");
			LOGGER.debug("* Get image ROI fit width else - use Cache");
			LOGGER.debug("*********************************************");
			CacheManager cache = new MCRImgCacheManager();
			MCRConfiguration config = MCRConfiguration.instance();
			InputStream input = null;
			boolean outputFilled = false;
			
			
			int cacheWidth = Integer.parseInt(config.getString("MCR.Module-iview.cache.size.width"));
			int cacheHeight = Integer.parseInt(config.getString("MCR.Module-iview.cache.size.height"));
			
			if (cache.isLocked(image)) {
				LOGGER.debug("****************************************");
				LOGGER.debug("* Create Lock Message!");
				LOGGER.debug("****************************************");
				processor.createText("Cache wird generiert", boundWidth, boundHeight, output);
				outputFilled = true;
			}
			else if ((boundWidth <= cacheWidth && boundHeight <= cacheHeight) && cache.existInCache(image, MCRImgCacheManager.CACHE)) {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Cache from ImgCache for " + image.getName());
				LOGGER.debug("*********************************************");
				
				int origW = cache.getImgWidth(image);
				scaleHelp = (float)cacheWidth / (float)origW;
				
				input = cache.getImageAsInputStream(image, MCRImgCacheManager.CACHE);
			} else if (cache.existInCache(image, MCRImgCacheManager.ORIG)) {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Orig from ImgCache for " + image.getName());
				LOGGER.debug("*********************************************");

				// get the orig cached version
				input = cache.getImageAsInputStream(image, MCRImgCacheManager.ORIG);
			} else {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Orig from IFS for " + image.getName());
				LOGGER.debug("*********************************************");
				
				input = image.getContentAsInputStream();
			}
			if (!outputFilled)
				processor.scaleROI(input, xTopPos, yTopPos, boundWidth, boundHeight, MCRImgProcessor.FIT_WIDTH, output);
		} else {
			processor.scaleROI(image.getContentAsInputStream(), xTopPos, yTopPos, boundWidth, boundHeight, MCRImgProcessor.FIT_WIDTH, output);
		}
		
		this.scaleFactor = processor.getScaleFactor() * scaleHelp;
	}
	
	public void getImage(MCRFile image, int xTopPos, int yTopPos, int boundWidth, int boundHeight, float scaleFactor, OutputStream output) throws IOException {
		ImgProcessor processor = new MCRImgProcessor();

		LOGGER.debug("*********************************************");
		LOGGER.debug("* Get image ROI!!!!");
		LOGGER.debug("*********************************************");
		this.scaleFactor = scaleFactor;
		
		if (USE_CACHE) {
			LOGGER.debug("*********************************************");
			LOGGER.debug("* Get image ROI else - use Cache");
			LOGGER.debug("*********************************************");
			CacheManager cache = new MCRImgCacheManager();
			MCRConfiguration config = MCRConfiguration.instance();
			InputStream input = null;
			boolean outputFilled = false;

			int origWidth = cache.getImgWidth(image);
			int origHeight = cache.getImgHeight(image);

			int resWidth = (int) (scaleFactor * origWidth);
			int resHeight = (int) (scaleFactor * origHeight);
			
			int cacheWidth = Integer.parseInt(config.getString("MCR.Module-iview.cache.size.width"));
			int cacheHeight = Integer.parseInt(config.getString("MCR.Module-iview.cache.size.height"));
			
			if (cache.isLocked(image)) {
				LOGGER.debug("****************************************");
				LOGGER.debug("* Create Lock Message!");
				LOGGER.debug("****************************************");
				processor.createText("Cache wird generiert", boundWidth, boundHeight, output);
				outputFilled = true;
			}
			else if ((resWidth <= cacheWidth && resHeight <= cacheHeight) && cache.existInCache(image, MCRImgCacheManager.CACHE)) {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Cache from ImgCache for " + image.getName());
				LOGGER.debug("*********************************************");
				
				float scaleHelpW = (float)cacheWidth / (float)origWidth;
				float scaleHelpH = (float)cacheHeight / (float)origHeight;
				
				float scaleHelp = (scaleHelpW > scaleHelpH) ? scaleHelpH : scaleHelpW;
				
				scaleFactor = scaleFactor / scaleHelp;
				// get the small cached version
				input = cache.getImageAsInputStream(image, MCRImgCacheManager.CACHE);
			} else if (cache.existInCache(image, MCRImgCacheManager.ORIG)) {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Orig from ImgCache for " + image.getName());
				LOGGER.debug("*********************************************");

				// get the orig cached version
				input = cache.getImageAsInputStream(image, MCRImgCacheManager.ORIG);
			} else {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Orig from IFS for " + image.getName());
				LOGGER.debug("*********************************************");

				input = image.getContentAsInputStream();
			}
			if (!outputFilled)
				processor.scaleROI(input, xTopPos, yTopPos, boundWidth, boundHeight, scaleFactor, output);
		} else {
			processor.scaleROI(image.getContentAsInputStream(), xTopPos, yTopPos, boundWidth, boundHeight, scaleFactor, output);
		}

		 this.scaleFactor = processor.getScaleFactor();
		
	}

	public void getImage(MCRFile image, int newWidth, OutputStream output) throws IOException {
		ImgProcessor processor = new MCRImgProcessor();

		LOGGER.debug("*********************************************");
		LOGGER.debug("* Get image Width!!!!");
		LOGGER.debug("*********************************************");

		if (USE_CACHE) {
			CacheManager cache = new MCRImgCacheManager();
			MCRConfiguration config = MCRConfiguration.instance();
			InputStream input = null;
			boolean outputFilled = false;
			
			int cacheWidth = Integer.parseInt(config.getString("MCR.Module-iview.cache.size.width"));
			int cacheHeight = Integer.parseInt(config.getString("MCR.Module-iview.cache.size.height"));
			
			LOGGER.debug("*********************************************");
			LOGGER.debug("* Cache width: " + cacheWidth);
			LOGGER.debug("* Cache height: " + cacheHeight);
			LOGGER.debug("*********************************************");

			if (cache.isLocked(image)) {
				LOGGER.debug("****************************************");
				LOGGER.debug("* Create Lock Message!");
				LOGGER.debug("****************************************");
				processor.createText("Cache wird generiert", newWidth, newWidth, output);
				outputFilled = true;
			}
			else if (newWidth <= cacheWidth && cache.existInCache(image, MCRImgCacheManager.CACHE)) {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Cache from ImgCache for " + image.getName());
				LOGGER.debug("*********************************************");
				
				// get the small cached version
				input = cache.getImageAsInputStream(image, MCRImgCacheManager.CACHE);
			} 
			else if (cache.existInCache(image, MCRImgCacheManager.ORIG)) {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Orig from ImgCache for " + image.getName());
				LOGGER.debug("*********************************************");
				
				// get the orig cached version
				input = cache.getImageAsInputStream(image, MCRImgCacheManager.ORIG);
			} 
			else {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Orig from IFS for " + image.getName());
				LOGGER.debug("*********************************************");
				
				input = image.getContentAsInputStream();
			}
			if (!outputFilled)
				processor.resizeFitWidth(input, newWidth, output);
		}
		else{
			processor.resizeFitWidth(image.getContentAsInputStream(), newWidth, output);
		}
		scaleFactor = processor.getScaleFactor();
	}

	public float getScaleFactor() {
		return scaleFactor;
	}

}
