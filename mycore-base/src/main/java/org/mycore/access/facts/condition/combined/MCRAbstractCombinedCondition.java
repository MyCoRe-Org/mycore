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
package org.mycore.access.facts.condition.combined;

import java.util.HashSet;
import java.util.Set;

import org.jdom2.Element;
import org.mycore.access.facts.MCRFactsAccessSystemHelper;
import org.mycore.access.facts.condition.MCRAbstractCondition;
import org.mycore.access.facts.model.MCRCombinedCondition;
import org.mycore.access.facts.model.MCRCondition;

/**
 * This is the base implementation for a combined condition.
 * It can be used to create conditions for a boolean algebra (and, or, not)
 * 
 * @author Robert Stephan
 *
 */
public abstract class MCRAbstractCombinedCondition extends MCRAbstractCondition implements MCRCombinedCondition {

    protected Set<MCRCondition> conditions = new HashSet<MCRCondition>();

    public void add(MCRCondition condition) {
        conditions.add(condition);
    }

    public void parse(Element xml) {
        super.parse(xml);
        for (Element child : xml.getChildren()) {
            conditions.add(MCRFactsAccessSystemHelper.parse(child));
        }
    }

    /**
     * @return the conditions
     */
    public Set<MCRCondition> getChildConditions() {
        return conditions;
    }

    public void debugInfoForMatchingChildElement(MCRCondition c, boolean matches) {
        if (isDebug()) {
            Element el = c.getBoundElement();
            if (el != null) {
                el.setAttribute("_matches", Boolean.toString(matches));
            }
        }
    }

}
