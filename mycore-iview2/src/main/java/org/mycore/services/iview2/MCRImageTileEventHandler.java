package org.mycore.services.iview2;

import java.util.Date;

import org.apache.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.frontend.iview2.MCRIView2Commands;

public class MCRImageTileEventHandler extends  MCREventHandlerBase {
	
	MCRTilingQueue tq = MCRTilingQueue.getInstance();
	private static Logger LOGGER = Logger.getLogger(MCRImageTileEventHandler.class);
	
	//tilingProg tp = null;	

	
	public void handleFileCreated(MCREvent evt, MCRFile file){
	    
	    MCRIView2Commands.tileImage(file);
	    
			
			//JavaIO.startTiling(file); //Startet unser(!) Kachelprogramm		
			/*String storeID = file.getStoreID();
			String baseDirName = MCRConfiguration.instance().getString(
					"MCR.IFS.ContentStore." + storeID + ".URI");
			MCRImage image = new MCRImage(MCRFile.getFile(tq.poll().getDerivate()));	
			*/
			//Speicherort f�r die Kacheln des Bildes
			/*String filePath = file.getAbsolutePath();
			filePath = filePath.substring(filePath.lastIndexOf("/"), filePath.lastIndexOf("."));
	        image.setOutputDirectory(new File(baseDirName + filePath));*/
			/*String path = file.getAbsolutePath().substring(
    				file.getAbsolutePath().lastIndexOf("/")+1, 
    				file.getAbsolutePath().lastIndexOf("."));
			LOGGER.warn("BaseDir " + baseDirName);
	        //image.setOutputDirectory(new File(baseDirName + "/" + file.getOwnerID() + "/" + path));
			image.setOutputDirectory(new File("/afs/rz.uni-jena.de/home/j/jo38fug/thulb-workspace/docportal/build/webapps/images/Pics/" + file.getOwnerID() + "/" + path));
			image.setPath(path);
			
	        try {
	            image.tile();
	            System.out.println("im Try-Block");
	        } catch (IOException e) {
	        	System.out.println("im Catch-Block");
	            e.printStackTrace();
	        }*/
	        
		//tilingProg.create();
	}
	
	public void handleFileDeleted(MCREvent evt, MCRFile file){
		tq.remove(file);
	}
	
	public void handleFileUpdated(MCREvent evt, MCRFile file){
		MCRTileJob job = new MCRTileJob();
		job.setDerivate(file.getID());
		job.setStart(new Date(System.currentTimeMillis()));
		tq.updateJob(job);
	}	

}
