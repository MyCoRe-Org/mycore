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
 * Builds deduplication criteria from the {@code mods:location/mods:shelfLocator} elements of a MODS
 * document. Two objects sharing a shelfmark are considered possible duplicates.
 */
public class MCRMODSShelfmarkDeDupCriterionBuilder extends MCRMODSDeDupCriterionBuilder {

    /** The type of the criteria built by this builder. */
    public static final String CRITERION_TYPE = "shelfmark";

    @Override
    public Set<MCRDeDupCriterion> buildFromMODS(Element mods) {
        Set<MCRDeDupCriterion> criteria = new HashSet<>();
        for (Element shelfmark : getNodes(mods, "mods:location/mods:shelfLocator")) {
            String value = shelfmark.getTextTrim();
            if (!value.isEmpty()) {
                criteria.add(new MCRDeDupCriterion(CRITERION_TYPE, value));
            }
        }
        return criteria;
    }
}
