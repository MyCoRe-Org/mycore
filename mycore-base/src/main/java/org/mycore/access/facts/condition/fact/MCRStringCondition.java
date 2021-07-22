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
package org.mycore.access.facts.condition.fact;

import java.util.Optional;

import org.mycore.access.facts.MCRFactsAccessSystem;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.access.facts.fact.MCRStringFact;

/**
 * This is a simple implemenation.
 * It checks if the fact already exists in the fact database and returns it.
 * Otherwise it returns an empty Optional
 * 
 * This is useful for "static" facts that are created before the rule processing started
 * in MCRFactsAccessSystem.
 * 
 * @author Robert Stephan
 *
 */
public class MCRStringCondition extends MCRAbstractFactCondition<MCRStringFact> {

    /**
     * Subclasses should override this method to retrieve the fact from MyCoReObject, MCRSession 
     * or from elsewhere ...
     *  
     *  @see MCRFactsAccessSystem
     */
    @Override
    public Optional<MCRStringFact> computeFact(MCRFactsHolder facts) {
        MCRStringFact checkFact = new MCRStringFact(getFactName(), getTerm());
        if (facts.isFact(getFactName(), getTerm())) {
            return Optional.of(checkFact);
        } else {
            //check if a simpleFact with the default name (name of Element) was stored in facts
            //and save it with the new name
            if (facts.isFact(getType(), getTerm())) {
                facts.add(checkFact);
                return Optional.of(checkFact);
            }
        }
        return Optional.empty();
    }
}
