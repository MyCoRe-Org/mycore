/*
 * $Id$
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

package org.mycore.mods;

import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.MCRMODSWrapper;

/**
 * Extracts occurences of mods:relatedItem and stores them as separate MCRObjects.
 * For mods:relatedItem/@type='host', sets the extracted object as parent.
 * Always, sets @xlink:href of mods:relatedItem to the extracted object's ID. 
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRExtractRelatedItemsEventHandler extends MCREventHandlerBase {

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleObjectCreated(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected void handleObjectCreated(final MCREvent evt, final MCRObject obj) {
        extractRelatedItems(evt, obj);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleObjectUpdated(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected void handleObjectUpdated(final MCREvent evt, final MCRObject obj) {
        extractRelatedItems(evt, obj);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleObjectRepaired(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected void handleObjectRepaired(final MCREvent evt, final MCRObject obj) {
        extractRelatedItems(evt, obj);
    }

    private final static Logger LOGGER = Logger.getLogger(MCRExtractRelatedItemsEventHandler.class);

    private void extractRelatedItems(final MCREvent evt, final MCRObject object) {
        if (!isMODS(object))
            return;

        Element mods = new MCRMODSWrapper(object).getMODS();
        MCRObjectID oid = object.getId();
        for (Element relatedItem : (List<Element>) (mods.getChildren("relatedItem", MCRConstants.MODS_NAMESPACE))) {
            String href = relatedItem.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
            LOGGER.info("Found related item in " + object.getId().toString() + ", href=" + href);
            if ((href == null) || href.isEmpty()) {
                MCRObjectID relatedID = createRelatedObject(relatedItem, oid);

                href = relatedID.toString();
                LOGGER.info("Setting href of related item to " + href);
                relatedItem.setAttribute("href", href, MCRConstants.XLINK_NAMESPACE);

                if (isParent(relatedItem)) {
                    LOGGER.info("Setting " + href + " as parent of " + oid);
                    object.getStructure().setParent(relatedID);
                }
            }
        }
    }

    private boolean isMODS(MCRObject obj) {
        return "mods".equals(obj.getId().getTypeId());
    }

    private boolean isParent(Element relatedItem) {
        return "host".equals(relatedItem.getAttributeValue("type"));
    }

    private MCRObjectID createRelatedObject(Element relatedItem, MCRObjectID childID) {
        MCRMODSWrapper wrapper = new MCRMODSWrapper();
        MCRObject object = wrapper.getMCRObject();
        MCRObjectID oid = MCRObjectID.getNextFreeId(childID.getBase());
        object.setId(oid);

        if (isParent(relatedItem)) {
            object.getStructure().addChild(new MCRMetaLinkID("child", childID, childID.toString(), childID.toString()));
        }

        Element mods = cloneRelatedItem(relatedItem);
        wrapper.setMODS(mods);

        LOGGER.info("create object " + oid.toString());
        MCRMetadataManager.create(object);
        return oid;
    }

    private Element cloneRelatedItem(Element relatedItem) {
        Element mods = (Element) (relatedItem.clone());
        mods.setName("mods");
        mods.removeAttribute("type");
        mods.removeChildren("part", MCRConstants.MODS_NAMESPACE);
        return mods;
    }
}
