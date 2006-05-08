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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileNodeServlet;
import org.mycore.datamodel.ifs.MCRFilesystemNode;

/***
 * The MCRIFSConnector deliver the method for getting
 * data into and out of the cache structure which lies
 * in the IFS.
 * The cache structure:<br>
 * <hr><blockquote><pre>
 * 				imgCache
 * 				/	   \
 * 	   	       /  1..n  \
 * 		MCRDerivate .......
 * 			/	   \
 * 		   /  1..n  \
 * 		Abs.Path("/" replaced as "%20")+filname
 * 		  /					      \
 * 		 /			 	 	       \
 * 		Thumbnail			(image - filename = scalefactor)
 * 		 /
 * 		/
 * 		(Thumbnail - filename = thumbsize)
 * </pre></blockquote><hr>
 * 
 * @version 0.01pre 03/01/2006
 * @author Vu Huu Chi
 *
 */

public class MCRIFSConnector{
	private Logger LOGGER = Logger.getLogger(MCRFileNodeServlet.class.getName());
	
	protected MCRDirectory cacheInIFS = null;
	
	public MCRIFSConnector(){
		if ((cacheInIFS = MCRDirectory.getRootDirectory("imgCache")) == null)
			try {
				cacheInIFS = new MCRDirectory("imgCache", "imgCache");
			} catch (Exception e) {
				throw new MCRException(e.getMessage());
			}
		/*else
			cacheInIFS = MCRDirectory.getRootDirectory("imgCache");*/
	}
	
	private MCRDirectory getCacheDir(){
		MCRDirectory imgCacheDir = MCRDirectory.getRootDirectory("imgCache");
		if ( imgCacheDir == null)
			try {
				return new MCRDirectory("imgCache", "imgCache");
			} catch (Exception e) {
				throw new MCRException(e.getMessage());
			}
		else
			return imgCacheDir;
	}
	
	/**
	 * create the required path if not exist and create the
	 * thumbnail in the cache tree.
	 * 
	 * @param ownerID - the ID of the MCRDerivate
	 * @param imgPath - the absolute path to the original image
	 * @param thumbsize - size of the thumbnail, set in the properties
	 * @param in - the InputStream you get e.g. from MCRImageProcessor_old
	 */
	public void setThumbnail(String ownerID, String imgPath, int thumbWidth, int thumbHeight, InputStream in) {
		String filename = String.valueOf(thumbWidth) + "x" + String.valueOf(thumbHeight);
		String thumbPath = ownerID + imgPath.replaceAll("/","%20") + "/Thumbnail";
		createFile(thumbPath, filename, in);
	}
	
	/*public void setThumbnail(String ownerID, String imgPath, int thumbWidth, int thumbHeight, ByteArrayOutputStream fromOut) {
		String filename = String.valueOf(thumbWidth) + "x" + String.valueOf(thumbHeight);
		String thumbPath = ownerID + imgPath.replaceAll("/","%20") + "/Thumbnail";
		createFile(thumbPath, filename, fromOut);
	}*/

	/**
	 * create the required path if not exist and create the
	 * scaled image in the cache tree.
	 * 
	 * @param ownerID - the ID of the MCRDerivate
	 * @param imgPath - the absolute path to the original image
	 * @param scaleFactor - the scale factor wich was used for scaling the orig. image
	 * @param in - the InputStream you get e.g. from MCRImageProcessor_old
	 */
	public void setImage(String ownerID, String imgPath, float scaleFactor, InputStream in) {
		String filename = String.valueOf(scaleFactor);
		String cachedImgPath = ownerID + imgPath.replaceAll("/","%20");
		createFile(cachedImgPath, filename, in);
	}

	public boolean getThumbnail(String ownerID, String imgPath, int thumbWidth, int thumbHeight, OutputStream output) {
		String filename = String.valueOf(thumbWidth) + "x" + String.valueOf(thumbHeight);
		String thumbPath = ownerID + imgPath.replaceAll("/","%20") + "/Thumbnail";
		MCRFile cachedImg = getFromCache(thumbPath + "/" + filename);
		
		if (cachedImg != null){
			cachedImg.getContentTo(output);
			return true;
		}
		else
			return false;
	}
	
	public boolean getImage(String ownerID, String imgPath, float scaleFactor, OutputStream output) {
		String filename = String.valueOf(scaleFactor);
		String cachedImgPath = ownerID + imgPath.replaceAll("/","%20");
		MCRFile cachedImg = getFromCache(cachedImgPath + "/" + filename);
		
		if (cachedImg != null){
			cachedImg.getContentTo(output);
			return true;
		}
		else
			return false;
	}
	
