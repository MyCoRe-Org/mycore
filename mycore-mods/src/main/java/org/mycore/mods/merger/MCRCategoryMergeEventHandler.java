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

import java.util.List;

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

    private void mergeCategories(MCRObject obj) {
        MCRMODSWrapper mcrmodsWrapper = new MCRMODSWrapper(obj);
        if (mcrmodsWrapper.getMODS() == null) {
            return;
        }
        LOGGER.info("merge redundant classification categories for {}", obj.getId());

        Element filledMods = mcrmodsWrapper.getMODS();
        Element emtpyMods = new Element("mods", MCRConstants.MODS_NAMESPACE);

        List<Element> supportedElements = filledMods.getChildren().stream()
            .filter(element -> MCRClassMapper.getCategoryID(element) != null).toList();
        supportedElements.forEach(Element::detach);

        for (Element testedElement : supportedElements) {
            emtpyMods.addContent(testedElement);
            MCRMergeTool.merge(filledMods, emtpyMods);

            emtpyMods = new Element("mods", MCRConstants.MODS_NAMESPACE);
        }

        MCRMODSSorter.sort(filledMods);
    }
}
