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

package org.mycore.mods.dedup;

import java.util.HashSet;
import java.util.Set;

import org.jdom2.Element;
import org.mycore.dedup.MCRDeDupCriterion;

/**
 * Builds deduplication criteria from the {@code mods:identifier} elements of a MODS document.
 * Two objects sharing an identifier of the same type and value are considered possible duplicates.
 */
public class MCRMODSIdentifierDeDupCriterionBuilder extends MCRMODSDeDupCriterionBuilder {

    /** The type of the criteria built by this builder. */
    public static final String CRITERION_TYPE = "identifier";

    @Override
    public Set<MCRDeDupCriterion> buildFromMODS(Element mods) {
        Set<MCRDeDupCriterion> criteria = new HashSet<>();
        for (Element identifier : getNodes(mods, "mods:identifier")) {
            String value = identifier.getTextTrim();
            if (!value.isEmpty()) {
                criteria.add(buildFromIdentifier(identifier.getAttributeValue("type"), value));
            }
        }
        return criteria;
    }

    /**
     * Builds a deduplication criterion for a given identifier type and value. The value is normalized
     * by removing hyphens so that for example {@code 978-1-56619-909-4} and {@code 9781566199094} match.
     *
     * @param type  the identifier type, e.g. {@code doi}, {@code isbn}, {@code issn}, {@code urn}
     * @param value the identifier value
     * @return the deduplication criterion
     */
    public MCRDeDupCriterion buildFromIdentifier(String type, String value) {
        String normalizedValue = value.replace("-", "");
        return new MCRDeDupCriterion(CRITERION_TYPE, type + ':' + normalizedValue);
    }
}
