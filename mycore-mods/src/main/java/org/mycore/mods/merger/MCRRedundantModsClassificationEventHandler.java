package org.mycore.mods.merger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.mods.MCRMODSSorter;
import org.mycore.mods.classification.MCRClassMapper;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Checks for and removes redundant classifications in Mods-Documents. If a classification category and
 * the classification's child category are both present in the document, the parent classification will
 * be removed. The processed document will be finally be sorted using {@link MCRMODSSorter}.
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
        final boolean hasSameAuthority = Objects.equals(el1.getAttributeValue("authorityURI"),
            el2.getAttributeValue("authorityURI")) &&
            Objects.equals(el1.getAttributeValue("authority"), el2.getAttributeValue("authority"));
        final String displayLabel1 = el1.getAttributeValue("displayLabel");
        final String displayLabel2 = el2.getAttributeValue("displayLabel");

        if (hasSameAuthority && !Objects.equals(displayLabel1, displayLabel2)) {
            final String authorityName = el1.getAttributeValue("authorityURI") != null
                                         ? el1.getAttributeValue("authorityURI")
                                         : el1.getAttributeValue("authority");
            final MCRCategoryID cat1 = MCRClassMapper.getCategoryID(el1);
            final MCRCategoryID cat2 = MCRClassMapper.getCategoryID(el2);
            final String classificationName1 = cat1 != null ? cat1.toString() : "unknown";
            final String classificationName2 = cat2 != null ? cat2.toString() : "unknown";

            String logMessage = new MessageFormat("""
                There are inconsistencies found between the classifications {0} and {1}.
                They have the same authority "{2}", but {0} has the displayLabel "{3}" and {1}
                has the displayLabel "{4}".""", Locale.ROOT).format(new Object[] {classificationName1,
                classificationName2, authorityName, displayLabel1, displayLabel2});

            LOGGER.warn(logMessage);
            return false;
        }
        return true;
    }

}
