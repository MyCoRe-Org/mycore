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

public class MCRSimpleCondition implements MCRCondition, MCRDebuggableCondition {

    static final String UNDEFINED = "undefined";

    public String value = UNDEFINED;

    protected String type;
    private Element boundElement = null;

    @Override
    public void parse(Element xml) {
        boundElement = xml;
        this.value = xml.getTextTrim();
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setCurrentValue(MCRFacts facts) {
        this.value = UNDEFINED;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean matches(MCRFacts facts) {
        return facts.isFact(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MCRSimpleCondition) {
            MCRSimpleCondition other = (MCRSimpleCondition) obj;
            return this.value.equals(other.value) && this.type.equals(other.type);

        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return type + "=" + value;
    }

    @Override
    public Element getBoundElement() {
        return boundElement;
    }
}
