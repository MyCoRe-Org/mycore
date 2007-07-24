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
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;

public class MCRImgCacheCommands extends MCRAbstractCommands {
    private static Logger LOGGER = Logger.getLogger(MCRImgCacheCommands.class.getName());

    /**
     * The empty constructor.
     */
    public MCRImgCacheCommands() {
        super();
        MCRCommand com = null;

        com = new MCRCommand("clear image cache", "org.mycore.services.imaging.MCRImgCacheCommands.clearCache", "The command clear the image cache.");
        command.add(com);

        com = new MCRCommand("create image cache for file {0}", "org.mycore.services.imaging.MCRImgCacheCommands.cacheFile String", "The command create the image cache version for the given File.");
        command.add(com);

        com = new MCRCommand("remove image cache for file {0}", "org.mycore.services.imaging.MCRImgCacheCommands.removeCachedFile String", "The command remove the image cache version for the given File.");
        command.add(com);

        com = new MCRCommand("create image cache for derivate {0}", "org.mycore.services.imaging.MCRImgCacheCommands.cacheDeriv String", "The command create the image cache version for the given Derivate.");
        command.add(com);

        com = new MCRCommand("remove image cache for derivate {0}", "org.mycore.services.imaging.MCRImgCacheCommands.removeCachedDeriv String", "The command remove the image cache version for the given Derivate.");
        command.add(com);

        /*
         * com = new MCRCommand( "cacheMkdir {0}",
         * "org.mycore.services.imaging.MCRImgCacheCommands.cacheMkdir String",
         * "The command create the Directories in the Image cache.");
         * command.add(com);
         */
    }

    public static void clearCache() throws Exception {
        MCRDirectory dir = (MCRDirectory) MCRFilesystemNode.getRootNode(MCRImgCacheManager.CACHE_FOLDER);
        
        while (dir != null){
            try {
                dir.delete();
                dir = (MCRDirectory) MCRFilesystemNode.getRootNode(MCRImgCacheManager.CACHE_FOLDER);
            } catch (Exception e) {
                LOGGER.info("Maybe inconsistency of image cache! Try to clean up.");
                Session dbSession = MCRHIBConnection.instance().getSession();
                
                int deletedEntities = dbSession.createQuery("delete from MCRFSNODES node where node.owner = :owner")
                                        .setString("owner", MCRImgCacheManager.CACHE_FOLDER)
                                        .executeUpdate();
                dir = (MCRDirectory) MCRFilesystemNode.getRootNode(MCRImgCacheManager.CACHE_FOLDER);
                
                if (dir != null){
                    /*dir = null;
                    LOGGER.info("Big mess!!! Send Developer a mail!");*/
                    throw new Exception("Big mess!!! Send Developer a mail!");
                }
                LOGGER.info("Deleted "+ deletedEntities + " Entities. Image cache cleaned!");
            }
            
        }
        
        LOGGER.info("Cache deleted!");
    }

    private static void getSuppFiles(List list, String contentType, MCRDirectory rootNode) {
        MCRFilesystemNode[] nodes = rootNode.getChildren();
        int i = 0;
        while ((i < nodes.length)) {
            if (nodes[i] instanceof MCRDirectory) {
                MCRDirectory dir = (MCRDirectory) (nodes[i]);
                getSuppFiles(list, contentType, dir);
            } else {
                MCRFile file = (MCRFile) (nodes[i]);
                if (contentType.indexOf(file.getContentTypeID()) > -1) {
                    list.add(file);
                }
            }
            i++;
        }
    }

    public static void removeCachedDeriv(String ID) throws Exception {
        List list = new Vector();
        MCRDirectory derivate = null;
        MCRConfiguration config = MCRConfiguration.instance();
        String suppContTypes = new String(config.getString("MCR.Module-iview.SupportedContentTypes"));

        MCRFilesystemNode node = MCRFilesystemNode.getRootNode(ID);

        if (node != null && node instanceof MCRDirectory)
            derivate = (MCRDirectory) node;
        else
            throw new MCRException("Derivate " + ID + " does not exist!");

        getSuppFiles(list, suppContTypes, derivate);

        MCRImgCacheManager cache = MCRImgCacheManager.instance();
        MCRFile image = null;

        for (Iterator iter = list.iterator(); iter.hasNext();) {
            image = (MCRFile) iter.next();
            image.removeAdditionalData("ImageMetaData");
            cache.deleteImage(image);
        }
    }

