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

/**
 * Merges MODS elements that must occur only once per type.
 * So if they have the same name and the same type attribute value,
 * they are regarded to represent the same information.
 *
 * @author Frank Lützenkirchen
 */
public class MCRUniqueTypeMerger extends MCRMerger {

    private String getType() {
        return this.element.getAttributeValue("type", "");
    }

    @Override
    public boolean isProbablySameAs(MCRMerger other) {
        if (!sameElementName(other)) {
            return false;
        }
        return other instanceof MCRUniqueTypeMerger typeMerger && this.getType().equals(typeMerger.getType());
    }
}
