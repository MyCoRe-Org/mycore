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

import org.jaxen.JaxenException;
import org.jdom2.Content;
import org.jdom2.Element;
import org.mycore.common.xml.MCRXPathBuilder;
import org.mycore.frontend.xeditor.MCRBinding;

/**
 * Sets an element's text in the edited xml, and tracks that change.  
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRSetElementText implements MCRChange {

    public String xPath;

    public String newValue;

    public List<Content> oldContent;

    public MCRSetElementText(Element element, String newValue) {
        this.xPath = MCRXPathBuilder.buildXPath(element);
        this.oldContent = element.cloneContent();
        this.newValue = newValue;
        element.setText(newValue);
    }

    @Override
    public String getMessage() {
        return "Set value of " + xPath + " to " + newValue;
    }

    @Override
    public void undo(MCRBinding root) throws JaxenException {
        MCRBinding elementBinding = new MCRBinding(xPath, false, root);
        Element element = (Element) (elementBinding.getBoundNode());
        element.setContent(oldContent);
        elementBinding.detach();
    }
}
