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
                LOGGER.debug("MCRImgService create Add-Data");
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
            LOGGER.debug("Get " + filename + " Width x Height - use Cache");

            CacheManager cache = MCRImgCacheManager.instance();

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
                LOGGER.debug("Get Thumbnail from ImgCache for " + filename);

                cache.getImage(image, MCRImgCacheManager.THUMB, output);
                outputFilled = true;
                // newWidth <= cacheWidth && newHeight <= cacheHeight
            } else if ((newWidth <= cacheWidth) && cache.existInCache(image, MCRImgCacheManager.CACHE)) {
                LOGGER.debug("Get Cache from ImgCache for " + filename);

                // scaleFactor = scaleFactor / scalefactor;

                // get the small cached version
                input = cache.getImageAsInputStream(image, MCRImgCacheManager.CACHE);
            } else if (cache.existInCache(image, MCRImgCacheManager.ORIG)) {
                LOGGER.debug("Get Orig from ImgCache for " + filename);
                scaleHelp = 1;
                input = cache.getImageAsInputStream(image, MCRImgCacheManager.ORIG);

            } else {
                LOGGER.debug("Get Orig from IFS for " + filename);
                input = image.getContentAsInputStream();
            }

            if (!outputFilled) {
                processor.resize(input, newWidth, newHeight, output);
                input.close();
            }
        } else {
            LOGGER.debug("Get " + filename + " Width x Height - use Processor");
            InputStream input = image.getContentAsInputStream();
            processor.resize(input, newWidth, newHeight, output);
            input.close();
        }

        output.close();
        scaleFactor = processor.getScaleFactor() * scaleHelp;
    }

    // fitToWidth
    public void getImage(MCRFile image, int xTopPos, int yTopPos, int boundWidth, int boundHeight, OutputStream output) throws IOException {
        // CacheManager cache = new MCRImgCacheManager();
        CacheManager cache = MCRImgCacheManager.instance();
        int origWidth = cache.getImgWidth(image);
        LOGGER.debug("getImage - fitToWidth # xTopPos: " + xTopPos);
        LOGGER.debug("getImage - fitToWidth # yTopPos: " + yTopPos);
        LOGGER.debug("getImage - fitToWidth # boundWidth: " + boundWidth);
        LOGGER.debug("getImage - fitToWidth # boundHeight: " + boundHeight);

        getImage(image, xTopPos, yTopPos, boundWidth, boundHeight, (float) boundWidth / (float) origWidth, output);
    }

    public void getImage(MCRFile image, int xTopPos, int yTopPos, int boundWidth, int boundHeight, float scaleFactor, OutputStream output) throws IOException {
        MCRConfiguration config = MCRConfiguration.instance();
        ImgProcessor processor = new MCRImgProcessor();
        float jpegQuality = java.lang.Float.parseFloat(config.getString("MCR.Module-iview.jpegQuality"));
        processor.setJpegQuality(jpegQuality);

        LOGGER.debug("getImage - ROI # scaleFactor: " + scaleFactor);
        LOGGER.debug("getImage - ROI # xTopPos: " + xTopPos);
        LOGGER.debug("getImage - ROI # yTopPos: " + yTopPos);
        LOGGER.debug("getImage - ROI # boundWidth: " + boundWidth);
        LOGGER.debug("getImage - ROI # boundHeight: " + boundHeight);

        this.scaleFactor = scaleFactor;

        try {
            if (image.getAdditionalData("ImageMetaData") == null) {
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
            LOGGER.debug("getImage - ROI using 'if (USE_CACHE)'");
            CacheManager cache = MCRImgCacheManager.instance();
            InputStream input = null;

            int origWidth = cache.getImgWidth(image);
            int origHeight = cache.getImgHeight(image);

            LOGGER.debug("getImage - ROI # OrigWidth: " + origWidth);
            LOGGER.debug("getImage - ROI # OrigHeight: " + origHeight);

            int resWidth = (int) (scaleFactor * origWidth);
            int resHeight = (int) (scaleFactor * origHeight);

            int cacheWidth = Integer.parseInt(config.getString("MCR.Module-iview.cache.size.width"));
            int cacheHeight = Integer.parseInt(config.getString("MCR.Module-iview.cache.size.height"));

            xTopPos = (int) (xTopPos * scaleFactor);
            yTopPos = (int) (yTopPos * scaleFactor);

            if ((resWidth <= cacheWidth) && cache.existInCache(image, MCRImgCacheManager.CACHE)) {
                LOGGER.debug("getImage - ROI # get Cache version");

                float scaleHelp = (float) cacheWidth / (float) origWidth;

                scaleFactor = scaleFactor / scaleHelp;

                xTopPos = (int) (xTopPos / scaleHelp);
                yTopPos = (int) (yTopPos / scaleHelp);

                // get the small cached version
                input = cache.getImageAsInputStream(image, MCRImgCacheManager.CACHE);
            } else if (cache.existInCache(image, MCRImgCacheManager.ORIG)) {
                LOGGER.debug("getImage - ROI # get orig version from Cache");

                // get the orig cached version
                input = cache.getImageAsInputStream(image, MCRImgCacheManager.ORIG);
            } else {
                LOGGER.debug("getImage - ROI # get orig version from IFS");

                // get the orig version from IFS
                input = image.getContentAsInputStream();
            }
            LOGGER.debug("getImage - ROI # edited scaleFactor: " + scaleFactor);
            LOGGER.debug("getImage - ROI # edited xTopPos: " + xTopPos);
            LOGGER.debug("getImage - ROI # edited yTopPos: " + yTopPos);
            LOGGER.debug("getImage - ROI # edited boundWidth: " + boundWidth);
            LOGGER.debug("getImage - ROI # edited boundHeight: " + boundHeight);

            if (input != null) {
                processor.scaleROI(input, xTopPos, yTopPos, boundWidth, boundHeight, scaleFactor, output);
            }
            input.close();
        } else {
            InputStream input = image.getContentAsInputStream();
            processor.scaleROI(input, xTopPos, yTopPos, boundWidth, boundHeight, scaleFactor, output);
            input.close();
        }
        output.close();
    }

    public float getScaleFactor() {
        return scaleFactor;
    }

}
