/*
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.mods.merger;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.MCRConfiguration;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRAbstractMerger extends MCRMerger {

    /** Maximum Levenshtein distance to accept two abstracts as equal, in percent */
    private static final int MAX_DISTANCE_PERCENT = MCRConfiguration.instance().getInt("MCR.MODS.Merger.AbstractMerger.MaxDistancePercent");

    /** Maximum number of characters to compare from two abstracts */
    private static final int MAX_COMPARE_LENGTH = MCRConfiguration.instance().getInt("MCR.MODS.Merger.AbstractMerger.MaxCompareLength");

    private String text;

    public void setElement(Element element) {
        super.setElement(element);
        text = MCRTextNormalizer.normalizeText(element.getText());
        text += element.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE, "");
        text = text.substring(0, Math.min(text.length(), MAX_COMPARE_LENGTH));
    }

    @Override
    public boolean isProbablySameAs(MCRMerger other) {
        if (!(other instanceof MCRAbstractMerger))
            return false;

        String textOther = ((MCRAbstractMerger) other).text;
        int length = Math.min(text.length(), textOther.length());
        int distance = StringUtils.getLevenshteinDistance(text, textOther);
        System.out.println(distance);
        return (distance * 100 / length) < MAX_DISTANCE_PERCENT;
    }
}
