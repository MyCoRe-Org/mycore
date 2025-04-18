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
package org.mycore.access.facts.condition.fact;

import java.util.Optional;

import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.access.facts.fact.MCRStringFact;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;

/**
 * This condition checks if the current user matches the query string.
 * 
 * Example:
 * &lt;user&gt;mcradmin&lt;/user&gt;
 * 
 * @author mcradmin
 *
 */
public class MCRUserCondition extends MCRStringCondition {

    @Override
    public Optional<MCRStringFact> computeFact(MCRFactsHolder facts) {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        MCRUserInformation user = session.getUserInformation();
        if (user != null && getTerm().equals(user.getUserID())) {
            MCRStringFact fact = new MCRStringFact(getFactName(), getTerm());
            fact.setValue(user.getUserID());
            facts.add(fact);
            return Optional.of(fact);
        }
        return Optional.empty();
    }
}
