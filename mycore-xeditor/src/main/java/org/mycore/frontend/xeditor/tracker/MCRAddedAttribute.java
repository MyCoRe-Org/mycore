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
 * Tracks that a new attribute was added.  
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRAddedAttribute implements MCRChange {

    private String xPath;

    public MCRAddedAttribute(Attribute attribute) {
        this.xPath = MCRXPathBuilder.buildXPath(attribute);
    }

    @Override
    public String getMessage() {
      return "Added attribute " + xPath;  
    }

    @Override
    public void undo(MCRBinding rootBinding) throws JaxenException {
        MCRBinding attributeBinding = new MCRBinding(xPath, false, rootBinding);
        Attribute attribute = (Attribute) (attributeBinding.getBoundNode());
        attribute.detach();
        attributeBinding.detach();
    }
}
