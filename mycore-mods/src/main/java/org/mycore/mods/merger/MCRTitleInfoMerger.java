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

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdom2.Element;
import org.mycore.common.MCRConstants;

/**
 * Compares and merges mods:titleInfo elements.
 * The normalized combined text of mods:nonSort, mods:title and mods:subTitle is compared.
 * Two titles are probably same if they are identical or one is prefix of the other.
 * When merging, the title that has a subtitle or the longer one wins.
 *
 * @author Frank Lützenkirchen
 */
public class MCRTitleInfoMerger extends MCRMerger {

    private String originalText;

    private String normalizedText;

    @Override
    public void setElement(Element element) {
        super.setElement(element);
        originalText = Stream.of(textOf("nonSort"), textOf("title"), textOf("subTitle"))
            .filter(s -> !s.isEmpty()).collect(Collectors.joining(" "));
        normalizedText = MCRTextNormalizer.normalizeText(originalText);
    }

    private String getType() {
        return this.element.getAttributeValue("type", "");
    }

    @Override
    public boolean isProbablySameAs(MCRMerger other) {
        if (!(other instanceof MCRTitleInfoMerger otherTitle)) {
            return false;
        }

        if (!this.getType().equals(((MCRTitleInfoMerger) other).getType())) {
            return false;
        }

        if (normalizedText.equals(otherTitle.normalizedText)) {
            return true;
        }
        return normalizedText.startsWith(otherTitle.normalizedText)
            || otherTitle.normalizedText.startsWith(normalizedText);
    }

    @Override
    public void mergeFrom(MCRMerger other) {
        mergeAttributes(other);

        MCRTitleInfoMerger otherTitle = (MCRTitleInfoMerger) other;

        boolean weHaveSubTitleOtherHasNot = !textOf("subTitle").isEmpty() && otherTitle.textOf("subTitle").isEmpty();
        boolean otherHasSubTitleAndWeNot = textOf("subTitle").isEmpty() && !otherTitle.textOf("subTitle").isEmpty();
        boolean otherTitleIsLonger = otherTitle.originalText.length() > this.originalText.length();

        if (!weHaveSubTitleOtherHasNot && (otherHasSubTitleAndWeNot || otherTitleIsLonger)) {
            this.element.setContent(other.element.cloneContent());
        }

    }

    private String textOf(String childName) {
        String text = element.getChildText(childName, MCRConstants.MODS_NAMESPACE);
        return text == null ? "" : text.trim();
    }

}
