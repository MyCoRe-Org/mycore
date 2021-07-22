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

import org.jdom2.Element;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.access.facts.model.MCRCondition;

/**
 * This condition negates its child condition 
 * (boolean NOT)
 * 
 * Only the first child condition will be evaluated.
 * Further child conditions will be ignored.
 * 
 * @author Robert Stephan
 *
 */
public class MCRNotCondition extends MCRAbstractCombinedCondition {

    public boolean matches(MCRFactsHolder facts) {
        MCRCondition negatedCondition = conditions.stream().findFirst().get();
        boolean result = negatedCondition.matches(facts);
        boolean negated = !result;
        if (isDebug()) {
            Element boundElement = negatedCondition.getBoundElement();
            if (boundElement != null) {
                boundElement.setAttribute("_matched", Boolean.toString(result));
            }

            if (this.getBoundElement() != null) {
                this.getBoundElement().setAttribute("_matched", Boolean.toString(negated));
            }
        }
        return negated;
    }
}
