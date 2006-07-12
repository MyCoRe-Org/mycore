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
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import org.mycore.frontend.cli.MCRClassificationCommands;
import org.mycore.frontend.cli.MCRCommand;

public class MCRImgCacheCommands extends MCRAbstractCommands {
	private static Logger LOGGER = Logger.getLogger(MCRClassificationCommands.class.getName());

	/**
	 * The empty constructor.
	 */
	public MCRImgCacheCommands() {
		super();
		MCRCommand com = null;

		com = new MCRCommand("clear cache", "org.mycore.services.imaging.MCRImgCacheCommands.clearCache", "The command clear the Image cache.");
		command.add(com);

		com = new MCRCommand("create cache for derivate {0}", "org.mycore.services.imaging.MCRImgCacheCommands.cacheThumbDeriv String", "The command create Thumbnail for the given Derivate.");
		command.add(com);

		com = new MCRCommand("remove cache for derivate {0}", "org.mycore.services.imaging.MCRImgCacheCommands.removeCachedDeriv String", "The command create Thumbnail for the given Derivate.");
		command.add(com);
		
		com = new MCRCommand("test {0}", "org.mycore.services.imaging.MCRImgCacheCommands.test String", "Just testing.");
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

	public static void cacheThumbDeriv(String ID) throws Exception {
		List list = new Vector();
		MCRDirectory derivate = null;
		MCRConfiguration config = MCRConfiguration.instance();
		String suppContTypes = new String(config.getString("MCR.Module-iview.SupportedContentTypes"));
		int thumbWidth = Integer.parseInt(config.getString("MCR.Module-iview.thumbnail.size.width"));
		int thumbHeight = Integer.parseInt(config.getString("MCR.Module-iview.thumbnail.size.height"));

		MCRFilesystemNode node = MCRFilesystemNode.getRootNode(ID);

		if (node != null && node instanceof MCRDirectory)
			derivate = (MCRDirectory) node;
		else
			LOGGER.info("Derivate " + ID + " does not exist!");

		getSuppFiles(list, suppContTypes, derivate);

		MCRImgCacheManager cache = new MCRImgCacheManager();
		MCRImgProcessor processor = new MCRImgProcessor();
		MCRFile image = null;
		ByteArrayOutputStream imgData = new ByteArrayOutputStream();

		for (Iterator iter = list.iterator(); iter.hasNext();) {
			image = (MCRFile) iter.next();

			// TODO test!!! rewrite this to be more useable - Chi 22.6.06 14:51
			Dimension imgSize = new Dimension(0, 0);

			// -----------------------------------------------------------------

			// cache Thumbnail first
			if (!cache.existInCache(image, cache.THUMB))
				try {
					processor.useEncoder(processor.JPEG_ENC);
					processor.resize(image.getContentAsInputStream(), thumbWidth, thumbHeight, imgData);
					ByteArrayInputStream input = new ByteArrayInputStream(imgData.toByteArray());
					cache.saveImage(image, cache.THUMB, input);
					imgData.reset();
					
					imgSize = processor.getOrigSize();
					cache.setImgSize(image, imgSize.width, imgSize.height);
					
					LOGGER.info("Image " + image.getName() + " cached successfull under the name " + cache.THUMB + " !");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			else
				LOGGER.info("Image " + image.getName() + " as version " + cache.THUMB + " allready exists in Cache!");
			// **************

			// cache Original
			if (!cache.existInCache(image, cache.ORIG))
				try {
					processor.useEncoder(processor.TIFF_ENC);
					processor.resize(image.getContentAsInputStream(), imgSize.width, imgSize.height, imgData);
					ByteArrayInputStream input = new ByteArrayInputStream(imgData.toByteArray());
					cache.saveImage(image, cache.ORIG, input);
					imgData.reset();

					LOGGER.info("Image " + image.getName() + " cached successfull under the name " + cache.ORIG + " !");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			else
				LOGGER.info("Image " + image.getName() + " as version " + cache.ORIG + " allready exists in Cache!");
			// **************

			if ((imgSize.width > 2 * 1024 || imgSize.height > 2 * 768) && !cache.existInCache(image, cache.CACHE))
				try {
					processor.useEncoder(processor.TIFF_ENC);
					processor.resize(image.getContentAsInputStream(), 1024, 768, imgData);
					ByteArrayInputStream input = new ByteArrayInputStream(imgData.toByteArray());
					cache.saveImage(image, cache.CACHE, input);
					imgData.reset();

					LOGGER.info("Image " + image.getName() + " cached successfull under the name " + cache.CACHE + " !");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			else
				LOGGER.info("Image " + image.getName() + " as version " + cache.CACHE + " allready exists in Cache!");
			// **************

		}

		LOGGER.info("Caching supported images from derivate " + derivate.getName() + " finished successfull!");
	}
	
	public static void test(String ID) throws IOException{
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
//			imgService.getImage(image, new Dimension(600, 300), bout);
			proz.getImageSize(image.getContentAsInputStream());
			timer.stop();
			LOGGER.info("Timer get size: " + timer.getElapsedTime());
			
			timer.reset();
			timer.start();
//			imgService.getImage(image, new Dimension(600, 300), bout);
			imgService.getImage(image, 300, 400, 600, 300, 0.4F, bout);
			timer.stop();
			LOGGER.info("Timer: " + timer.getElapsedTime());
		}
	}
}