    public static void removeCachedFile(String ID) throws Exception {
        List list = new Vector();
        MCRFile imgFile = null;
        MCRConfiguration config = MCRConfiguration.instance();
        String suppContTypes = new String(config.getString("MCR.Module-iview.SupportedContentTypes"));

        MCRFilesystemNode node = MCRFilesystemNode.getNode(ID);

        LOGGER.debug("****************************************");
        LOGGER.debug("* MCRImgCacheCommands - removeCachedFile ");
        LOGGER.debug("* The node: " + node.getAbsolutePath());
        LOGGER.debug("****************************************");

        if (node != null && node instanceof MCRFile)
            imgFile = (MCRFile) node;
        else
            throw new MCRException("File " + ID + " does not exist!");

        if (suppContTypes.indexOf(imgFile.getContentTypeID()) > -1) {
            MCRImgCacheManager cache = MCRImgCacheManager.instance();
            MCRFile image = null;

            image = imgFile;
            image.removeAdditionalData("ImageMetaData");
            cache.deleteImage(image);
        }
        LOGGER.info("******************************************************************************");
        LOGGER.info("* Remove cache version for image " + imgFile.getName() + " finished successfull!");
        LOGGER.info("******************************************************************************");

    }

    public static void cacheFile(String ID) throws Exception {
        MCRFilesystemNode node = MCRFilesystemNode.getNode(ID);
        MCRFile imgFile = null;

        if (node != null && node instanceof MCRFile) {
            LOGGER.info("******************************************");
            LOGGER.info("* File " + ID + " getThem!");
            LOGGER.info("******************************************");
            imgFile = (MCRFile) node;
            cacheFile(imgFile);
        } else {
            LOGGER.info("******************************************");
            LOGGER.info("* File " + ID + " does not exist!");
            LOGGER.info("******************************************");
        }

    }

    public static void cacheFile(MCRFile imgFile) throws Exception {
        cacheFile(imgFile, false);
    }

