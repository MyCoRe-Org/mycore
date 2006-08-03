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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
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

		com = new MCRCommand("clear cache", "org.mycore.services.imaging.MCRImgCacheCommands.clearCache", "The command clear the Image cache.");
		command.add(com);
		
		com = new MCRCommand("create cache for file {0}", "org.mycore.services.imaging.MCRImgCacheCommands.cacheFile String", "The command create the cache version for the given File.");
		command.add(com);

		com = new MCRCommand("remove cache for file {0}", "org.mycore.services.imaging.MCRImgCacheCommands.removeCachedFile String", "The command remove the cache version for the given File.");
		command.add(com);

		com = new MCRCommand("create cache for derivate {0}", "org.mycore.services.imaging.MCRImgCacheCommands.cacheDeriv String", "The command create the cache version for the given Derivate.");
		command.add(com);

		com = new MCRCommand("remove cache for derivate {0}", "org.mycore.services.imaging.MCRImgCacheCommands.removeCachedDeriv String", "The command remove the cache version for the given Derivate.");
		command.add(com);

		com = new MCRCommand("test {0}", "org.mycore.services.imaging.MCRImgCacheCommands.test String", "Just testing.");
		command.add(com);

		com = new MCRCommand("testProz {0} {1} {2} {3} {4} {5} {6} {7}", "org.mycore.services.imaging.MCRImgCacheCommands.testProz String String String String String String String String", "testProz(cmd, inFile, outFile, xPos, yPos, width, height, scaleFactor)" + System.getProperty("line.separator") + "cmd = {resizeFitWidth; resizeFitHeight; resize; scale; scaleROI");
		command.add(com);

		/*
		 * com = new MCRCommand( "cacheMkdir {0}",
		 * "org.mycore.services.imaging.MCRImgCacheCommands.cacheMkdir String",
		 * "The command create the Directories in the Image cache.");
		 * command.add(com);
		 */
	}

	public static void clearCache() {
		MCRDirectory.getRootDirectory("imgCache").delete();
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

		MCRImgCacheManager cache = new MCRImgCacheManager();
		MCRFile image = null;

		for (Iterator iter = list.iterator(); iter.hasNext();) {
			image = (MCRFile) iter.next();
			image.removeAdditionalData(new Document(new Element("ImageMetaData")));
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
		LOGGER.debug("* The node: " + node.getAbsolutePath());
		LOGGER.debug("****************************************");

		if (node != null && node instanceof MCRFile)
			imgFile = (MCRFile) node;
		else
			throw new MCRException("File " + ID + " does not exist!");

		if (suppContTypes.indexOf(imgFile.getContentTypeID()) > -1) {
			MCRImgCacheManager cache = new MCRImgCacheManager();
			MCRFile image = null;

			image = imgFile;
			image.removeAdditionalData(new Document(new Element("ImageMetaData")));
			cache.deleteImage(image);
		}
		LOGGER.info("Remove cache version for image " + imgFile.getName() + " finished successfull!");

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
		}
		else {
			LOGGER.info("******************************************");
			LOGGER.info("* File " + ID + " does not exist!");
			LOGGER.info("******************************************");
		}
		
	}
	
	public static void cacheFile(MCRFile imgFile) throws Exception {
		MCRConfiguration config = MCRConfiguration.instance();
		String suppContTypes = new String(config.getString("MCR.Module-iview.SupportedContentTypes"));
		boolean cacheOrig = Boolean.getBoolean(config.getString("MCR.Module-iview.cacheOrig"));
		int thumbWidth = Integer.parseInt(config.getString("MCR.Module-iview.thumbnail.size.width"));
		int thumbHeight = Integer.parseInt(config.getString("MCR.Module-iview.thumbnail.size.height"));

		if (suppContTypes.indexOf(imgFile.getContentTypeID()) > -1) {
			LOGGER.debug("****************************************");
			LOGGER.debug("* MCRImgCacheCommands - cacheFile      *");
			LOGGER.debug("* suppContTypes.indexOf(imgFile.getContentTypeID()) > -1  *");
			LOGGER.debug("****************************************");
			MCRImgCacheManager cache = new MCRImgCacheManager();
			MCRImgProcessor processor = new MCRImgProcessor();
			MCRFile image = null;
			ByteArrayOutputStream imgData = new ByteArrayOutputStream();

			image = imgFile;

			processor.loadImageFileCache(image.getContentAsInputStream());
			Dimension imgSize = processor.getOrigSize();
			cache.setImgSize(image, imgSize.width, imgSize.height);
			cache.setLock(image);
			
			// cache Original
			if (cacheOrig && !processor.hasCorrectTileSize() && !cache.existInCache(image, cache.ORIG)){
				LOGGER.debug("****************************************");
				LOGGER.debug("* MCRImgCacheCommands - cacheFile      *");
				LOGGER.debug("* (!processor.hasCorrectTileSize() && !cache.existInCache(image, cache.ORIG)) *");
				LOGGER.debug("****************************************");
				processor.tiffEncode(imgData);
				ByteArrayInputStream input = new ByteArrayInputStream(imgData.toByteArray());
				cache.saveImage(image, cache.ORIG, input);
				imgData.reset();
			}
			else {
				LOGGER.info("****************************************");
				LOGGER.info("Image " + image.getName() + " as version " + cache.ORIG + " allready exists in Cache!");
				LOGGER.info("****************************************");
			}
			
			// cache small version
			if ((imgSize.width > 2 * 1024 || imgSize.height > 2 * 768) && !cache.existInCache(image, cache.CACHE)) {
				LOGGER.debug("****************************************");
				LOGGER.debug("* MCRImgCacheCommands - cacheFile      *");
				LOGGER.debug("* (imgSize.width > 2 * 1024 || imgSize.height > 2 * 768)  *");
				LOGGER.debug("****************************************");
				
				processor.resize(1024, 768);
				processor.tiffEncode(imgData);
				ByteArrayInputStream input = new ByteArrayInputStream(imgData.toByteArray());
				cache.saveImage(image, cache.CACHE, input);
				imgData.reset();

				LOGGER.info("****************************************");
				LOGGER.info("Image " + image.getName() + " cached successfull under the name " + cache.CACHE + " !");
				LOGGER.info("****************************************");
				
			}
			else {
				LOGGER.info("****************************************");
				LOGGER.info("Image " + image.getName() + " as version " + cache.CACHE + " allready exists in Cache!");
				LOGGER.info("****************************************");
			}
			
			//	cache Thumbnail
			if (!cache.existInCache(image, cache.THUMB)) {
				LOGGER.debug("****************************************");
				LOGGER.debug("* MCRImgCacheCommands - cacheFile      *");
				LOGGER.debug("* !cache.existInCache(image, cache.THUMB)  *");
				LOGGER.debug("****************************************");
				
				processor.resize(thumbWidth, thumbHeight);
				processor.jpegEncode(imgData);
				ByteArrayInputStream input = new ByteArrayInputStream(imgData.toByteArray());
				cache.saveImage(image, cache.THUMB, input);
				imgData.reset();
						
				LOGGER.info("****************************************");
				LOGGER.info("Image " + image.getName() + " cached successfull under the name " + cache.THUMB + " !");
				LOGGER.info("****************************************");
			}
			else {
				LOGGER.info("****************************************");
				LOGGER.info("Image " + image.getName() + " as version " + cache.THUMB + " allready exists in Cache!");
				LOGGER.info("****************************************");
			}
			
			cache.removeLock(image);
			LOGGER.info("****************************************");
			LOGGER.info("* Caching image " + imgFile.getName() + " finished successfull!");
			LOGGER.info("****************************************");
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

	public static void test(String ID) throws IOException {
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

		MCRImgService imgService = new MCRImgService();
		MCRFile image = null;

		Stopwatch timer = new Stopwatch();

		for (Iterator iter = list.iterator(); iter.hasNext();) {
			image = (MCRFile) iter.next();
			FileOutputStream out = new FileOutputStream("/home/chi/images/filestore/" + image.getName());
			BufferedOutputStream buffOut = new BufferedOutputStream(out);
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			MCRImgProcessor proz = new MCRImgProcessor();

			timer.reset();
			timer.start();
			// imgService.getImage(image, new Dimension(600, 300), bout);
			proz.getImageSize(image.getContentAsInputStream());
			timer.stop();
			LOGGER.info("Timer get size: " + timer.getElapsedTime());

			timer.reset();
			timer.start();
			// imgService.getImage(image, new Dimension(600, 300), bout);
			imgService.getImage(image, 300, 400, 600, 300, 0.4F, bout);
			timer.stop();
			LOGGER.info("Timer: " + timer.getElapsedTime());
		}
	}

	public static void testProz(String cmd, String inFile, String outFile, String xPos, String yPos, String width, String height, String scaleFactor) throws IOException {

		Stopwatch timer = new Stopwatch();

		FileInputStream in = new FileInputStream(inFile);
		BufferedInputStream buffIn = new BufferedInputStream(in);
		FileOutputStream out = new FileOutputStream(outFile);
		BufferedOutputStream buffOut = new BufferedOutputStream(out);
		ImgProcessor proz = new MCRImgProcessor();

		LOGGER.info("*****************************");
		LOGGER.info("* Amazing Test Proz Command *");
		LOGGER.info("*****************************");

		if (cmd.equals("resizeFitWidth")) {
			timer.reset();
			timer.start();

			proz.resizeFitWidth(buffIn, Integer.valueOf(width).intValue(), buffOut);

			timer.stop();
			LOGGER.info("*******************************************");
			LOGGER.info("* Timer resizeFitWidth: " + timer.getElapsedTime());
			LOGGER.info("* Used scale factor: " + proz.getScaleFactor());
			LOGGER.info("*******************************************");
		} else if (cmd.equals("resizeFitHeight")) {
			timer.reset();
			timer.start();

			proz.resizeFitHeight(buffIn, Integer.valueOf(height).intValue(), buffOut);

			timer.stop();
			LOGGER.info("*******************************************");
			LOGGER.info("* Timer resizeFitHeight: " + timer.getElapsedTime());
			LOGGER.info("* Used scale factor: " + proz.getScaleFactor());
			LOGGER.info("*******************************************");
		} else if (cmd.equals("resize")) {
			timer.reset();
			timer.start();

			proz.resize(buffIn, Integer.valueOf(width).intValue(), Integer.valueOf(height).intValue(), buffOut);

			timer.stop();
			LOGGER.info("*******************************************");
			LOGGER.info("* Timer resize: " + timer.getElapsedTime());
			LOGGER.info("* Used scale factor: " + proz.getScaleFactor());
			LOGGER.info("*******************************************");
		} else if (cmd.equals("scale")) {
			timer.reset();
			timer.start();

			proz.scale(buffIn, Float.valueOf(scaleFactor).floatValue(), buffOut);

			timer.stop();
			LOGGER.info("*******************************************");
			LOGGER.info("* Timer scale: " + timer.getElapsedTime());
			LOGGER.info("* Used scale factor: " + proz.getScaleFactor());
			LOGGER.info("*******************************************");
		} else if (cmd.equals("scaleROI")) {
			int xTopPos = Integer.valueOf(xPos).intValue();
			int yTopPos = Integer.valueOf(yPos).intValue();
			int boundWidth = Integer.valueOf(width).intValue();
			int boundHeight = Integer.valueOf(height).intValue();
			float scaleFact = Float.valueOf(scaleFactor).floatValue();

			timer.reset();
			timer.start();

			proz.scaleROI(buffIn, xTopPos, yTopPos, boundWidth, boundHeight, scaleFact, buffOut);

			timer.stop();
			LOGGER.info("*******************************************");
			LOGGER.info("* Timer scaleROI: " + timer.getElapsedTime());
			LOGGER.info("* Used scale factor: " + proz.getScaleFactor());
			LOGGER.info("*******************************************");
		}
	}
}
