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

import org.jaxen.JaxenException;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.mycore.common.xml.MCRXPathBuilder;
import org.mycore.frontend.xeditor.MCRBinding;

/**
 * Removes an attribute from the edited xml, and tracks that change.  
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRRemoveAttribute implements MCRChange {

    private String xPath;

    private Attribute attribute;

    public MCRRemoveAttribute(Attribute attribute) {
        this.attribute = attribute.clone();
        this.xPath = MCRXPathBuilder.buildXPath(attribute);
        attribute.detach();
    }

    @Override
    public String getMessage() {
        return "Removed attribute " + xPath;
    }

    @Override
    public void undo(MCRBinding root) throws JaxenException {
        String parentXPath = xPath.substring(0, xPath.lastIndexOf("/@"));
        MCRBinding parentBinding = new MCRBinding(parentXPath, false, root);
        Element parent = (Element) (parentBinding.getBoundNode());
        parent.setAttribute(attribute);
        parentBinding.detach();
    }
}
