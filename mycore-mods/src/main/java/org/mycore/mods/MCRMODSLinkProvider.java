package org.mycore.mods;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

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
        if (!linkingNodes.isEmpty()) {
            for (Element linkingNode : linkingNodes) {
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
