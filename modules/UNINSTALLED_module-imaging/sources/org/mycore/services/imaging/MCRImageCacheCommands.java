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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.apache.log4j.Logger;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRClassificationCommands;
import org.mycore.frontend.cli.MCRCommand;

public class MCRImageCacheCommands extends MCRAbstractCommands{
	private static Logger LOGGER = Logger.getLogger(MCRClassificationCommands.class.getName());
	
	/**
     * The empty constructor.
     */
    public MCRImageCacheCommands() {
        super();
        MCRCommand com = null;

        com = new MCRCommand(
                "clear cache",
                "org.mycore.services.imaging.MCRImageCacheCommands.clearCache",
                "The command clear the Image cache.");
        command.add(com);
        
        com = new MCRCommand(
                "test Imaging Service",
                "org.mycore.services.imaging.MCRImageCacheCommands.testImageService",
                "The command test the Imaging Service.");
        command.add(com);
        
        /*com = new MCRCommand(
                "cacheMkdir {0}",
                "org.mycore.services.imaging.MCRImageCacheCommands.cacheMkdir String",
                "The command create the Directories in the Image cache.");
        command.add(com);*/
    }

    public static void clearCache() {
    	MCRDirectory.getRootDirectory("imgCache").delete();
    	LOGGER.info("Cache deleted!");
		
	}
    
    public static void testImageService() throws FileNotFoundException {
    		Stopwatch timer = new Stopwatch();
		Stopwatch timerAll = new Stopwatch();
    	
    		FileInputStream in = new FileInputStream("/home/chi/images/img/input.tif");
		BufferedInputStream input = new BufferedInputStream(in);
		FileOutputStream out = new FileOutputStream("/home/chi/images/img/output.jpg");
		BufferedOutputStream output = new BufferedOutputStream(out);
		
		timerAll.reset();
		timerAll.start();
		
		MCRFile imageFile = new MCRFile("foo", "foo");
		imageFile.setContentFrom(input);
		
		MCRImageService imageService = new MCRImageService();
		imageService.useCache(true);
		imageService.getThumbnail(imageFile, 100, 75, output);
		
		imageFile.delete();
		
		timerAll.stop();
		System.out.println("Total time: " + timerAll.getElapsedTime());
		
	}
}
