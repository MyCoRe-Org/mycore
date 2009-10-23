package org.mycore.services.iview2;

import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.frontend.iview2.MCRIView2Commands;

public class MCRImageTileEventHandler extends MCREventHandlerBase {

    MCRTilingQueue tq = MCRTilingQueue.getInstance();
    public void handleFileCreated(MCREvent evt, MCRFile file) {

        MCRIView2Commands.tileImage(file);
    }

    public void handleFileDeleted(MCREvent evt, MCRFile file) {
        MCRIView2Commands.deleteImageTiles(file.getOwnerID(), file.getAbsolutePath());
    }

    public void handleFileUpdated(MCREvent evt, MCRFile file) {
        handleFileDeleted(evt, file);
        handleFileCreated(evt, file);
    }

}
