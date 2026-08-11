/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.mods;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import javax.naming.OperationNotSupportedException;

import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRXlink;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRDefaultLinkProvider;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This class is used to extract {@link org.mycore.datamodel.common.MCRLinkTableManager.MCRLinkReference}s and
 * {@link MCRCategoryID}s from MODS objects. It replaces the old MCRMODSLinksEventHandler.
 */
public class MCRMODSLinkProvider extends MCRDefaultLinkProvider {

    @Override
    public Collection<MCRLinkTableManager.MCRLinkReference> getLinksOfObject(MCRObject obj)
        throws OperationNotSupportedException {
        checkObjectType(obj);
        Collection<MCRLinkTableManager.MCRLinkReference> linksOfObject = new HashSet<>(super.getLinksOfObject(obj));
        MCRMODSWrapper modsWrapper = new MCRMODSWrapper(obj);

        List<Element> linkingNodes = modsWrapper.getLinkedRelatedItems();
        List<Element> linkingPersons = modsWrapper.getLinkedPersons();
        List<Element> joinedNodes = Stream.concat(linkingNodes.stream(), linkingPersons.stream()).toList();
        if (!joinedNodes.isEmpty()) {
            for (Element linkingNode : joinedNodes) {
                String targetID = linkingNode.getAttributeValue(MCRXlink.HREF, MCRConstants.XLINK_NAMESPACE);
                if (targetID == null) {
                    continue;
                }
                String relationshipTypeRaw = linkingNode.getAttributeValue("type");
                MCRMODSRelationshipType relType = MCRMODSRelationshipType.fromValue(relationshipTypeRaw);
                //MCR-1328 (no reference links for 'host')
                if (relType != MCRMODSRelationshipType.HOST) {
                    linksOfObject
                        .add(new MCRLinkTableManager.MCRLinkReference(obj.getId(), MCRObjectID.getInstance(targetID),
                            MCRLinkTableManager.ENTRY_TYPE_REFERENCE, relType.getValue()));
                }
            }
        }

        return linksOfObject;
    }

    @Override
    public Collection<MCRCategoryID> getCategoriesOfObject(MCRObject obj) throws OperationNotSupportedException {
        checkObjectType(obj);
        Collection<MCRCategoryID> categoriesOfObject = new HashSet<>(super.getCategoriesOfObject(obj));

        MCRMODSWrapper modsWrapper = new MCRMODSWrapper(obj);
        categoriesOfObject.addAll(modsWrapper.getMcrCategoryIDs());

        return categoriesOfObject;
    }

    private static void checkObjectType(MCRObject obj) throws OperationNotSupportedException {
        if (!MCRMODSWrapper.isSupported(obj)) {
            throw new OperationNotSupportedException("MCRMODSLinkProvider only supports mods types");
        }
    }
}
