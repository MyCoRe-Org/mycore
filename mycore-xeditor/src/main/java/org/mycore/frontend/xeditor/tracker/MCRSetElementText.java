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

import java.util.List;

import org.jdom2.Content;
import org.jdom2.Element;
import org.mycore.common.xml.MCRXPathBuilder;

/**
 * Sets an element's text in the edited xml, and tracks that change.  
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRSetElementText extends MCRChange {

    public Element element;

    public List<Content> oldContent;

    public MCRSetElementText(Element element, String newValue) {
        this.message = "Set value of " + MCRXPathBuilder.buildXPath(element) + " to " + newValue;
        this.element = element;
        this.oldContent = element.cloneContent();
        element.setText(newValue);
    }

    @Override
    public void undo() {
        element.setContent(oldContent);
    }
}
