package org.mycore.iview2.events;

import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.iview2.frontend.MCRIView2Commands;
import org.mycore.iview2.services.MCRTilingQueue;

public class MCRImageTileEventHandler extends MCREventHandlerBase {

    MCRTilingQueue tq = MCRTilingQueue.getInstance();
    @Override
    public void handleFileCreated(MCREvent evt, MCRFile file) {

        MCRIView2Commands.tileImage(file);
    }

    @Override
    public void handleFileDeleted(MCREvent evt, MCRFile file) {
        MCRIView2Commands.deleteImageTiles(file.getOwnerID(), file.getAbsolutePath());
    }

    @Override
    public void handleFileUpdated(MCREvent evt, MCRFile file) {
        handleFileDeleted(evt, file);
        handleFileCreated(evt, file);
    }

}
