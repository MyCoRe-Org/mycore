package org.mycore.mods.merger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.mods.classification.MCRClassMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Checks for and removes redundant classifications in Mods-Documents. If a classification category and
 * the classification's child category are both present in the document, the parent classification will
 * be removed. <br><br>
 * This abstract class can be extended by overriding the following abstract methods:
 * <ol>
 *     <li>{@link MCRAbstractRedundantModsEventHandler#isConsistent(Element, Element)}: custom checks for
 *     the two classifications' consistency.</li>
 *     <li>{@link MCRAbstractRedundantModsEventHandler#getClassificationElementName()}: the name of
 *     the mods-element that the EventHandler is checking duplicates for.</li>
 * </ol>
 */
public abstract class MCRAbstractRedundantModsEventHandler extends MCREventHandlerBase {

    private final Logger logger = LogManager.getLogger(getClass());

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        mergeCategories(obj);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        mergeCategories(obj);
    }

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        mergeCategories(obj);
    }

    /**
     * Merging classifications by detaching parent-categories inside an {@link MCRObject}.
     * Mods-element is traversed for classifications, found relatedItems are processed separately from
     * the rest of the document.
     * @param obj the handled object
     */
    protected void mergeCategories(MCRObject obj) {
        MCRMODSWrapper mcrmodsWrapper = new MCRMODSWrapper(obj);
        if (mcrmodsWrapper.getMODS() == null) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("merge redundant " + getClassificationElementName() + " categories for {}", obj.getId());
        }

        Element mods = mcrmodsWrapper.getMODS();
        List<Element> supportedElements = getAllDescendants(mods).stream()
            .filter(element -> element.getName().equals(getClassificationElementName()))
            .filter(element -> MCRClassMapper.getCategoryID(element) != null).toList();
        dropRedundantCategories(supportedElements);

        List<Element> relatedItems = getAllRelatedItems(mods);
        for (Element relatedItem : relatedItems) {
            if (relatedItem.getAttribute("href", MCRConstants.XLINK_NAMESPACE) == null) {
                dropRedundantCategories(getAllDescendants(relatedItem));
            }
        }
    }

    /**
     * Recursively writes all child-elements of a given element into a list and returns the list once completed.
     * @param element the parent element for which all children should be listed
     * @return a list with all child-elements
     */
    protected static List<Element> getAllDescendants(Element element) {
        List<Element> descendants = new ArrayList<>();

        for (Element child : element.getChildren()) {
            if (!child.getName().equals("relatedItem")) {
                descendants.add(child);
                descendants.addAll(getAllDescendants(child));
            }
        }
        return descendants;
    }

    /**
     * Returns all relatedItem-Elements from a mods-Element. Assumes that relatedItems are only used at top-level.
     * @param mods The mods-Element to be searched for relatedItems
     * @return a List of all Elements with the name "relatedItem"
     */
    protected static List<Element> getAllRelatedItems(Element mods) {
        return mods.getChildren().stream().filter(child -> "relatedItem".equals(child.getName())).toList();
    }

    /**
     * Iterates through a list of classification elements and for each element pair checks if one of the element
     * is a parent category of the other. Calls
     * {@link MCRAbstractRedundantModsEventHandler#isConsistent(Element, Element)} and only detaches parent element
     * if the method returns true.
     * @param elements a list of classification elements that are all compared to each other in pairs
     */
    protected void dropRedundantCategories(List<Element> elements) {
        for (int i = 0; i < elements.size(); i++) {
            for (int j = i + 1; j < elements.size(); j++) {

                Element element1 = elements.get(i);
                Element element2 = elements.get(j);
                Element parentElement = MCRCategoryMerger.getElementWithParentCategory(element1, element2);
                if (parentElement != null && isConsistent(element1, element2)) {
                    parentElement.detach();
                }
            }
        }
    }

    /**
     * Parses the authority name from an element.
     * @param element the element using an authority
     * @return the String of the authority
     */
    protected String getAuthority(Element element) {
        return element.getAttributeValue("authorityURI") != null ?
               element.getAttributeValue("authorityURI") : element.getAttributeValue("authority");
    }

    /**
     * Compares two classification elements for the same authority
     * @param el1 first element to be compared
     * @param el2 second element to be compared
     * @return true if both have the same authority, or if none of them has an authority
     */
    protected boolean hasSameAuthority(Element el1, Element el2) {
        return Objects.equals(el1.getAttributeValue("authorityURI"),
            el2.getAttributeValue("authorityURI")) &&
            Objects.equals(el1.getAttributeValue("authority"), el2.getAttributeValue("authority"));
    }

    /**
     * Get the name of an element's classification or return value "unknown".
     * @param element the element that contains the classification
     * @return the name of the classification or the string "unknown"
     */
    protected String getClassificationName(Element element) {
        return Optional.ofNullable(MCRClassMapper.getCategoryID(element)).map(MCRCategoryID::toString)
            .orElse("unknown");
    }

    /**
     * Method can be overridden to implement custom checks to two categories' consistency regarding attributes.
     * @param element1 the first element to be compared
     * @param element2 the first element to be compared
     * @return will always return true
     */
    protected abstract boolean isConsistent(Element element1, Element element2);

    /**
     * Returns the name of the classification element that the specific EventHandler is handling.
     * @return name of the classification element
     */
    protected abstract String getClassificationElementName();
}
