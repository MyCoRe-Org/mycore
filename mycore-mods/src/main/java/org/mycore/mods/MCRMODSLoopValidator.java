package org.mycore.mods;

import static org.mycore.mods.MCRMODSWrapper.LINKED_RELATED_ITEMS;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRXlink;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.validator.MCRObjectValidator;
import org.mycore.datamodel.metadata.validator.MCRValidationResult;

/**
 * Validates the hierarchy of mods:relatedItem elements in MODS objects and checks for loops.
 */
public class MCRMODSLoopValidator extends MCRObjectValidator {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public MCRValidationResult validate(MCRObject object) {
        return checkHierarchyOfObject(object, object.getId());
    }

    /**
     * Recursivly checks <code>relatedItem</code> and parent &lt;mods:relatedItem&gt; elements for multiple {@link MCRObjectID}s.
     * @param relatedItem &lt;mods:relatedItem&gt;
     * @param checkObject the Object which is being validated
     * @throws MCRPersistenceException if {@link MCRObjectID} of <code>relatedItem</code> is in <code>idCollected</code>
     */
    private MCRValidationResult checkHierarchyOfRelatedItem(Element relatedItem, MCRObjectID checkObject)
        throws MCRPersistenceException {
        final String href = relatedItem.getAttributeValue(MCRXlink.HREF, MCRConstants.XLINK_NAMESPACE);
        if (Objects.isNull(href)) {
            /* TODO: can there be related items without href, which contain other related items, which have href? 
                If yes, then the children related items should be checked.
            */
            return MCRValidationResult.VALID;
        }

        LOGGER.debug("Checking relatedItem {}.", href);
        if (MCRObjectID.isValid(href)) {
            final MCRObjectID relatedItemId = MCRObjectID.getInstance(href);
            LOGGER.debug("Checking if {} is in {}.", relatedItemId, checkObject);
            if (checkObject.equals(relatedItemId)) {
                return new MCRMODSLoopFoundResult(Stream.of(checkObject, relatedItemId).collect(Collectors.toSet()));
            }

            MCRObject childRelatedItem = MCRMetadataManager.retrieveMCRObject(relatedItemId);
            return checkHierarchyOfObject(childRelatedItem, checkObject);
        } else {
            return new MCRMODSInvalidLink(href);
        }
    }

    private MCRValidationResult checkHierarchyOfObject(MCRObject modsObject, MCRObjectID checkObject)
        throws MCRPersistenceException {
        final MCRMODSWrapper mods = new MCRMODSWrapper(modsObject);
        final List<Element> relatedItemLeaves = mods.getElements("./" + LINKED_RELATED_ITEMS);

        return relatedItemLeaves.stream().map(e -> checkHierarchyOfRelatedItem(e, checkObject))
            .filter(Predicate.not(MCRValidationResult::isValid))
            .findAny()
            .orElse(MCRValidationResult.VALID);
    }

    private static final class MCRMODSLoopFoundResult extends MCRValidationResult {
        private final Set<MCRObjectID> ids;

        MCRMODSLoopFoundResult(Set<MCRObjectID> ids) {
            this.ids = ids;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public String getMessage() {
            return "Hierarchy of mods:relatedItem contains circuits by objects " + ids;
        }
    }

    private static final class MCRMODSInvalidLink extends MCRValidationResult {
        private final String href;

        MCRMODSInvalidLink(String href) {
            this.href = href;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public String getMessage() {
            return "Invalid xlink:href " + href;
        }
    }
}
