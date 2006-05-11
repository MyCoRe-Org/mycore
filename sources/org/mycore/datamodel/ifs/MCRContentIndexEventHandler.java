/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.datamodel.ifs;

import org.apache.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.metadata.MCRDerivate;

/**
 * This class manages all operations of the repair of index of all content files
 * of one derivate.
 * 
 * @author Jens Kupferschmidt
 */
public final class MCRContentIndexEventHandler extends MCREventHandlerBase {

    private static Logger logger = Logger.getLogger(MCRContentIndexEventHandler.class);

    /**
     * Handles derivate repair events. The method lokk over all files and
     * initialize a repair event for all files.
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRDerivate that caused the event
     */
    protected void handleDerivateRepaired(MCREvent evt, MCRDerivate der) {
        MCRDirectory rootifs = MCRDirectory.getRootDirectory(der.getId().getId());
        doForChildren(rootifs);
    }

    /**
     * This is a recursive method to start an event handler for each file.
     * 
     * @param thisnode
     *            a IFS nod (file or directory)
     */
    private final void doForChildren(MCRFilesystemNode thisnode) {
        if (thisnode instanceof MCRDirectory) {
            MCRFilesystemNode[] childnodes = ((MCRDirectory) thisnode).getChildren();
            for (int i = 0; i < childnodes.length; i++) {
                doForChildren(childnodes[i]);
            }
        } else {
            // handle events
            MCREvent evt = new MCREvent(MCREvent.FILE_TYPE, MCREvent.REPAIR_EVENT);
            evt.put("file", (MCRFile)thisnode);
            MCREventManager.instance().handleEvent(evt);
            String fn = ((MCRFile) thisnode).getAbsolutePath();
            logger.debug("repair file " + fn);

        }
    }

}
