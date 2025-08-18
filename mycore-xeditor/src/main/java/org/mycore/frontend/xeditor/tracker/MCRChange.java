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

import org.jdom2.Document;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.common.xml.MCRXPathBuilder;

/**
 * Represents a change in the edited xml, which can be undone.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public abstract class MCRChange extends MCRTrackedAction {

    /** The xPath of the changed node (or it's parent) */
    private String xPath;

    /**
     * Creates a new object representing a change in XML.
     * 
     * @param node the node the change is performed on
     * @param action the action
     */
    protected MCRChange(Object node) {
        this.xPath = MCRXPathBuilder.buildXPath(node);
    }

    protected String getXPath() {
        return xPath;
    }

    /** Returns the node represented by the stored XPath */
    protected Object getNodeByXPath(Document doc) {
        return XPathFactory.instance()
            .compile(xPath, Filters.fpassthrough(), null, MCRConstants.getStandardNamespaces())
            .evaluateFirst(doc);
    }

    /** Performs an undo of this change in the edited xml **/
    protected abstract void undo(Document doc);
}
