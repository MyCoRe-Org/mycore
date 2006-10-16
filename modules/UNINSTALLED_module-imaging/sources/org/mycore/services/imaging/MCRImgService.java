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

import java.awt.geom.Arc2D.Float;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.bcel.generic.IfInstruction;
import org.apache.log4j.Logger;
import org.jdom.JDOMException;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRFile;

public class MCRImgService {

	public final int THUMBNAIL = 0;

	public final int IMAGE = 1;

	protected float scaleFactor = 0.0F;

	protected boolean USE_CACHE = false;

	protected int format = -1;
	
	private static Logger LOGGER = Logger.getLogger(MCRImgService.class.getName());

	public MCRImgService() {
		MCRConfiguration config = MCRConfiguration.instance();
		USE_CACHE = (new Boolean(config.getString("MCR.Module-iview.useCache"))).booleanValue();
	}

	// Image getter methods

	// fit to Width x Heigth, even Thumbnail
	public void getImage(MCRFile image, int newWidth, int newHeight, OutputStream output) throws IOException {
		MCRConfiguration config = MCRConfiguration.instance();
		ImgProcessor processor = new MCRImgProcessor();
		float jpegQuality = java.lang.Float.parseFloat(config.getString("MCR.Module-iview.jpegQuality"));
		processor.setJpegQuality(jpegQuality);
		
		String filename = image.getName();

		boolean outputFilled = false;
		float scaleHelp = 1;

		try {
			if (image.getAdditionalData("ImageMetaData") == null) {
				LOGGER.debug("*********************************");
				LOGGER.debug("* MCRImgService create Add-Data *");
				LOGGER.debug("*********************************");
				MCRImgCacheCommands.cacheFile(image, true);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (USE_CACHE) {
			LOGGER.debug("*********************************************");
			LOGGER.debug("* Get " + filename + " Width x Height - use Cache");
			LOGGER.debug("*********************************************");

			CacheManager cache = new MCRImgCacheManager();
			
			InputStream input = null;

			int thumbWidth = Integer.parseInt(config.getString("MCR.Module-iview.thumbnail.size.width"));
			int thumbHeight = Integer.parseInt(config.getString("MCR.Module-iview.thumbnail.size.height"));

			int origWidth = cache.getImgWidth(image);
			int origHeight = cache.getImgHeight(image);

			int cacheWidth = Integer.parseInt(config.getString("MCR.Module-iview.cache.size.width"));
			int cacheHeight = Integer.parseInt(config.getString("MCR.Module-iview.cache.size.height"));

			float scaleHelpW = (float) cacheWidth / (float) origWidth;
			float scaleHelpH = (float) cacheHeight / (float) origHeight;

			if (scaleHelpW > scaleHelpH) {
				scaleHelp = scaleHelpH;
				cacheWidth = (int) (cacheWidth * scaleHelp);
			} else {
				scaleHelp = scaleHelpW;
				cacheHeight = (int) (cacheHeight * scaleHelp);
			}
			
			if ((newWidth == thumbWidth || newHeight == thumbHeight) && cache.existInCache(image, MCRImgCacheManager.THUMB)) {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Thumbnail from ImgCache for " + filename);
				LOGGER.debug("*********************************************");

				cache.getImage(image, MCRImgCacheManager.THUMB, output);
				outputFilled = true;
			} else if ((newWidth <= cacheWidth && newHeight <= cacheHeight) && cache.existInCache(image, MCRImgCacheManager.CACHE)) {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Cache from ImgCache for " + filename);
				LOGGER.debug("*********************************************");

				// scaleFactor = scaleFactor / scalefactor;

				// get the small cached version
				input = cache.getImageAsInputStream(image, MCRImgCacheManager.CACHE);
			} else if (cache.existInCache(image, MCRImgCacheManager.ORIG)) {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Orig from ImgCache for " + filename);
				LOGGER.debug("*********************************************");
				scaleHelp = 1;
				input = cache.getImageAsInputStream(image, MCRImgCacheManager.ORIG);

			} else {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Orig from IFS for " + filename);
				LOGGER.debug("*********************************************");
				input = image.getContentAsInputStream();
			}

			if (!outputFilled) {
				processor.resize(input, newWidth, newHeight, output);
			}
		} else {
			LOGGER.debug("*********************************************");
			LOGGER.debug("* Get " + filename + " Width x Height - use Processor");
			LOGGER.debug("*********************************************");
			processor.resize(image.getContentAsInputStream(), newWidth, newHeight, output);
		}

		scaleFactor = processor.getScaleFactor() * scaleHelp;
	}

	// fitToWidth
	public void getImage(MCRFile image, int xTopPos, int yTopPos, int boundWidth, int boundHeight, OutputStream output) throws IOException {
		CacheManager cache = new MCRImgCacheManager();
		int origWidth = cache.getImgWidth(image);

		getImage(image, xTopPos, yTopPos, boundWidth, boundHeight, (float) boundWidth / (float) origWidth, output);
	}

	public void getImage(MCRFile image, int xTopPos, int yTopPos, int boundWidth, int boundHeight, float scaleFactor, OutputStream output) throws IOException {
		MCRConfiguration config = MCRConfiguration.instance();
		ImgProcessor processor = new MCRImgProcessor();
		float jpegQuality = java.lang.Float.parseFloat(config.getString("MCR.Module-iview.jpegQuality"));
		processor.setJpegQuality(jpegQuality);

		LOGGER.debug("*********************************************");
		LOGGER.debug("* Get image ROI!!!!");
		LOGGER.debug("* ScaleFactor: " + scaleFactor);
		LOGGER.debug("*********************************************");
		this.scaleFactor = scaleFactor;

		try {
			if (image.getAdditionalData("ImageMetaData") == null) {
				LOGGER.debug("*********************************");
				LOGGER.debug("* MCRImgService create Add-Data *");
				LOGGER.debug("*********************************");
				MCRImgCacheCommands.cacheFile(image, true);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (USE_CACHE) {
			LOGGER.debug("*********************************************");
			LOGGER.debug("* Get image ROI else - use Cache");
			LOGGER.debug("*********************************************");
			CacheManager cache = new MCRImgCacheManager();
			InputStream input = null;
			boolean outputFilled = false;

			int origWidth = cache.getImgWidth(image);
			int origHeight = cache.getImgHeight(image);

			int resWidth = (int) (scaleFactor * origWidth);
			int resHeight = (int) (scaleFactor * origHeight);

			int cacheWidth = Integer.parseInt(config.getString("MCR.Module-iview.cache.size.width"));
			int cacheHeight = Integer.parseInt(config.getString("MCR.Module-iview.cache.size.height"));

			xTopPos = (int) (xTopPos * scaleFactor);
			yTopPos = (int) (yTopPos * scaleFactor);

			/*if (cache.isLocked(image)) {
				LOGGER.debug("****************************************");
				LOGGER.debug("* Create Lock Message!");
				LOGGER.debug("****************************************");
				processor.createText("Cache wird generiert", boundWidth, boundHeight, output);
				outputFilled = true;
			} else*/ if ((resWidth <= cacheWidth && resHeight <= cacheHeight) && cache.existInCache(image, MCRImgCacheManager.CACHE)) {
				LOGGER.debug("*********************************************");
				LOGGER.debug("* Get Cache from ImgCache for " + image.getName());
				LOGGER.debug("*********************************************");

				float scaleHelpW = (float) cacheWidth / (float) origWidth;
				float scaleHelpH = (float) cacheHeight / (float) origHeight;

				float scaleHelp = (scaleHelpW > scaleHelpH) ? scaleHelpH : scaleHelpW;

				scaleFactor = scaleFactor / scaleHelp;

				xTopPos = (int) (xTopPos / scaleHelp);
				yTopPos = (int) (yTopPos / scaleHelp);

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
			if (!outputFilled) {
				processor.scaleROI(input, xTopPos, yTopPos, boundWidth, boundHeight, scaleFactor, output);
			}
		} else {
			processor.scaleROI(image.getContentAsInputStream(), xTopPos, yTopPos, boundWidth, boundHeight, scaleFactor, output);
		}
	}

	public float getScaleFactor() {
		return scaleFactor;
	}

}
