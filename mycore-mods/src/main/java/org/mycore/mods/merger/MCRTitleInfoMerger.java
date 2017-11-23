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
import org.mycore.common.MCRConstants;

/**
 * Compares and merges mods:titleInfo elements.
 * The normalized combined text of mods:nonSort, mods:title and mods:subTitle is compared.
 * Two titles are probably same if they are identical or one is prefix of the other.
 * When merging, the title that has a subtitle or the longer one wins.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRTitleInfoMerger extends MCRMerger {

    private String text;

    public void setElement(Element element) {
        super.setElement(element);

        text = textOf("nonSort") + " " + textOf("title") + " " + textOf("subTitle");
        text = MCRTextNormalizer.normalizeText(text.trim());
    }

    @Override
    public boolean isProbablySameAs(MCRMerger other) {
        if (!(other instanceof MCRTitleInfoMerger)) {
            return false;
        }

        MCRTitleInfoMerger otherTitle = (MCRTitleInfoMerger) other;
        if (text.equals(otherTitle.text)) {
            return true;
        }
        return text.startsWith(otherTitle.text) || otherTitle.text.startsWith(text);
    }

    public void mergeFrom(MCRMerger other) {
        mergeAttributes(other);

        MCRTitleInfoMerger otherTitle = (MCRTitleInfoMerger) other;

        boolean otherHasSubTitleAndWeNot = textOf("subTitle").isEmpty() && !otherTitle.textOf("subTitle").isEmpty();
        boolean otherTitleIsLonger = otherTitle.text.length() > this.text.length();

        if (otherHasSubTitleAndWeNot || otherTitleIsLonger) {
            this.element.setContent(other.element.cloneContent());
        }
    }

    private String textOf(String childName) {
        String text = element.getChildText(childName, MCRConstants.MODS_NAMESPACE);
        return text == null ? "" : text.trim();
    }
}
