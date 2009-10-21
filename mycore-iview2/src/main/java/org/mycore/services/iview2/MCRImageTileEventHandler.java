package org.mycore.services.iview2;

import java.util.Date;

import org.apache.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.frontend.iview2.MCRIView2Commands;

public class MCRImageTileEventHandler extends MCREventHandlerBase {

    MCRTilingQueue tq = MCRTilingQueue.getInstance();

    private static Logger LOGGER = Logger.getLogger(MCRImageTileEventHandler.class);

    //tilingProg tp = null;	

    public void handleFileCreated(MCREvent evt, MCRFile file) {

        MCRIView2Commands.tileImage(file);
    }

    public void handleFileDeleted(MCREvent evt, MCRFile file) {
        //TODO: remove tiles
        tq.remove(file);
    }

    public void handleFileUpdated(MCREvent evt, MCRFile file) {
        //TODO: remove tiles
        handleFileCreated(evt, file);
    }

}
