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

package org.mycore.parsers.bool;

import org.jdom2.Element;

/**
 * Implementation of the boolean "true" primitive
 *
 * @author Matthias Kramm
 */
public class MCRTrueCondition<T> implements MCRCondition<T> {

    @Override
    public boolean evaluate(T o) {
        return true;
    }

    @Override
    public String toString() {
        return "true";
    }

    @Override
    public Element toXML() {
        Element cond = new Element("boolean");
        cond.setAttribute("operator", "true");
        return cond;
    }
}
