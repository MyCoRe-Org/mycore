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
import org.mycore.common.xml.MCRXPathBuilder;
import org.mycore.frontend.xeditor.MCRBinding;

/**
 * Sets an attribute value in the edited xml, and tracks that change.  
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRSetAttributeValue implements MCRChange {

    public String xPath;

    public String newValue;

    public String oldValue;

    public MCRSetAttributeValue(Attribute attribute, String newValue) {
        this.xPath = MCRXPathBuilder.buildXPath(attribute);
        this.oldValue = attribute.getValue();
        this.newValue = newValue;
        attribute.setValue(newValue);
    }

    @Override
    public String getMessage() {
        return "Set value of " + xPath + " to " + newValue;
    }

    @Override
    public void undo(MCRBinding root) throws JaxenException {
        MCRBinding attributeBinding = new MCRBinding(xPath, false, root);
        Attribute attribute = (Attribute) (attributeBinding.getBoundNode());
        attribute.setValue(oldValue);
        attributeBinding.detach();
    }
}
