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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRObjectReference;
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
        MCRObjectID mcr_id = obj.getId();
        mcr_linktable.deleteReferenceLink(mcr_id);
        MCRCategLinkServiceFactory.getInstance().deleteLinks(Collections.nCopies(1, obj.getId().toString()));
        // set new entries
        MCRObjectMetadata meta = obj.getMetadata();
        MCRMetaElement elm = null;
        MCRMetaInterface inf = null;
        if (false) {
            // TODO: add undo events
            checkLinkTargets(obj);
        }
        Collection<MCRCategoryID> categories = new ArrayList<MCRCategoryID>();
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
                    mcr_linktable.addReferenceLink(mcr_id.toString(), ((MCRMetaLink) inf).getXLinkHref(), MCRLinkTableManager.ENTRY_TYPE_REFERENCE, "");
                    continue;
                }
            }
        }
        if (categories.size() > 0) {
            MCRObjectReference objectReference = new MCRObjectReference(mcr_id.toString(), mcr_id.getTypeId());
            MCRCategLinkServiceFactory.getInstance().setLinks(objectReference, categories);
        }
        // delete all derivate references
        // NOTE: Derivates are deleted by handleObjectDeleted() above
        // mcr_linktable.deleteReferenceLink(obj.getId().toString(),MCRLinkTableManager.ENTRY_TYPE_DERIVATE,"");
        // add derivate referece
        MCRObjectStructure struct = obj.getStructure();
        int dersize = struct.getDerivateSize();
        for (int i = 0; i < dersize; i++) {
            MCRMetaLinkID lid = struct.getDerivate(i);
            mcr_linktable.addReferenceLink(obj.getId(), lid.getXLinkHrefID(), MCRLinkTableManager.ENTRY_TYPE_DERIVATE, "");
        }

    }

    private void checkLinkTargets(MCRObject obj) {
        MCRObjectID mcr_id = obj.getId();
        MCRObjectMetadata meta = obj.getMetadata();
        MCRMetaElement elm;
        MCRMetaInterface inf;
        for (int i = 0; i < meta.size(); i++) {
            elm = meta.getMetadataElement(i);
            for (int j = 0; j < elm.size(); j++) {
                inf = elm.getElement(j);
                if (inf instanceof MCRMetaClassification) {
                    String classID = ((MCRMetaClassification) inf).getClassId();
                    String categID = ((MCRMetaClassification) inf).getCategId();
                    boolean exists = MCRCategoryDAOFactory.getInstance().exist(new MCRCategoryID(classID, categID));
                    if (exists) {
                        continue;
                    }
                    MCRActiveLinkException activeLink = new MCRActiveLinkException("Failure while adding link!. Destination does not exist.");
                    String destination = classID + "##" + categID;
                    activeLink.addLink(mcr_id.toString(), destination);
                    // throw activeLink;
                    // TODO: should trigger undo-Event
                }
                if (inf instanceof MCRMetaLinkID) {
                    String destination = ((MCRMetaLinkID) inf).getXLinkHref();
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
        mcr_linktable.deleteReferenceLink(mcr_id);
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
    protected final void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        handleObjectCreated(evt, obj);
    }

}