    public static void cacheFile(MCRFile image, boolean cacheSizeOnly) throws Exception {
        MCRConfiguration config = MCRConfiguration.instance();
        String suppContTypes = new String(config.getString("MCR.Module-iview.SupportedContentTypes"));
        boolean useCache = (new Boolean(config.getString("MCR.Module-iview.useCache"))).booleanValue();
        String fileType = image.getContentTypeID();
        String filename = image.getName();

        if (suppContTypes.indexOf(fileType) > -1) {
            LOGGER.debug("****************************************");
            LOGGER.debug("* MCRImgCacheCommands - " + filename);
            LOGGER.debug("* File type " + fileType + " supported in file list - " + suppContTypes);
            LOGGER.debug("* Index in file list: " + suppContTypes.indexOf(fileType));
            LOGGER.debug("****************************************");
            CacheManager cache = MCRImgCacheManager.instance();
            MCRImgProcessor processor = new MCRImgProcessor();
            // ByteArrayOutputStream imgData = new ByteArrayOutputStream();

            if (cacheSizeOnly)
                processor.loadImage(image.getContentAsInputStream());
            else
                processor.loadImageFileCache(image.getContentAsInputStream());

            Dimension imgSize = processor.getOrigSize();
            cache.setImgSize(image, imgSize.width, imgSize.height);

            if (useCache && !cacheSizeOnly) {
                // cache.setLock(image);
                boolean cacheOrig = (new Boolean(config.getString("MCR.Module-iview.cacheOrig"))).booleanValue();
                // cache Original
                if (cacheOrig && !processor.hasCorrectTileSize() && !cache.existInCache(image, MCRImgCacheManager.ORIG)) {
                    ByteArrayOutputStream imgData = new ByteArrayOutputStream();
                    processor.tiffEncode(imgData);
                    if ((fileType.equals("tiff") || fileType.equals("tif"))) {
                        LOGGER.debug("****************************************");
                        LOGGER.debug("* MCRImgCacheCommands - cacheFile      *");
                        LOGGER.debug("* is TIFF						         *");
                        LOGGER.debug("****************************************");
                        image.setContentFrom(imgData.toByteArray());
                    } else {
                        LOGGER.debug("****************************************");
                        LOGGER.debug("* MCRImgCacheCommands - cacheFile      *");
                        LOGGER.debug("* not TIFF						         *");
                        LOGGER.debug("****************************************");
                        ByteArrayInputStream input = new ByteArrayInputStream(imgData.toByteArray());
                        cache.saveImage(image, MCRImgCacheManager.ORIG, input);
                        imgData.reset();
                    }

                } else {
                    LOGGER.info("****************************************");
                    LOGGER.info("Image " + image.getName() + " as version " + MCRImgCacheManager.ORIG + " allready exists in Cache!");
                    LOGGER.info("****************************************");
                }

                LOGGER.debug("Cache files ORIG: \n" + cache.listCacheDir());
                
                int cacheWidth = Integer.parseInt(config.getString("MCR.Module-iview.cache.size.width"));
                int cacheHeight = Integer.parseInt(config.getString("MCR.Module-iview.cache.size.height"));
                // cache small version
                if ((imgSize.width > 2 * cacheWidth || imgSize.height > 2 * cacheHeight) && !cache.existInCache(image, MCRImgCacheManager.CACHE)) {
                    LOGGER.debug("****************************************");
                    LOGGER.debug("* MCRImgCacheCommands - cacheFile      *");
                    LOGGER.debug("* (imgSize.width > 2 * 1024 || imgSize.height > 2 * 768)  *");
                    LOGGER.debug("****************************************");
                    ByteArrayOutputStream imgData = new ByteArrayOutputStream();
                    processor.resize(cacheWidth, cacheHeight);
                    processor.tiffEncode(imgData);
                    ByteArrayInputStream input = new ByteArrayInputStream(imgData.toByteArray());
                    cache.saveImage(image, MCRImgCacheManager.CACHE, input);
                    imgData.reset();

                    LOGGER.info("****************************************");
                    LOGGER.info("Image " + image.getName() + " cached successfull under the name " + MCRImgCacheManager.CACHE + " !");
                    LOGGER.info("****************************************");

                } else {
                    LOGGER.info("****************************************");
                    LOGGER.info("Image " + image.getName() + " as version " + MCRImgCacheManager.CACHE + " allready exists in Cache!");
                    LOGGER.info("****************************************");
                }
                
                LOGGER.debug("Cache files CACHE: \n" + cache.listCacheDir());

                // cache Thumbnail
                if (!cache.existInCache(image, MCRImgCacheManager.THUMB)) {
                    int thumbWidth = Integer.parseInt(config.getString("MCR.Module-iview.thumbnail.size.width"));
                    int thumbHeight = Integer.parseInt(config.getString("MCR.Module-iview.thumbnail.size.height"));
                    LOGGER.debug("****************************************");
                    LOGGER.debug("* MCRImgCacheCommands - cacheFile      *");
                    LOGGER.debug("* !cache.existInCache(image, MCRImgCacheManager.THUMB)  *");
                    LOGGER.debug("****************************************");
                    ByteArrayOutputStream imgData = new ByteArrayOutputStream();
                    processor.resize(thumbWidth, thumbHeight);
                    processor.jpegEncode(imgData);
                    ByteArrayInputStream input = new ByteArrayInputStream(imgData.toByteArray());
                    cache.saveImage(image, MCRImgCacheManager.THUMB, input);
                    imgData.reset();

                    LOGGER.info("****************************************");
                    LOGGER.info("Image " + image.getName() + " cached successfull under the name " + MCRImgCacheManager.THUMB + " !");
                    LOGGER.info("****************************************");
                    // cache.removeLock(image);
                } else {
                    LOGGER.info("****************************************");
                    LOGGER.info("Image " + image.getName() + " as version " + MCRImgCacheManager.THUMB + " allready exists in Cache!");
                    LOGGER.info("****************************************");
                }

                LOGGER.debug("Cache files THUMB: \n" + cache.listCacheDir());
                
                LOGGER.info("****************************************");
                LOGGER.info("* Caching image " + image.getName() + " finished successfull!");
                LOGGER.info("****************************************");
            } else {
                LOGGER.debug("****************************************");
                LOGGER.debug("* MCRImgCacheCommands - " + filename);
                LOGGER.debug("* File type " + fileType + " not supported in file list - " + suppContTypes);
                LOGGER.debug("* Index in file list: " + suppContTypes.indexOf(fileType));
                LOGGER.debug("****************************************");
            }
        }
    }

    public static void cacheDeriv(String ID) throws Exception {
        List list = new Vector();
        MCRDirectory derivate = null;
        MCRConfiguration config = MCRConfiguration.instance();
        String suppContTypes = new String(config.getString("MCR.Module-iview.SupportedContentTypes"));

        MCRFilesystemNode node = MCRFilesystemNode.getRootNode(ID);

        if (node != null && node instanceof MCRDirectory)
            derivate = (MCRDirectory) node;
        else
            LOGGER.info("Derivate " + ID + " does not exist!");

        getSuppFiles(list, suppContTypes, derivate);

        MCRFile image = null;

        for (Iterator iter = list.iterator(); iter.hasNext();) {
            image = (MCRFile) iter.next();

            cacheFile(image);

        }

        LOGGER.info("Caching supported images from derivate " + derivate.getName() + " finished successfull!");
    }
}
