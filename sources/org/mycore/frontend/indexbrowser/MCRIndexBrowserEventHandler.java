/*
 * 
 * $Revision: 13085 $ $Date: 2008-02-06 18:27:24 +0100 (Mi, 06. Feb 2008) $
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

package org.mycore.frontend.indexbrowser;

import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * This class implements an event handler that reacts on object modify / create /
 * delete events. It basically clears the different classification caches to
 * make sure the number of documents for each classification item is displayed
 * correctly. This has also an impact to the question if a classification item
 * can be expanded to display subitems.
 * 
 * @author Robert Stephan
 */
public class MCRIndexBrowserEventHandler extends MCREventHandlerBase {

    /**
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected final void handleObjectCreated(MCREvent evt, MCRObject obj) {
        MCRIndexBrowserData.deleteIndexCacheOfObjectType(getObjectType(obj));
        MCRIndexBrowserCache.deleteIndexCacheFromHashtable(getObjectType(obj));
    }

    private String getObjectType(MCRObject obj) {
        String typeID = obj.getId().getTypeId();
        return typeID;
    }

    /**
     * This method update the data to SQL table of XML data via
     * MCRXMLTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected final void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        MCRIndexBrowserData.deleteIndexCacheOfObjectType(getObjectType(obj));
        MCRIndexBrowserCache.deleteIndexCacheFromHashtable(getObjectType(obj));
    }

    /**
     * This method delete the XML data from SQL table data via
     * MCRXMLTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected final void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        MCRIndexBrowserData.deleteIndexCacheOfObjectType(getObjectType(obj));
        MCRIndexBrowserCache.deleteIndexCacheFromHashtable(getObjectType(obj));
    }

}
