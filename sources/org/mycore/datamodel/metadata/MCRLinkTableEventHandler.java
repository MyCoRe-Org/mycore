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

package org.mycore.datamodel.metadata;

import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.classifications.MCRCategoryItem;
import org.mycore.datamodel.classifications.MCRClassificationItem;

/**
 * This class manages all operations of the LinkTables for operations of an
 * object.
 * 
 * @author Jens Kupferschmidt
 */
public class MCRLinkTableEventHandler extends MCREventHandlerBase {

    static MCRLinkTableManager mcr_linktable = MCRLinkTableManager.instance();

    /**
     * This method add the data to the link and classification table via
     * MCRLinkTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected final void handleObjectCreated(MCREvent evt, MCRObject obj) {
        // delete old entries
        handleObjectDeleted(evt, obj);
        // set new entries
        MCRObjectID mcr_id = obj.getId();
        MCRObjectMetadata meta = obj.getMetadata();
        MCRMetaElement elm = null;
        MCRMetaInterface inf = null;
        String classID, categID, destination;
        for (int i = 0; i < meta.size(); i++) {
            elm = meta.getMetadataElement(i);
            for (int j = 0; j < elm.size(); j++) {
                inf = elm.getElement(j);
                if (inf instanceof MCRMetaClassification) {
                    classID = ((MCRMetaClassification) inf).getClassId();
                    categID = ((MCRMetaClassification) inf).getCategId();
                    MCRClassificationItem classification = MCRClassificationItem.getClassificationItem(classID);
                    if (classification != null) {
                        MCRCategoryItem categ = classification.getCategoryItem(categID);
                        if (categ != null) {
                            continue;
                        }
                    }
                    MCRActiveLinkException activeLink = new MCRActiveLinkException("Failure while adding link!. Destination does not exist.");
                    destination = classID + "##" + categID;
                    activeLink.addLink(mcr_id.toString(), destination);
                    // throw activeLink;
                    // TODO: should trigger undo-Event
                }
                if (inf instanceof MCRMetaLinkID) {
                    destination = ((MCRMetaLinkID) inf).getXLinkHref();
                    if (!MCRXMLTableManager.instance().exist(new MCRObjectID(destination))) {
                        continue;
                    }
                    MCRActiveLinkException activeLink = new MCRActiveLinkException("Failure while adding link!. Destination does not exist.");
                    activeLink.addLink(mcr_id.toString(), destination);
                    // throw activeLink;
                    // TODO: should trigger undo-Event
                }
            }
        }
        for (int i = 0; i < meta.size(); i++) {
            elm = meta.getMetadataElement(i);
            for (int j = 0; j < elm.size(); j++) {
                inf = elm.getElement(j);
                if (inf instanceof MCRMetaClassification) {
                    mcr_linktable.addClassificationLink(mcr_id, new MCRObjectID(((MCRMetaClassification) inf).getClassId()), ((MCRMetaClassification) inf)
                            .getCategId());
                    continue;
                }
                if (inf instanceof MCRMetaLinkID) {
                    mcr_linktable.addReferenceLink(MCRLinkTableManager.TYPE_HREF, mcr_id.toString(), ((MCRMetaLink) inf).getXLinkHref(),mcr_linktable.ENTRY_TYPE_REFERNCE);
                    continue;
                }
            }
        }
    }

    /**
     * This method update the data to the link and classification table via
     * MCRLinkTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected final void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        //handleObjectDeleted(evt, obj);
        handleObjectCreated(evt, obj);
    }

    /**
     * This method delete the data from the link and classification table via
     * MCRLinkTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected final void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        MCRObjectID mcr_id = obj.getId();
        mcr_linktable.deleteReferenceLink("href", mcr_id);
        mcr_linktable.deleteClassificationLink(mcr_id);
    }

    /**
     * This method repair the data from the link and classification table via
     * MCRLinkTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected final void handleObjectRepair(MCREvent evt, MCRObject obj) {
        //handleObjectDeleted(evt, obj);
        handleObjectCreated(evt, obj);
    }

}
