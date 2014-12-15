package org.mycore.iview2.events;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.ifs2.MCRFile;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.iview2.frontend.MCRIView2Commands;
import org.mycore.iview2.services.MCRTilingQueue;

/**
 * Handles {@link MCRFile} events to keep image tiles up-to-date.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRImageTileEventHandler extends MCREventHandlerBase {

    MCRTilingQueue tq = MCRTilingQueue.getInstance();

    /**
     * creates image tiles if <code>file</code> is an image file.
     */
    @Override
    public void handlePathCreated(MCREvent evt, Path file, BasicFileAttributes attrs) {
        if (!(file instanceof MCRPath)) {
            return;
        }
        try {
            MCRIView2Commands.tileImage(MCRPath.toMCRPath(file));
        } catch (IOException e) {
            throw new MCRException(e);
        }
    }

    /**
     * deletes image tiles for <code>file</code>.
     */
    @Override
    public void handlePathDeleted(MCREvent evt, Path file, BasicFileAttributes attrs) {
        if (!(file instanceof MCRPath)) {
            return;
        }
        MCRPath path = MCRPath.toMCRPath(file);
        try {
            MCRIView2Commands.deleteImageTiles(path.getOwner(), path.getOwnerRelativePath());
        } catch (IOException e) {
            throw new MCRException(e);
        }
    }

    /**
     * updates image times if <code>file</code> is an image file.
     */
    @Override
    public void handlePathUpdated(MCREvent evt, Path file, BasicFileAttributes attrs) {
        handlePathDeleted(evt, file, attrs);
        handlePathCreated(evt, file, attrs);
    }

}
