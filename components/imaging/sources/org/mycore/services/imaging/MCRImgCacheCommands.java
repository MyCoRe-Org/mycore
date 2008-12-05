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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRXMLTableManager;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;

public class MCRImgCacheCommands extends MCRAbstractCommands {
    private static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    private static Logger LOGGER = Logger.getLogger(MCRImgCacheCommands.class.getName());

    private static String SUPPORTED_CONTENT_TYPES = CONFIG.getString("MCR.Module-iview.SupportedContentTypes").toLowerCase();

    private static final int CACHE_WIDTH = CONFIG.getInt("MCR.Module-iview.cache.size.width");

    private static final int THUMB_WIDTH = CONFIG.getInt("MCR.Module-iview.thumbnail.size.width");

    private static final int THUMB_HEIGHT = CONFIG.getInt("MCR.Module-iview.thumbnail.size.height");

    /**
     * The empty constructor.
     */
    public MCRImgCacheCommands() {
        super();
        MCRCommand com = null;

        com = new MCRCommand("delete image cache for all derivates", "org.mycore.services.imaging.MCRImgCacheCommands.deleteCache",
                "The command deletes the whole image cache.");
        command.add(com);

        com = new MCRCommand("create image cache for file {0}", "org.mycore.services.imaging.MCRImgCacheCommands.cacheFile String",
                "The command create the image cache version for the given File.");
        command.add(com);

        com = new MCRCommand("delete image cache for file {0}", "org.mycore.services.imaging.MCRImgCacheCommands.deleteCachedFile String",
                "The command delete the image cache version for the given File.");
        command.add(com);

        com = new MCRCommand("create image cache for derivate {0}", "org.mycore.services.imaging.MCRImgCacheCommands.cacheDerivate String",
                "The command create the image cache version for the given Derivate.");
        command.add(com);
        com = new MCRCommand("create image cache for all derivates", "org.mycore.services.imaging.MCRImgCacheCommands.createCache",
                "The command create the the complete image cache for all derivates in the MyCore System. Caution this will take a lot of time!");
        command.add(com);

        com = new MCRCommand("delete image cache for derivate {0}", "org.mycore.services.imaging.MCRImgCacheCommands.deleteCachedDerivate String",
                "The command delete the image cache version for the given Derivate.");
        command.add(com);

    }

    public static final List<String> createCache() {
        MCRXMLTableManager xmlTableManager = MCRXMLTableManager.instance();
        List<String> derivateList = xmlTableManager.retrieveAllIDs("derivate");
        List<String> returns = new ArrayList<String>(derivateList.size());
        for (String derivateID : derivateList) {
            returns.add("create image cache for derivate " + derivateID);
        }
        return returns;
    }

    public static void deleteCache() {
        MCRImgCacheManager.instance().deleteCache();
        LOGGER.info("Cache deleted!");
    }

    public static List<String> cacheDerivate(String ID) {
        List<String> returns = new ArrayList<String>();
        MCRDirectory derivate = null;

        MCRFilesystemNode node = MCRFilesystemNode.getRootNode(ID);

        if (node == null || !(node instanceof MCRDirectory))
            throw new MCRException("Derivate " + ID + " does not exist or is not a directory!");
        derivate = (MCRDirectory) node;

        List<MCRFile> supportedFiles = getSuppFiles(derivate);
        for (MCRFile image : supportedFiles) {
            returns.add("create image cache for file " + image.getID());
        }
        return returns;
    }

    public static List<String> deleteCachedDerivate(String ID) throws Exception {
        List<String> returns = new ArrayList<String>();
        MCRDirectory derivate = null;

        MCRFilesystemNode node = MCRFilesystemNode.getRootNode(ID);

        if (node == null || !(node instanceof MCRDirectory))
            throw new MCRException("Derivate " + ID + " does not exist or is not a directory!");
        derivate = (MCRDirectory) node;

        List<MCRFile> supportedFiles = getSuppFiles(derivate);

        for (MCRFile image : supportedFiles) {
            returns.add("delete image cache for file " + image.getID());
        }
        return returns;
    }

    public static void cacheFile(String ID) throws Exception {
        MCRFilesystemNode node = MCRFilesystemNode.getNode(ID);
        MCRFile imgFile = null;

        if (node != null && node instanceof MCRFile) {
            imgFile = (MCRFile) node;
            cacheFile(imgFile);
        } else {
            LOGGER.info("File " + ID + " does not exist!");
        }

    }

    public static void deleteCachedFile(String ID) throws Exception {
        MCRFile imgFile = null;
        MCRFilesystemNode node = MCRFilesystemNode.getNode(ID);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MCRImgCacheCommands - removeCachedFile ");
            LOGGER.debug("The node: " + node.getAbsolutePath());
        }

        if (node != null && node instanceof MCRFile)
            imgFile = (MCRFile) node;
        else
            throw new MCRException("File " + ID + " does not exist!");

