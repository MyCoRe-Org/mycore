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
import org.mycore.access.xml.MCRConditionFactory;
import org.mycore.access.xml.MCRFacts;

public class MCRNotCondition implements MCRCondition, MCRDebuggableCondition {

    private MCRCondition negatedCondition;

    private Element boundElement;

    private boolean debug;

    public boolean matches(MCRFacts facts) {
        boolean result = negatedCondition.matches(facts);
        if (isDebug() && negatedCondition instanceof MCRDebuggableCondition) {
            Element boundElement = ((MCRDebuggableCondition) negatedCondition).getBoundElement();
            if (boundElement != null) {
                boundElement.setAttribute("matched",  Boolean.toString(result));
            }
        }

        boolean negated = !result;
        if (this.boundElement != null) {
            this.boundElement.setAttribute("matches", Boolean.toString(negated));
        }
        return negated;
    }

    private boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean b) {
        this.debug = b;
    }

    public void parse(Element xml) {
        this.boundElement = xml;
        negatedCondition = MCRConditionFactory.parse(xml.getChildren().get(0));
    }

    @Override
    public Element getBoundElement() {
        return boundElement;
    }
}
