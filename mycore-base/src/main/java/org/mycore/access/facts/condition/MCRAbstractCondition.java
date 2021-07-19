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
package org.mycore.access.facts.condition;

import org.jdom2.Element;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.access.facts.model.MCRCondition;

/**
 * This is the base implementation for a condition.
 * 
 * It is the super class for MCRCombinedCondition and MCRFactCondition.
 * 
 * @author Robert Stephan
 *
 */
public abstract class MCRAbstractCondition implements MCRCondition {

    private Element boundElement = null;

    private String type;

    private boolean debug;

    /** 
     * implementors of this method should call super.parse(xml) to bind the XML element to the condition
     */
    public void parse(Element xml) {
        boundElement = xml;
        type = xml.getName();
    }

    public Element getBoundElement() {
        return boundElement;
    }

    public String getType() {
        return type;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean b) {
        this.debug = b;

    }

    public abstract boolean matches(MCRFactsHolder facts);
}
