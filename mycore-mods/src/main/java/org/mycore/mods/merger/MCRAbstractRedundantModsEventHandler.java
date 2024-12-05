package org.mycore.mods.merger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSSorter;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.mods.classification.MCRClassMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks for and removes redundant classifications in Mods-Documents. If a classification category and
 * the classification's child category are both present in the document, the parent classification will
 * be removed. The processed document will be finally be sorted using {@link MCRMODSSorter}.<br><br>
 * This abstract class can be extended by overriding the following abstract methods:
 * <ol>
 *     <li>{@link MCRAbstractRedundantModsEventHandler#isConsistent(Element, Element)}: custom checks for
 *     the two classifications' consistency, default return-value is true.</li>
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

    protected void mergeCategories(MCRObject obj) {
        MCRMODSWrapper mcrmodsWrapper = new MCRMODSWrapper(obj);
        if (mcrmodsWrapper.getMODS() == null) {
            return;
        }
        logger.info("merge redundant " + getClassificationElementName() + " categories for {}", obj.getId());

        Element mods = mcrmodsWrapper.getMODS();
        List<Element> supportedElements = getAllDescendants(mods).stream()
            .filter(element -> element.getName().equals(getClassificationElementName()))
            .filter(element -> MCRClassMapper.getCategoryID(element) != null)
            .toList();
        dropRedundantCategories(supportedElements);

        List<Element> relatedItems = getAllRelatedItems(mods);
        for (Element relatedItem : relatedItems) {
            if (relatedItem.getAttribute("href", MCRConstants.XLINK_NAMESPACE) == null) {
                dropRedundantCategories(getAllDescendants(relatedItem));
            }
        }
        MCRMODSSorter.sort(mods);
    }

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
        return mods.getChildren().stream()
            .filter(child -> "relatedItem".equals(child.getName()))
            .toList();
    }

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
     * Method can be overridden to implement custom checks to two categories' consistency regarding attributes.
     * @param element1 the first element to be compared
     * @param element2 the first element to be compared
     * @return will always return true
     */
    protected abstract boolean isConsistent(Element element1, Element element2);

    protected abstract String getClassificationElementName();
}
