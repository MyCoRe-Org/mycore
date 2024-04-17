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
import org.jdom2.Element;
import org.mycore.common.xml.MCRXPathBuilder;
import org.mycore.frontend.xeditor.MCRBinding;

/**
 * Swaps two elements in the edited xml, and tracks that change.  
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRSwapElements implements MCRChange {

    private String xPath;

    private int posA;

    private int posB;

    public MCRSwapElements(Element parent, Element a, Element b) {
        this(parent, parent.getChildren().indexOf(a), parent.getChildren().indexOf(b));
    }

    public MCRSwapElements(Element parent, int posA, int posB) {
        this.xPath = MCRXPathBuilder.buildXPath(parent);
        this.posA = posA;
        this.posB = posB;
        swap(parent, posA, posB);
    }

    @Override
    public String getMessage() {
        return "Swapped elements " + posA + " and " + posB + " at " + xPath;
    }

    private void swap(Element parent, int posA, int posB) {
        if (posA > posB) {
            swap(parent, posB, posA);
        } else {
            List<Element> children = parent.getChildren(); // => x a x x b x
            Element b = children.remove(posB); // => x a x x x  
            children.add(posA, b); // => x b a x x x 
            Element a = children.remove(posA + 1); // => x b x x x 
            children.add(posB, a); // => x b x x a x
        }
    }

    public void undo(MCRBinding root) throws JaxenException {
        MCRBinding parentBinding = new MCRBinding(xPath, false, root);
        Element parent = (Element) (parentBinding.getBoundNode());
        swap(parent, posA, posB);
    }
}
