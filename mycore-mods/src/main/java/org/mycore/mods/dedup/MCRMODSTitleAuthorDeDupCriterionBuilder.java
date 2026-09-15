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
import org.mycore.common.MCRConstants;
import org.mycore.dedup.MCRDeDupCriterion;
import org.mycore.mods.merger.MCRTextNormalizer;

/**
 * Builds combined title/author deduplication criteria from a MODS document. For every combination of
 * a {@code mods:titleInfo} and a personal author's family name a criterion is built, so both the title
 * and the author have to match together to identify a possible duplicate. Titles and authors are
 * normalized via {@link MCRTextNormalizer} to be fault-tolerant against accents, umlauts, case and
 * punctuation.
 */
public class MCRMODSTitleAuthorDeDupCriterionBuilder extends MCRMODSDeDupCriterionBuilder {

    /** The type of the criteria built by this builder. */
    public static final String CRITERION_TYPE = "title-author";

    @Override
    public Set<MCRDeDupCriterion> buildFromMODS(Element mods) {
        Set<MCRDeDupCriterion> criteria = new HashSet<>();
        for (Element title : getNodes(mods, "mods:titleInfo")) {
            for (Element name : getNodes(mods, "mods:name[@type='personal']/mods:namePart[@type='family']")) {
                criteria.add(buildFromTitleAuthor(getCombinedTitle(title), name.getTextTrim()));
            }
        }
        return criteria;
    }

    /**
     * Builds a combined title/author criterion. Both title and author are normalized so that both must
     * match together to identify a possible duplicate.
     *
     * @param title  the title text
     * @param author the author's family name
     * @return the deduplication criterion
     */
    public MCRDeDupCriterion buildFromTitleAuthor(String title, String author) {
        String normalizedTitle = MCRTextNormalizer.normalizeText(title);
        String normalizedAuthor = MCRTextNormalizer.normalizeText(author);
        return new MCRDeDupCriterion(CRITERION_TYPE, normalizedAuthor + ": " + normalizedTitle);
    }

    /**
     * Returns the combined text of a {@code mods:title} and its optional {@code mods:subTitle}.
     */
    private String getCombinedTitle(Element titleInfo) {
        String mainTitle = titleInfo.getChildTextTrim("title", MCRConstants.MODS_NAMESPACE);
        String subTitle = titleInfo.getChildTextTrim("subTitle", MCRConstants.MODS_NAMESPACE);
        return (mainTitle == null ? "" : mainTitle) + (subTitle == null ? "" : " " + subTitle);
    }
}
