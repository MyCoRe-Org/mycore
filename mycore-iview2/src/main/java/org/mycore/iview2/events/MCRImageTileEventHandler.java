package org.mycore.iview2.events;

import java.io.IOException;

import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileEventHandlerBase;
import org.mycore.iview2.frontend.MCRIView2Commands;
import org.mycore.iview2.services.MCRTilingQueue;

/**
 * Handles {@link MCRFile} events to keep image tiles up-to-date.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRImageTileEventHandler extends MCRFileEventHandlerBase {

    MCRTilingQueue tq = MCRTilingQueue.getInstance();

    /**
     * creates image tiles if <code>file</code> is an image file.
     */
    @Override
    public void handleFileCreated(MCREvent evt, MCRFile file) {
        try {
            MCRIView2Commands.tileImage(file.toPath());
        } catch (IOException e) {
            throw new MCRException(e);
        }
    }

    /**
     * deletes image tiles for <code>file</code>.
     */
    @Override
    public void handleFileDeleted(MCREvent evt, MCRFile file) {
        try {
            MCRIView2Commands.deleteImageTiles(file.getOwnerID(), file.getAbsolutePath());
        } catch (IOException e) {
            throw new MCRException(e);
        }
    }

    /**
     * updates image times if <code>file</code> is an image file.
     */
    @Override
    public void handleFileUpdated(MCREvent evt, MCRFile file) {
        handleFileDeleted(evt, file);
        handleFileCreated(evt, file);
    }

}