	private MCRDirectory mkdir(String path) throws Exception{
		MCRDirectory currentDir = cacheInIFS;
		
		StringTokenizer st = new StringTokenizer(path, "/");
		while(st.hasMoreTokens()){
			String dirName = st.nextToken();
			MCRFilesystemNode node = currentDir.getChild(dirName);
			
			if (node == null)
				currentDir = new MCRDirectory(dirName, currentDir);
			else if(node instanceof MCRDirectory)
				currentDir = (MCRDirectory)node;
			else
				throw new Exception("Could not create Directory " + dirName + " in " + currentDir.getAbsolutePath()
									+ "\n File allready exist.");
				
		}
		
		return currentDir;
	}
	
	private void createFile(String path, String filename, InputStream in){
		MCRDirectory cachePath = (MCRDirectory)cacheInIFS.getChildByPath(path);
		
		if (cachePath == null)
			try {
				cachePath = mkdir(path);
			} catch (Exception e) {
				LOGGER.debug(this.getClass().getName() + ": Could not create " + path + " in IFS.");
			}
		MCRFile cachedImage = (MCRFile)cachePath.getChild(filename);
		
		if (cachedImage == null)
			try {
				cachedImage = new MCRFile(filename,cachePath);
			} catch (Exception e) {
				LOGGER.debug(this.getClass().getName() + ": Could not create Path in IFS " + path);
			}
		
		if (in != null)
			cachedImage.setContentFrom(in);
	}
	
	/*private void createFile(String path, String filename, ByteArrayOutputStream fromOut){
		MCRDirectory cachePath = (MCRDirectory)cacheInIFS.getChildByPath(path);
		
		if (cachePath == null)
			try {
				cachePath = mkdir(path);
			} catch (Exception e) {
				LOGGER.debug("Could not create Path in IFS" + path);
			}
		MCRFile cachedImage = (MCRFile)cachePath.getChild(filename);
		
		if (cachedImage == null){
			try {
				cachedImage = new MCRFile(filename,cachePath);
			} catch (Exception e) {
				LOGGER.debug("Could not create Path in IFS" + path);
			}
		
			if (fromOut != null)
				cachedImage.setContentFrom(fromOut.toByteArray());
			else
				throw new MCRException("Could now create Image File in IFS, ");
		}
	}*/
	
	private MCRFile getFromCache(String path) throws MCRException {
		return (MCRFile)getCacheDir().getChildByPath(path);
	}

	public void setimgSize(String ownerID, String imgPath, Dimension size) {
		// Height x Width
		String imgSize = String.valueOf(size.width)+"x"+String.valueOf(size.height);
		String cachedImgPath = ownerID + imgPath.replaceAll("/","%20") + "/Size";
		createFile(cachedImgPath, imgSize, null);
	}

	public int[] getImgSize(String ownerID, String imgPath) {
		String cachedImgPath = ownerID + imgPath.replaceAll("/","%20") + "/Size";
		
		MCRDirectory cachedImgDir = (MCRDirectory) getCacheDir().getChildByPath(cachedImgPath);
		if (cachedImgDir != null){
			String[] st = cachedImgDir.getChild(0).getName().split("x");
			/*size[0] = Integer.valueOf(st[0]).intValue();
			size[1] = Integer.valueOf(st[1]).intValue();
			return size;*/
			return new int[]{Integer.valueOf(st[0]).intValue(),
							 Integer.valueOf(st[1]).intValue()};
		}
		else
			return new int[]{0,0};
	}

	public float getImage(String ownerID, String imgPath, int imgWidth, OutputStream output) {
		int[] imageSize = getImgSize(ownerID, imgPath);
		float scaleFactor = 0.0F;
		if (imageSize[0] != 0 && imageSize[1] != 0){
			scaleFactor = (float)imgWidth / (float)imageSize[0];
			getImage(ownerID, imgPath, scaleFactor, output);
		}
		return scaleFactor;
	}
	
	public float getImage(String ownerID, String imgPath, int imgWidth, int imgHeight, OutputStream output) {
		int[] imageSize = getImgSize(ownerID, imgPath);
		float scaleFactor = 0.0F;
		if (imageSize[0] != 0 && imageSize[1] != 0){
			scaleFactor = (imageSize[0] > imageSize[1])? (float)imgHeight / (float)imageSize[0]:
														 (float)imgWidth / (float)imageSize[1];
			getImage(ownerID, imgPath, scaleFactor, output);
		}
		return scaleFactor;
	}
}
