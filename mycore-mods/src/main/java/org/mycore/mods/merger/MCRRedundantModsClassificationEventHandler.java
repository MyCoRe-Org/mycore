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

    private static final Logger LOGGER = LogManager.getLogger();

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
        return checkDisplayLabelConsistence(el1, el2);
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

        if (!Objects.equals(displayLabel1, displayLabel2)) {
            final String classificationName1 = getClassificationName(el1);
            final String classificationName2 = getClassificationName(el2);

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
