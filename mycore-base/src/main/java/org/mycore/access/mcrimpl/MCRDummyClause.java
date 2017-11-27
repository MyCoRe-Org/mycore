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

package org.mycore.access.mcrimpl;

import org.jdom2.Element;
import org.mycore.parsers.bool.MCRCondition;

/**
 * Implementation of a dummy clause (useful for debugging)
 * 
 * @author Matthias Kramm
 */
class MCRDummyClause implements MCRCondition<Object> {
    private String s;

    MCRDummyClause(String s) {
        this.s = s;
    }

    public boolean evaluate(Object o) {
        return false;
    }

    @Override
    public String toString() {
        return "\"" + s + "\"";
    }

    public Element toXML() {
        return null; /* TODO */
    }
}
