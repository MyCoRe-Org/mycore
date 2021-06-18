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
import org.mycore.access.xml.MCRConditionHelper;

import java.util.HashSet;
import java.util.Set;

public abstract class MCRCombinedCondition implements MCRCondition, MCRDebuggableCondition {

    protected Set<MCRCondition> conditions = new HashSet<MCRCondition>();

    protected boolean debug = false;

    protected Element boundElement = null;

    public void add(MCRCondition condition) {
        conditions.add(condition);
    }

    public void parse(Element xml) {
        boundElement = xml;
        for (Element child : xml.getChildren()) {
            conditions.add(MCRConditionHelper.parse(child));
        }
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public Element getBoundElement() {
        return boundElement;
    }

    protected void setChildElementMatching(MCRCondition c, boolean matches) {
        if (isDebug() && c instanceof MCRDebuggableCondition) {
            Element el = ((MCRDebuggableCondition) c).getBoundElement();
            if (el != null) {
                el.setAttribute("matches", Boolean.toString(matches));
            }
        }
    }
}
