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
 * Compares and merges mods:extent elements.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRExtentMerger extends MCRMerger {

    public void setElement(Element element) {
        super.setElement(element);
    }

    @Override
    public boolean isProbablySameAs(MCRMerger other) {
        return (other instanceof MCRExtentMerger);
    }

    private boolean hasStartPage() {
        return element.getChild("start", MCRConstants.MODS_NAMESPACE) != null;
    }

    @Override
    public void mergeFrom(MCRMerger other) {
        if (element.getParentElement().getName().equals("physicalDescription")) {
            super.mergeFrom(other);
        } else { // parent is "mods:part"
            if ((!this.hasStartPage()) && ((MCRExtentMerger) other).hasStartPage()) {
                mergeAttributes(other);
                this.element.setContent(other.element.cloneContent());
            }
        }
    }
}
