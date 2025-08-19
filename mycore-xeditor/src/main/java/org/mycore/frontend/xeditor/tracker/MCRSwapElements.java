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

import java.util.List;

import org.jdom2.Element;
import org.mycore.common.xml.MCRXPathBuilder;

/**
 * Swaps two elements in the edited xml, and tracks that change.  
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRSwapElements extends MCRChange {

    private Element parent;

    /** The position of the first element swapped */
    private int posA;

    /** The position of the other element swapped */
    private int posB;

    /**
     * Swaps the position of two elements within their parent
     * 
     * @param parent the common parent of the elements
     * @param a the first element to swap
     * @param b the other element to swap
     */
    public MCRSwapElements(Element parent, Element a, Element b) {
        this(parent, parent.getChildren().indexOf(a), parent.getChildren().indexOf(b));
    }

    /**
     * Swaps the position of two elements within their parent
     * 
     * @param parent the common parent of the elements
     * @param posA the position of first element to swap
     * @param posB the position of the other element to swap
     */
    public MCRSwapElements(Element parent, int posA, int posB) {
        this.parent = parent;
        this.posA = posA;
        this.posB = posB;
        swap(parent, posA, posB);
    }

    /** 
     * Custom swap() method, as Collections.swap() does not work with JDOM2 children. 
     **/
    private void swap(Element parent, int posA, int posB) {
        if (posA > posB) {
            swap(parent, posB, posA);
        } else {
            List<Element> children = parent.getChildren(); // => x a x x b x

            String xPathParent = MCRXPathBuilder.buildXPath(parent);
            String xPathOfA = MCRXPathBuilder.buildChildPath(children.get(posA));
            String xPathOfB = MCRXPathBuilder.buildChildPath(children.get(posB));

            setMessage("Swapped " + xPathParent + " children " + xPathOfA + " and " + xPathOfB);

            Element b = children.remove(posB); // => x a x x x  
            Element a = children.remove(posA); // => x x x x 
            children.add(posA, b); // => x b x x x 
            children.add(posB, a); // => x b x x a x
        }
    }

    @Override
    protected void undo() {
        swap(parent, posA, posB);
    }
}
