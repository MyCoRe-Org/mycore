/*
 * 
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

package org.mycore.datamodel.common;

import java.util.Collection;
import java.util.HashSet;

import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRMetaInterface;
import org.mycore.datamodel.metadata.MCRMetaLink;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectMetadata;
import org.mycore.datamodel.metadata.MCRObjectStructure;

/**
 * This class manages all operations of the LinkTables for operations of an object.
 * 
 * @author Jens Kupferschmidt
 */
public class MCRLinkTableEventHandler extends MCREventHandlerBase {

    static MCRLinkTableManager mcr_linktable = MCRLinkTableManager.instance();

    /**
     * This method add the data to the link and classification table via MCRLinkTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    @Override
    protected final void handleObjectCreated(MCREvent evt, MCRObject obj) {
        MCRObjectID mcr_id = obj.getId();
        // set new entries
        MCRObjectMetadata meta = obj.getMetadata();
        MCRMetaElement elm = null;
        MCRMetaInterface inf = null;
        //use Set for category collection to remove duplicates if there are any
        Collection<MCRCategoryID> categories = new HashSet<MCRCategoryID>();
        for (int i = 0; i < meta.size(); i++) {
            elm = meta.getMetadataElement(i);
            for (int j = 0; j < elm.size(); j++) {
                inf = elm.getElement(j);
                if (inf instanceof MCRMetaClassification) {
                    String classId = ((MCRMetaClassification) inf).getClassId();
                    String categId = ((MCRMetaClassification) inf).getCategId();
                    categories.add(new MCRCategoryID(classId, categId));
                    continue;
                }
                if (inf instanceof MCRMetaLinkID) {
                    mcr_linktable.addReferenceLink(mcr_id.toString(), ((MCRMetaLink) inf).getXLinkHref(),
                        MCRLinkTableManager.ENTRY_TYPE_REFERENCE, "");
                }
            }
        }
        MCRCategoryID state = obj.getService().getState();
        if (state != null) {
            categories.add(state);
        }
        if (categories.size() > 0) {
            MCRCategLinkReference objectReference = new MCRCategLinkReference(mcr_id);
            MCRCategLinkServiceFactory.getInstance().setLinks(objectReference, categories);
        }
        // add derivate referece
        MCRObjectStructure struct = obj.getStructure();
        int dersize = struct.getDerivates().size();
        for (int i = 0; i < dersize; i++) {
            MCRMetaLinkID lid = struct.getDerivates().get(i);
            mcr_linktable.addReferenceLink(obj.getId(), lid.getXLinkHrefID(), MCRLinkTableManager.ENTRY_TYPE_DERIVATE,
                "");
        }
        // add parent reference
        if (struct.getParentID() != null) {
            mcr_linktable.addReferenceLink(mcr_id, struct.getParentID(), MCRLinkTableManager.ENTRY_TYPE_PARENT, "");
        }
    }

    private void deleteOldLinks(final MCRObjectID objectId) {
        mcr_linktable.deleteReferenceLink(objectId);
        MCRCategLinkReference reference = new MCRCategLinkReference(objectId);
        MCRCategLinkServiceFactory.getInstance().deleteLink(reference);
    }

    /**
     * This method update the data to the link and classification table via MCRLinkTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    @Override
    protected final void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        handleObjectRepaired(evt, obj);
    }

    /**
     * This method delete the data from the link and classification table via MCRLinkTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    @Override
    protected final void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        deleteOldLinks(obj.getId());
    }

    /**
     * This method repair the data from the link and classification table via MCRLinkTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    @Override
    protected final void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        handleObjectDeleted(evt, obj);
        handleObjectCreated(evt, obj);
    }

}
