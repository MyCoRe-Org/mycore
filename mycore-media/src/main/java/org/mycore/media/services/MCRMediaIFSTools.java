package org.mycore.media.services;

import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.ifs2.MCRDirectory;
import org.mycore.datamodel.ifs2.MCRFile;
import org.mycore.datamodel.ifs2.MCRFileCollection;
import org.mycore.datamodel.ifs2.MCRFileStore;
import org.mycore.datamodel.ifs2.MCRNode;
import org.mycore.datamodel.ifs2.MCRStoreManager;

public class MCRMediaIFSTools {
    private static final Logger LOGGER = LogManager.getLogger(MCRMediaIFSTools.class);

    private static MCRConfiguration config = MCRConfiguration.instance();

    private static String mediaStore = config.getString("MCR.Media.ContentStore.Name", "avdata");

    private static String getPath(String filePath) {
        if (filePath.lastIndexOf("/") != -1) {
            return filePath.substring(0, filePath.lastIndexOf("/") + 1);
        }

        return "";
    }

    private static String getFileName(String filePath) {
        return filePath.replace(getPath(filePath), "");
    }

    private static String getFileExtension(String filePath) {
        String fileName = getFileName(filePath);
        if (fileName.lastIndexOf(".") != -1) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }

        return "";
    }

    private static String buildFileName(String filePath) {
        String ext = getFileExtension(filePath);

        if (filePath.lastIndexOf("." + ext) != -1)
            return filePath.substring(0, filePath.lastIndexOf("." + ext)) + "_" + ext;

        return filePath;
    }

    public static void deleteMetadata(String derivateID, String filePath) throws MCRException {
        LOGGER.debug("Delete metadata for file \"" + filePath + "\" in Derivate " + derivateID);
        try {
            MCRFileStore store = MCRStoreManager.getStore(mediaStore, MCRFileStore.class);
            if (store == null)
                store = MCRStoreManager.createStore(mediaStore, MCRFileStore.class);

            MCRFileCollection col = store.retrieve(Integer.parseInt(derivateID));
            if (col != null) {
                MCRNode node = col.getNodeByPath(buildFileName(filePath) + ".moxml");
                if (node != null) {
                    if (node.isFile())
                        ((MCRFile) node).delete();
                    while (node.getParent() != null) {
                        node = node.getParent();
                        if (node.isDirectory() && !node.hasChildren())
                            ((MCRDirectory) node).delete();
                    }
                }
            }
        } catch (Exception ex) {
            throw new MCRException(ex);
        }
    }

    public static void storeMetadata(org.jdom2.Element mediaXML, String derivateID, String filePath)
        throws MCRException {
        LOGGER.debug("Store metadata for file \"" + filePath + "\" in Derivate " + derivateID);
        try {
            MCRFileStore store = MCRStoreManager.getStore(mediaStore, MCRFileStore.class);
            if (store == null)
                store = MCRStoreManager.createStore(mediaStore, MCRFileStore.class);

            MCRFileCollection col = store.retrieve(Integer.parseInt(derivateID));
            if (col == null)
                col = store.create(Integer.parseInt(derivateID));

            MCRDirectory dir = null;
            StringTokenizer pathTok = new StringTokenizer(getPath(filePath), "/");
            while (pathTok.hasMoreTokens()) {
                dir = col.createDir(pathTok.nextToken());
            }

            MCRFile data;
            if (dir != null)
                data = dir.createFile(buildFileName(getFileName(filePath)) + ".moxml");
            else
                data = col.createFile(buildFileName(getFileName(filePath)) + ".moxml");

            data.setContent(new MCRJDOMContent(mediaXML));
        } catch (Exception ex) {
            throw new MCRException(ex);
        }
    }

    public static void deleteThumbnail(String derivateID, String filePath) throws MCRException {
        LOGGER.debug("Delete thumbnail for file \"" + filePath + "\" in Derivate " + derivateID);
        try {
            MCRFileStore store = MCRStoreManager.getStore(mediaStore, MCRFileStore.class);
            if (store == null)
                store = MCRStoreManager.createStore(mediaStore, MCRFileStore.class);

            MCRFileCollection col = store.retrieve(Integer.parseInt(derivateID));
            if (col != null) {
                MCRNode node = col.getNodeByPath(buildFileName(filePath) + ".mothumb");
                if (node != null) {
                    if (node.isFile())
                        ((MCRFile) node).delete();
                    while (node.getParent() != null) {
                        node = node.getParent();
                        if (node.isDirectory() && !node.hasChildren())
                            ((MCRDirectory) node).delete();
                    }
                }
            }
        } catch (Exception ex) {
            throw new MCRException(ex);
        }
    }

    public static org.jdom2.Document getMetadataFromStore(String derivateID, String filePath) throws MCRException {
        LOGGER.debug("Get metadata for file \"" + filePath + "\" in Derivate " + derivateID);
        try {
            MCRFileStore store = MCRStoreManager.getStore(mediaStore, MCRFileStore.class);
            if (store == null)
                store = MCRStoreManager.createStore(mediaStore, MCRFileStore.class);

            MCRFileCollection col = store.retrieve(Integer.parseInt(derivateID));
            if (col != null) {
                MCRNode node = col.getNodeByPath(buildFileName(filePath) + ".moxml");
                if (node != null && node.isFile()) {
                    return node.getContent().asXML();
                } else
                    throw new MCRException("Metadata for file \"" + filePath + "\" not found.");
            } else
                throw new MCRException("Derivate " + derivateID + " not found.");
        } catch (Exception ex) {
            throw new MCRException(ex);
        }
    }

    public static void storeThumbnail(byte[] thumb, String derivateID, String filePath) throws MCRException {
        LOGGER.debug("Store thumbnail for file \"" + filePath + "\" in Derivate " + derivateID);
        try {
            MCRFileStore store = MCRStoreManager.getStore(mediaStore, MCRFileStore.class);
            if (store == null)
                store = MCRStoreManager.createStore(mediaStore, MCRFileStore.class);

            MCRFileCollection col = store.retrieve(Integer.parseInt(derivateID));
            if (col == null)
                col = store.create(Integer.parseInt(derivateID));

            MCRDirectory dir = null;
            StringTokenizer pathTok = new StringTokenizer(getPath(filePath), "/");
            while (pathTok.hasMoreTokens()) {
                dir = col.createDir(pathTok.nextToken());
            }

            MCRFile data;
            if (dir != null)
                data = dir.createFile(buildFileName(getFileName(filePath)) + ".mothumb");
            else
                data = col.createFile(buildFileName(getFileName(filePath)) + ".mothumb");

            data.setContent(new MCRByteContent(thumb, System.currentTimeMillis()));
        } catch (Exception ex) {
            throw new MCRException(ex);
        }
    }

    public static MCRFile getThumbnailFromStore(String derivateID, String filePath) throws MCRException {
        LOGGER.debug("Get thumbnail for file \"" + filePath + "\" in Derivate " + derivateID);
        try {
            MCRFileStore store = MCRStoreManager.getStore(mediaStore, MCRFileStore.class);
            if (store == null)
                store = MCRStoreManager.createStore(mediaStore, MCRFileStore.class);

            MCRFileCollection col = store.retrieve(Integer.parseInt(derivateID));
            if (col != null) {
                MCRNode node = col.getNodeByPath(buildFileName(filePath) + ".mothumb");
                if (node != null && node.isFile()) {
                    return (MCRFile) node;
                } else
                    throw new MCRException("Thumbnail for file \"" + filePath + "\" not found.");
            } else
                throw new MCRException("Derivate " + derivateID + " not found.");
        } catch (Exception ex) {
            throw new MCRException(ex);
        }
    }

    public static boolean hasThumbnailInStore(String derivateID, String filePath) throws MCRException {
        try {
            MCRFileStore store = MCRStoreManager.getStore(mediaStore, MCRFileStore.class);
            if (store == null)
                store = MCRStoreManager.createStore(mediaStore, MCRFileStore.class);

            MCRFileCollection col = store.retrieve(Integer.parseInt(derivateID));
            if (col != null) {
                MCRNode node = col.getNodeByPath(buildFileName(filePath) + ".mothumb");
                return (node != null && node.isFile());
            }

            return false;
        } catch (Exception ex) {
            throw new MCRException(ex);
        }
    }
}
