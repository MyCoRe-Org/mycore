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

package org.mycore.frontend.xeditor.tracker;

import org.jdom2.Attribute;
import org.jdom2.Document;

/**
 * Sets an attribute value in the edited xml, and tracks that change.  
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRSetAttributeValue extends MCRChange {

    private String oldValue;

    public MCRSetAttributeValue(Attribute attr, String newValue) {
        super(attr);
        setMessage(String.format("Set %s=\"%s\"", getXPath(), attr.getValue()));

        this.oldValue = attr.getValue();
        attr.setValue(newValue);
    }

    @Override
    protected void undo(Document doc) {
        Attribute attr = (Attribute) (getNodeByXPath(doc));
        attr.setValue(oldValue);
    }
}
