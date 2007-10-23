package org.mycore.services.imaging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.mycore.datamodel.ifs.MCRFile;

/**
 * The cache manager represents an object which is responsible for the creating
 * and deleting cache files for an image in the MyCore system.
 * 
 * @author chi
 * 
 */
public interface CacheManager {
    /**
     * Get the cache version of the image.
     * 
     * @param image
     *            the original image
     * @param filename
     *            the cache version thumbnail etc.
     * @param imageData
     *            the cached image
     */
    void getImage(MCRFile image, String filename, OutputStream imageData);

    InputStream getImageAsInputStream(MCRFile image, String filename) throws IOException;

    /**
     * Save the edited image as cache for the original image.
     * 
     * @param image
     *            the original image
     * @param filename
     *            the cache version
     * @param imageData
     *            the cached image
     */
    void saveImage(MCRFile image, String filename, InputStream imageData);

    /**
     * Deletes one certain cache version for the original image.
     * 
     * @param image
     *            the original image
     * @param filename
     *            the cache version of the original image
     */
    void deleteImage(MCRFile image, String filename);

    /**
     * Deletes every cache version for the original image.
     * 
     * @param image
     *            the original image
     */
    void deleteImage(MCRFile image);

    /**
     * Checks if one certain cache version of the original image exists in
     * cache.
     * 
     * @param image
     *            the original image
     * @param filename
     *            the cache version
     * @return true if the cache version exists in cache
     */
    boolean existInCache(MCRFile image, String filename);

    /**
     * Checks if the original image has at least one cache version.
     * 
     * @param image
     * @return true if the the original image has at least one cache version
     */
    boolean existInCache(MCRFile image);

    /**
     * Get the width of the original image which is stored in the cache
     * 
     * @param image
     *            the original image
     * @return int the image width
     */
    int getImgWidth(MCRFile image);

    /**
     * Get the width of the original image height is stored in the cache
     * 
     * @param image
     *            the original image
     * @return int the image height
     */
    int getImgHeight(MCRFile image);

    /**
     * Save the size of the original image in the cache.
     * 
     * @param image
     *            the original image
     * @param width
     *            original image width
     * @param height
     *            original image height
     */
    void setImgSize(MCRFile image, int width, int height);
}
