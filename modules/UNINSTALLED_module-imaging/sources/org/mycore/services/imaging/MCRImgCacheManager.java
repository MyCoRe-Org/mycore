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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.mycore.common.MCRException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;

/*******************************************************************************
 * The MCRImgCacheManager deliver the method for getting data into and out of
 * the cache structure which lies in the IFS.<br>
 * The cache structure is just an one level flat tree:<br>
 * |--> [imgCache] --> [cached Image File] --> [versions of image]<br>
 * <br>
 * 
 * Cached image file is a subfolder of the root node imgCache in IFS. The name
 * is composed the following:<br>
 * 'imgCache' + ownerID + absolute path + filename<br>
 * in absolute path the "/" is replaced by "%20"
 * 
 * 
 * @version 0.01pre 03/01/2006<br>
 *          1.00 08/08/2006
 * @author Vu Huu Chi
 * 
 */

public class MCRImgCacheManager implements CacheManager {
    public static final String THUMB = "Thumb";

    public static final String CACHE = "Cache";

    public static final String ORIG = "Orig";

    public static final String CACHE_FOLDER = "imgCache";

    private Logger LOGGER = Logger.getLogger(MCRImgCacheManager.class.getName());

    private static MCRImgCacheManager cacheManager;

    public static synchronized MCRImgCacheManager instance() {
        if (cacheManager == null)
            cacheManager = new MCRImgCacheManager();

        return cacheManager;
    }

    private MCRImgCacheManager() {
    }

    public void getImage(MCRFile image, String filename, OutputStream imageData) {
        MCRDirectory cacheNode = (MCRDirectory) MCRFilesystemNode.getRootNode(buildPath(image));
        MCRFilesystemNode cachedImg = cacheNode.getChild(filename);

        if (cachedImg != null && cachedImg instanceof MCRFile) {
            Stopwatch timer = new Stopwatch();

            timer.start();
            ((MCRFile) cachedImg).getContentTo(imageData);
            timer.stop();
            LOGGER.info("getContentTo: " + timer.getElapsedTime());
        } else
            throw new MCRException("Could not load " + image.getName() + "from cache!");
    }

    public InputStream getImageAsInputStream(MCRFile image, String filename) throws IOException {
        MCRDirectory cacheNode = (MCRDirectory) MCRFilesystemNode.getRootNode(buildPath(image));
        MCRFilesystemNode cachedImg = cacheNode.getChild(filename);

        if (cachedImg != null && cachedImg instanceof MCRFile) {
            LOGGER.debug("Return Image from Cache as InputStream.");
            return ((MCRFile) cachedImg).getContentAsInputStream();
        } else {
            LOGGER.debug("Return Null.");
            return null;
        }
    }

    public void saveImage(MCRFile image, Dimension size, InputStream imageData) {
        saveImage(image, dimToString(size), imageData);
    }

    public synchronized void saveImage(MCRFile image, String filename, InputStream imageData) {

        MCRDirectory cacheNode = getCacheDir(image);

        if (cacheNode == null)
            cacheNode = setCacheDir(image);

        try {
            MCRFile cachedImgIFS = new MCRFile(filename, cacheNode);

            cachedImgIFS.setContentFrom(imageData);
        } catch (Exception e) {
            throw new MCRException("Could not save Image " + image.getName() + " as " + filename + " in cache!");
        }
    }

    public void deleteImage(MCRFile image, Dimension size) {
        deleteImage(image, dimToString(size));
    }

    public synchronized void deleteImage(MCRFile image, String filename) {
        MCRDirectory cacheNode = (MCRDirectory) MCRFilesystemNode.getRootNode(buildPath(image));
        MCRFilesystemNode cachedImg = cacheNode.getChild(filename);

        if (cachedImg != null && cachedImg instanceof MCRFile)
            ((MCRFile) cachedImg).delete();
        else
            LOGGER.debug("Could not delete " + image.getName() + "from cache!");
    }

    public void deleteImage(MCRFile image) {
        MCRFilesystemNode cacheNode = MCRFilesystemNode.getRootNode(buildPath(image));

        if (cacheNode != null && cacheNode instanceof MCRDirectory) {
            LOGGER.debug("MCRImgCacheManager - deleteImage");
            LOGGER.debug("Delte : " + cacheNode.getName());
            ((MCRDirectory) cacheNode).delete();
        } else
            LOGGER.debug("Could not delete " + image.getName() + "from cache!");
    }

