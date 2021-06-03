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

package org.mycore.access.xml.conditions;

import org.jdom2.Element;
import org.mycore.access.xml.MCRFacts;

import java.util.Objects;

public class MCRRegExpCondition extends MCRSimpleCondition {

    private String fact;

    @Override
    public void parse(Element xml) {
        super.parse(xml);
        this.fact = xml.getAttributeValue("fact");
    }

    @Override
    public boolean matches(MCRFacts facts) {
        String id = facts.require(fact).value;

        String value = this.value;
        if (id.matches(value)) {
            facts.add(this);
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        MCRRegExpCondition that = (MCRRegExpCondition) o;
        return fact.equals(that.fact);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fact);
    }
}