        if (isSupported(imgFile.getContentTypeID())) {
            MCRImgCacheManager cache = MCRImgCacheManager.instance();
            imgFile.removeAdditionalData("ImageMetaData");
            cache.deleteImage(imgFile);
        }
        LOGGER.info("Remove cache version for image " + imgFile.getName() + " finished successfull!");

    }

    private static List<MCRFile> getSuppFiles(MCRDirectory rootNode) {
        ArrayList<MCRFile> files = new ArrayList<MCRFile>();
        MCRFilesystemNode[] nodes = rootNode.getChildren();
        for (MCRFilesystemNode node : nodes) {
            if (node instanceof MCRDirectory) {
                MCRDirectory dir = (MCRDirectory) node;
                files.addAll(getSuppFiles(dir));
            } else {
                MCRFile file = (MCRFile) node;
                if (isSupported(file.getContentTypeID())) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    private static void cacheFile(MCRFile imgFile) throws Exception {
        cacheFile(imgFile, false);
    }

    public static void cacheFile(MCRFile image, boolean cacheSizeOnly) {
        boolean useCache = CONFIG.getBoolean("MCR.Module-iview.useCache");
        String fileType = image.getContentTypeID();
        String filename = image.getName();

        try {
            InputStream imgInputStream = image.getContentAsInputStream();

            if (isSupported(fileType)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("MCRImgCacheCommands - " + filename);
                    LOGGER.debug("File type " + fileType + " supported in file list - " + SUPPORTED_CONTENT_TYPES);
                }
                CacheManager cache = MCRImgCacheManager.instance();
                MCRImgProcessor processor = new MCRImgProcessor();

                if (cacheSizeOnly)
                    processor.loadImage(imgInputStream);
                else
                    processor.loadImageFileCache(imgInputStream);

                Dimension imgSize = new Dimension(0, 0);
                try {
                    imgSize = processor.getOrigSize();
                    cache.setImgSize(image, imgSize.width, imgSize.height);
                } catch (Exception e) {
                    useCache = false;
                    LOGGER.error("Could not get image size for " + image.getName() + " will not cached!", e);
                }

                if (useCache && !cacheSizeOnly) {
                    // cache.setLock(image);
                    boolean cacheOrig = CONFIG.getBoolean("MCR.Module-iview.cacheOrig");
                    // cache Original
                    if (cacheOrig && !processor.hasCorrectTileSize()) {
                        if (!cache.existInCache(image, MCRImgCacheManager.ORIG)) {
                            ByteArrayOutputStream imgData = new ByteArrayOutputStream();
                            processor.tiffEncode(imgData);
                            if ((fileType.equals("tiff") || fileType.equals("tif"))) {
                                LOGGER.debug("MCRImgCacheCommands - cacheFile");
                                LOGGER.debug("is TIFF");
                                image.setContentFrom(imgData.toByteArray());
                            } else {
                                LOGGER.debug("MCRImgCacheCommands - cacheFile");
                                LOGGER.debug("not TIFF");
                                ByteArrayInputStream input = new ByteArrayInputStream(imgData.toByteArray());
                                cache.saveImage(image, MCRImgCacheManager.ORIG, input);
                                imgData.reset();
                            }
                        } else {
                            LOGGER.info("Image " + image.getName() + " as version " + MCRImgCacheManager.ORIG + " allready exists in Cache!");
                        }

                    } else {
                        LOGGER.info("Image " + image.getName() + " is to small for caching as version " + MCRImgCacheManager.ORIG + " !");
                    }

                    int factor = 2;

                    if (imgSize.width < imgSize.height)
                        factor = 1;
                    // cache small version
                    if ((imgSize.width > factor * CACHE_WIDTH)) {
                        LOGGER.debug("MCRImgCacheCommands - cacheFile");
                        if (!cache.existInCache(image, MCRImgCacheManager.CACHE)) {
                            ByteArrayOutputStream imgData = new ByteArrayOutputStream();
                            processor.resizeFitWidth(CACHE_WIDTH);
                            processor.encode(imgData, MCRImgProcessor.TIFF_ENC);
                            ByteArrayInputStream input = new ByteArrayInputStream(imgData.toByteArray());
                            cache.saveImage(image, MCRImgCacheManager.CACHE, input);
                            imgData.reset();

                            LOGGER.info("Image " + image.getName() + " cached successfull under the name " + MCRImgCacheManager.CACHE + " !");
                        } else {
                            LOGGER.info("Image " + image.getName() + " as version " + MCRImgCacheManager.CACHE + " allready exists in Cache!");
                        }

                    } else {
                        LOGGER.info("Image " + image.getName() + " is to small for caching as version " + MCRImgCacheManager.CACHE + " !");
                    }

                    // cache Thumbnail
                    if (!cache.existInCache(image, MCRImgCacheManager.THUMB)) {
                        ByteArrayOutputStream imgData = new ByteArrayOutputStream();
                        processor.resize(THUMB_WIDTH, THUMB_HEIGHT);
                        processor.encode(imgData, MCRImgProcessor.JPEG_ENC);
                        ByteArrayInputStream input = new ByteArrayInputStream(imgData.toByteArray());
                        cache.saveImage(image, MCRImgCacheManager.THUMB, input);
                        imgData.reset();

                        LOGGER.info("Image " + image.getName() + " cached successfull under the name " + MCRImgCacheManager.THUMB + " !");
                    } else {
                        LOGGER.info("Image " + image.getName() + " as version " + MCRImgCacheManager.THUMB + " allready exists in Cache!");
                    }

                    LOGGER.info("Caching image " + image.getName() + " finished successfull!");
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("MCRImgCacheCommands - " + filename);
                        LOGGER.debug("File type " + fileType + " not supported in file list - " + SUPPORTED_CONTENT_TYPES);
                    }
                }
            }

            imgInputStream.close();
        } catch (IOException e) {
            LOGGER.error(e);
            LOGGER.info("Loading image " + image.getName() + " failed!");
        }
    }

    private static boolean isSupported(String fileType) {
        return SUPPORTED_CONTENT_TYPES.indexOf(fileType.toLowerCase()) > -1;
    }
}
