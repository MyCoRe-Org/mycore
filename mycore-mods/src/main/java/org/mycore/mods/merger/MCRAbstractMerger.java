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

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRXlink;
import org.mycore.common.config.MCRConfiguration2;

/**
 * Compares and merges mods:abstract elements. The abstract text is normalized before comparing.
 * Two abstracts are regarded probably same
 * if their levenshtein distance is less than a configured percentage of the text length.
 * <p>
 * MCR.MODS.Merger.AbstractMerger.MaxDistancePercent=[Maximum levenshtein distance in percent]
 * MCR.MODS.Merger.AbstractMerger.MaxCompareLength=[Maximum number of characters to compare from the two abstracts]
 *
 * @author Frank Lützenkirchen
 */
public class MCRAbstractMerger extends MCRMerger {

    /** Maximum Levenshtein distance to accept two abstracts as equal, in percent */
    private static final int MAX_DISTANCE_PERCENT = MCRConfiguration2
        .getOrThrow("MCR.MODS.Merger.AbstractMerger.MaxDistancePercent", Integer::parseInt);

    /** Maximum number of characters to compare from two abstracts */
    private static final int MAX_COMPARE_LENGTH = MCRConfiguration2
        .getOrThrow("MCR.MODS.Merger.AbstractMerger.MaxCompareLength", Integer::parseInt);

    private String text;

    @Override
    public void setElement(Element element) {
        super.setElement(element);
        text = MCRTextNormalizer.normalizeText(element.getText());
        text += element.getAttributeValue(MCRXlink.HREF, MCRConstants.XLINK_NAMESPACE, "");
        text = text.substring(0, Math.min(text.length(), MAX_COMPARE_LENGTH));
    }

    /**
     *  Two abstracts are regarded probably same
     *  if their levenshtein distance is less than a configured percentage of the text length.
     */
    @Override
    public boolean isProbablySameAs(MCRMerger other) {
        if (!(other instanceof MCRAbstractMerger)) {
            return false;
        }

        String textOther = ((MCRAbstractMerger) other).text;
        int length = Math.min(text.length(), textOther.length());

        if (length == 0) {
            return false;
        }

        int distance = LevenshteinDistance.getDefaultInstance().apply(text, textOther);
        return (distance * 100 / length) < MAX_DISTANCE_PERCENT;
    }
}
