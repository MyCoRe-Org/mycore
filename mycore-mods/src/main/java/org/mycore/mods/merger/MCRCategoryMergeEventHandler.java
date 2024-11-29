package org.mycore.mods.merger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSSorter;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.mods.classification.MCRClassMapper;

import java.util.List;
import java.util.ArrayList;

/**
 * Checks for and removes redundant classifications in Mods-Documents. If a classification category and
 * the classification's child category are both present in the document, the parent classification will
 * be removed. The processed document will be finally be sorted using {@link MCRMODSSorter}.
 */
public class MCRCategoryMergeEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger(MCRCategoryMergeEventHandler.class);

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

    public static List<Element> getAllDescendants(Element element) {
        List<Element> descendants = new ArrayList<>();

        for (Element child : element.getChildren()) {
            descendants.add(child);
            descendants.addAll(getAllDescendants(child));
        }

        return descendants;
    }

    private void mergeCategories(MCRObject obj) {
        MCRMODSWrapper mcrmodsWrapper = new MCRMODSWrapper(obj);
        if (mcrmodsWrapper.getMODS() == null) {
            return;
        }
        LOGGER.info("merge redundant classification categories for {}", obj.getId());

        Element filledMods = mcrmodsWrapper.getMODS();
        List<Element> supportedElements = getAllDescendants(filledMods).stream()
        //List<Element> supportedElements = filledMods.getChildren().stream()
            .filter(element -> MCRClassMapper.getCategoryID(element) != null).toList();

        for (int i = 0; i < supportedElements.size(); i++) {
            for (int j = i + 1; j < supportedElements.size(); j++) {

                Element element1 = supportedElements.get(i);
                Element element2 = supportedElements.get(j);
                Element parentElement = MCRCategoryMerger.getElementWithParentCategory(element1, element2);
                if (parentElement != null) {
                    parentElement.detach();
                }
            }
        }

        MCRMODSSorter.sort(filledMods);
    }
}
