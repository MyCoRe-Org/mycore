package org.mycore.mods.merger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;

import java.util.Objects;

/**
 * Checks for and removes redundant classifications in Mods-Documents. If a classification category and
 * the classification's child category are both present in the document, the parent classification will
 * be removed.
 */
public class MCRRedundantModsClassificationEventHandler extends MCRAbstractRedundantModsEventHandler {

    private static final Logger LOGGER = LogManager.getLogger(MCRRedundantModsClassificationEventHandler.class);

    protected static final String CLASSIFICATION_ELEMENT_NAME = "classification";

    @Override
    protected String getClassificationElementName() {
        return CLASSIFICATION_ELEMENT_NAME;
    }

    /**
     * Returns false if the authorities of the two classification elements are the same, but
     * the displayLabels differ from each other.
     * @param el1 the first element to be compared
     * @param el2 the first element to be compared
     * @return false if inconsistent
     */
    @Override
    protected boolean isConsistent(Element el1, Element el2) {
        return !hasSameAuthority(el1, el2) || checkDisplayLabelConsistence(el1, el2);
    }

    /**
     * Checks if both elements have the same displayLabel. Logs a warning if not.
     * @param el1 first element to check
     * @param el2 second element to check
     * @return true, if both elements have the same displayLabel (or both have none)
     */
    private boolean checkDisplayLabelConsistence(Element el1, Element el2) {
        final String displayLabel1 = el1.getAttributeValue("displayLabel");
        final String displayLabel2 = el2.getAttributeValue("displayLabel");

        final String classificationName1 = getClassificationName(el1);
        final String classificationName2 = getClassificationName(el2);

        if (!Objects.equals(displayLabel1, displayLabel2)) {

            String logMessage = """
                There are inconsistencies found between the classifications {} and {}.
                They have the same authority "{}" but {} has the displayLabel "{}" and {} has the displayLabel "{}".""";

            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(logMessage, classificationName1, classificationName2, getAuthority(el1),
                    classificationName1, displayLabel1, classificationName2, displayLabel2);
            }
            return false;
        }
        return true;
    }

}
