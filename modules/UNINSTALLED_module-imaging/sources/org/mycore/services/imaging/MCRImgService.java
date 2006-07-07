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

	public void useCache(boolean useCache) {
		USE_CACHE = useCache;
	}

	private static Logger LOGGER = Logger.getLogger(MCRFileNodeServlet.class.getName());

	// Image getter methods

	// fit to Width x Heigth, even Thumbnail
	public void getImage(MCRFile image, int newWidth, int newHeight, OutputStream output) throws IOException {
		ImgProcessor processor = new MCRImgProcessor();
		
		boolean getThumb = false;

		if (USE_CACHE == true) {
			LOGGER.debug("*********************************************");
			LOGGER.debug("* Get image Width x Height - use Cache");
			LOGGER.debug("*********************************************");

			CacheManager cache = new MCRImgCacheManager();
			MCRConfiguration config = MCRConfiguration.instance();
			InputStream input = null;
			
			LOGGER.debug("*********************************************");
			LOGGER.debug("* Thumb: " + cache.existInCache(image, MCRImgCacheManager.THUMB));
			LOGGER.debug("* Cache: " + cache.existInCache(image, MCRImgCacheManager.CACHE));
			LOGGER.debug("* Orig: " + cache.existInCache(image, MCRImgCacheManager.ORIG));
			LOGGER.debug("*********************************************");

			int thumbWidth = Integer.parseInt(config.getString("MCR.Module-iview.thumbnail.size.width"));
			int thumbHeight = Integer.parseInt(config.getString("MCR.Module-iview.thumbnail.size.height"));

			if (newWidth == thumbWidth && newHeight == thumbHeight && cache.existInCache(image, MCRImgCacheManager.THUMB)) {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Thumbnail from ImgCache for " + image.getName());
				LOGGER.debug("*********************************************");
				getThumb = true;
				
				cache.getImage(image, MCRImgCacheManager.THUMB, output);
			} else if (newWidth <= 1024 && newHeight <= 768 && cache.existInCache(image, MCRImgCacheManager.CACHE)) {
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
			
			if (!getThumb)
				processor.resize(input, newWidth, newHeight, output);
		} else {
			LOGGER.debug("*********************************************");
			LOGGER.debug("* Get image Width x Height - use Processor");
			LOGGER.debug("*********************************************");
			processor.resize(image.getContentAsInputStream(), newWidth, newHeight, output);
		}

		scaleFactor = processor.getScaleFactor();
	}

	public void getImage(MCRFile image, int xTopPos, int yTopPos, int boundWidth, int boundHeight, float scaleFactor, OutputStream output) throws IOException {
		ImgProcessor processor = new MCRImgProcessor();

		LOGGER.debug("*********************************************");
		LOGGER.debug("* Get image ROI!!!!");
		LOGGER.debug("*********************************************");
		this.scaleFactor = scaleFactor;
		
		if (USE_CACHE == true) {
			LOGGER.debug("*********************************************");
			LOGGER.debug("* Get image ROI else - use Cache");
			LOGGER.debug("*********************************************");
			CacheManager cache = new MCRImgCacheManager();
			InputStream input = null;

			int origWidth = cache.getImgWidth(image);
			int origHeight = cache.getImgHeight(image);

			int resWidth = (int) (scaleFactor * origWidth);
			int resHeight = (int) (scaleFactor * origHeight);

			if (resWidth <= 1024 && resHeight <= 768 && cache.existInCache(image, MCRImgCacheManager.CACHE)) {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Cache from ImgCache for " + image.getName());
				LOGGER.debug("*********************************************");
				
				float scaleHelpW = (float)1024 / (float)origWidth;
				float scaleHelpH = (float)768 / (float)origHeight;
				
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

			processor.scaleROI(input, xTopPos, yTopPos, boundWidth, boundHeight, scaleFactor, output);
		} else {
			processor.scaleROI(image.getContentAsInputStream(), xTopPos, yTopPos, boundWidth, boundHeight, scaleFactor, output);
		}

		// this.scaleFactor = processor.getScaleFactor();
		
	}

	public void getImage(MCRFile image, int newWidth, OutputStream output) throws IOException {
		ImgProcessor processor = new MCRImgProcessor();

		LOGGER.debug("*********************************************");
		LOGGER.debug("* Get image Width!!!!");
		LOGGER.debug("*********************************************");

		if (USE_CACHE == true) {
			CacheManager cache = new MCRImgCacheManager();
			InputStream input = null;

			if (newWidth <= 1024 && cache.existInCache(image, MCRImgCacheManager.CACHE)) {
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