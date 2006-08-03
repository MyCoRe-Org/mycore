package org.mycore.services.imaging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.mycore.datamodel.ifs.MCRFile;

public interface CacheManager {
	void getImage(MCRFile image, String filename, OutputStream imageData);
	InputStream getImageAsInputStream(MCRFile image, String filename) throws IOException;
	void saveImage(MCRFile image, String filename, InputStream imageData);
	void deleteImage(MCRFile image, String filename);
	void deleteImage(MCRFile image);
	boolean existInCache(MCRFile image, String filename);
	boolean existInCache(MCRFile image);
	int getImgWidth(MCRFile image);
	int getImgHeight(MCRFile image);
	void setImgSize(MCRFile image, int width, int height);
	void setLock(MCRFile image);
	void removeLock(MCRFile image);
	boolean isLocked(MCRFile image);
}
