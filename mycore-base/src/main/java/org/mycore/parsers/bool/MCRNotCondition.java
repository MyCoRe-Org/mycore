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

package org.mycore.parsers.bool;

import org.jdom2.Element;

/**
 * @author Frank Lützenkirchen
 */
public class MCRNotCondition<T> implements MCRCondition<T> {
    private MCRCondition<T> child;

    public MCRNotCondition(MCRCondition<T> child) {
        this.child = child;
    }

    public MCRCondition<T> getChild() {
        return child;
    }

    @Override
    public String toString() {
        return "not (" + child + ")";
    }

    public boolean evaluate(T o) {
        return !child.evaluate(o);
    }

    public Element toXML() {
        Element not = new Element("boolean");
        not.setAttribute("operator", "not");
        not.addContent(child.toXML());
        return not;
    }
}