    public boolean existInCache(MCRFile image, String filename) {
        MCRDirectory cacheNode = (MCRDirectory) MCRFilesystemNode.getRootNode(buildPath(image));
        String parentName = image.getParent().getName();
        String origName = image.getAbsolutePath() + "/" +image.getName();
        
        if (cacheNode == null && !parentName.startsWith(CACHE_FOLDER)) {
            LOGGER.debug("Cache node does not exist for " + origName);
            return false;
        } else {
            MCRFilesystemNode cachedImg = cacheNode.getChild(filename);
            String absolutePath = cacheNode.getAbsolutePath();
            
            if (cachedImg != null && cachedImg instanceof MCRFile ) {
                LOGGER.debug("Cache version " + filename + " exist in " + absolutePath);
                return true;
            } else {
                LOGGER.debug("Cache version " + filename + " don't exist in " + absolutePath);
                return false;
            }
        }
    }

    public boolean existInCache(MCRFile image) {
        return (existInCache(image, THUMB) || existInCache(image, ORIG) || existInCache(image, CACHE));
    }

    public int getImgWidth(MCRFile image) {
        int width = 0;

        try {
            Element addData = image.getAdditionalData("ImageMetaData");

            if (addData != null) {
                width = (new Integer(addData.getChild("imageSize").getChild("width").getText())).intValue();
            }
        } catch (IOException e) {
            LOGGER.debug(e);
        } catch (JDOMException e) {
            LOGGER.debug(e);
        }

        return width;
    }

    public int getImgHeight(MCRFile image) {
        int height = 0;

        try {
            Element addData = image.getAdditionalData("ImageMetaData");

            if (addData != null) {
                height = (new Integer(addData.getChild("imageSize").getChild("height").getText())).intValue();
            }
        } catch (IOException e) {
            LOGGER.debug(e);
        } catch (JDOMException e) {
            LOGGER.debug(e);
        }

        return height;
    }

    public void setImgSize(MCRFile image, int width, int height) {
        try {
            Element addData = image.getAdditionalData("ImageMetaData");
            if (addData == null) {
                addData = new Element("ImageMetaData").addContent(new Element("imageSize"));
                Element elem = addData.getChild("imageSize");
                elem.addContent((new Element("width")).setText(String.valueOf(width)));
                elem.addContent((new Element("height")).setText(String.valueOf(height)));
                image.setAdditionalData(addData);

                LOGGER.info("Writing additional data for " + image.getName() + " complete!");
            } else
                LOGGER.info("Additional data for " + image.getName() + " allready exists!");
        } catch (IOException e) {
            LOGGER.debug(e);
        } catch (JDOMException e) {
            LOGGER.debug(e);
        }
    }

    /* *********************************************************************************************** */
    /* End implement interface */
    /* *********************************************************************************************** */

    private synchronized MCRDirectory getCacheDir(MCRFile image) {
        String path = buildPath(image);
        // MCRFilesystemNode node = getCacheInIFS().getChildByPath(path);
        MCRFilesystemNode node = MCRFilesystemNode.getRootNode(path);
        MCRDirectory cachedImg = null;

        LOGGER.info("PATH for img in Cache: " + path);
        LOGGER.info("Node: " + node);
        LOGGER.info("Directory: " + (node instanceof MCRDirectory));

        if (node != null && node instanceof MCRDirectory) {
            cachedImg = (MCRDirectory) node;
            LOGGER.debug("Path in cache found: " + cachedImg.getAbsolutePath());
        } else {
            // cachedImg = new MCRDirectory(path, cacheInIFS);
            LOGGER.debug("No path in cache!" + path);
        }
        return cachedImg;
    }

    /**
     * Creates a directory for the cache version of an image file
     * 
     * @param image
     * @return
     */
    private MCRDirectory setCacheDir(MCRFile image) {
        String path = buildPath(image);
        // MCRDirectory dir = new MCRDirectory(path, getCacheInIFS());
        MCRDirectory dir = new MCRDirectory(path, path);

        return dir;
    }

    private String buildPath(MCRFile image) {
        String ownerID = image.getOwnerID();
        String absPath = image.getAbsolutePath().replaceAll("/", "%20");

        return CACHE_FOLDER + ownerID + absPath;
    }

    // return 'width'x'height' as String
    private String dimToString(Dimension size) {
        return size.width + "x" + size.height;
    }

    public boolean existInCache(MCRFile image, Dimension size) {
        return existInCache(image, dimToString(size));
    }

}
