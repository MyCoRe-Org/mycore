package org.mycore.datamodel.common;

import static org.mycore.datamodel.common.MCRLinkTableManager.ENTRY_TYPE_DERIVATE;
import static org.mycore.datamodel.common.MCRLinkTableManager.ENTRY_TYPE_DERIVATE_LINK;
import static org.mycore.datamodel.common.MCRLinkTableManager.ENTRY_TYPE_REFERENCE;
import static org.mycore.datamodel.common.MCRLinkTableManager.MCRLinkReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.naming.OperationNotSupportedException;

import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaDerivateLink;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectMetadata;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.datamodel.metadata.MCRObjectStructure;

/**
 * <p>Default implementation of {@link MCRBaseLinkProvider} which provides the default behavior of MyCoRe for getting
 * {@link MCRLinkReference}s and {@link MCRCategoryID}s from objects and derivates.
 * <p>It extracts {@link MCRCategoryID} from {@link MCRMetaClassification}, {@link MCRObjectService#getState()} and
 * {@link MCRObjectDerivate#getClassifications()}.
 * <p>It extracts {@link MCRLinkReference}s from {@link MCRMetaLinkID}, {@link MCRMetaDerivateLink},
 * {@link MCRObjectStructure#getParentID()} and {@link MCRDerivate#getOwnerID()}.
 */
public class MCRDefaultLinkProvider implements MCRBaseLinkProvider {

    @Override
    public Collection<MCRCategoryID> getCategoriesOfObject(MCRObject obj) throws OperationNotSupportedException {
        MCRObjectMetadata meta = obj.getMetadata();
        Collection<MCRCategoryID> categories = new HashSet<>();
        meta.stream().flatMap(MCRMetaElement::stream).forEach(inf -> {
            if (inf instanceof MCRMetaClassification classification) {
                String classId = classification.getClassId();
                String categId = classification.getCategId();
                categories.add(new MCRCategoryID(classId, categId));
            }
        });
        MCRCategoryID state = obj.getService().getState();
        if (state != null) {
            categories.add(state);
        }
        categories.addAll(obj.getService().getClassifications());

        return categories;
    }

    @Override
    public Collection<MCRCategoryID> getCategoriesOfDerivate(MCRDerivate der) throws OperationNotSupportedException {
        Collection<MCRCategoryID> categoryList = der.getDerivate().getClassifications()
            .stream()
            .map(this::metaClassToCategoryID)
            .collect(Collectors.toSet());

        MCRCategoryID state = der.getService().getState();
        if (state != null) {
            categoryList.add(state);
        }

        return categoryList;
    }

    @Override
    public Collection<MCRLinkReference> getLinksOfObject(MCRObject obj) throws OperationNotSupportedException {
        List<MCRLinkReference> linkReferences = new ArrayList<>();
        MCRObjectID mcrId = obj.getId();
        // set new entries
        MCRObjectMetadata meta = obj.getMetadata();
        //use Set for category collection to remove duplicates if there are any
        meta.stream().flatMap(MCRMetaElement::stream).forEach(inf -> {
            if (inf instanceof MCRMetaLinkID linkID) {
                linkReferences.add(new MCRLinkReference(mcrId, linkID.getXLinkHrefID(), ENTRY_TYPE_REFERENCE, ""));
            } else if (inf instanceof MCRMetaDerivateLink derLink) {
                linkReferences.add(new MCRLinkReference(mcrId, MCRObjectID.getInstance(derLink.getXLinkHref()),
                    ENTRY_TYPE_DERIVATE_LINK, ""));
            }
        });

        // add derivate reference
        MCRObjectStructure structure = obj.getStructure();
        // add parent reference
        if (structure.getParentID() != null) {
            linkReferences.add(new MCRLinkReference(mcrId, structure.getParentID(), MCRLinkType.PARENT, ""));
        }

        return linkReferences;
    }

    @Override
    public Collection<MCRLinkReference> getLinksOfDerivate(MCRDerivate der) throws OperationNotSupportedException {
        return List.of(new MCRLinkReference(der.getId(), der.getOwnerID(), ENTRY_TYPE_DERIVATE, ""));
    }

    private MCRCategoryID metaClassToCategoryID(MCRMetaClassification metaClazz) {
        return new MCRCategoryID(metaClazz.getClassId(), metaClazz.getCategId());
    }
}
