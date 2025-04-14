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

package org.mycore.frontend.xeditor.tracker;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.mycore.common.xml.MCRXPathBuilder;

/**
 * Removes an attribute from the edited xml, and tracks that change.  
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRRemoveAttribute extends MCRChange {

    private Element parent;

    private Attribute removedAttribute;

    public MCRRemoveAttribute(Attribute attribute) {
        this.message = "Removed attribute " + MCRXPathBuilder.buildXPath(attribute);
        this.parent = attribute.getParent();
        this.removedAttribute = attribute.detach();
    }

    @Override
    public void undo() {
        parent.setAttribute(removedAttribute);
    }
}
