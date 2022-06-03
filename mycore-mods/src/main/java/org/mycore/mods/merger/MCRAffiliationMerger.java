/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import org.jdom2.Element;
import org.apache.commons.lang.StringUtils;
import org.mycore.common.config.MCRConfiguration2;

public class MCRAffiliationMerger extends MCRMerger {

    private static final int MAX_DISTANCE_PERCENT = MCRConfiguration2
        .getOrThrow("MCR.MODS.Merger.AffiliationMerger.MaxDistancePercent", Integer::parseInt);

    private String text;

    public void setElement(Element element) {
        super.setElement(element);
        text = MCRTextNormalizer.normalizeText(element.getText());
    }

    @Override
    public boolean isProbablySameAs(MCRMerger other) {
        if (!(other instanceof MCRAffiliationMerger)) {
            return false;
        }
        String textOther = ((MCRAffiliationMerger) other).text;
        int length = Math.min(text.length(), textOther.length());
        int distance = StringUtils.getLevenshteinDistance(text, textOther);
        if(length == 0) {
            return true;
        }
        return (distance * 100 / length) < MAX_DISTANCE_PERCENT;
    }
}
