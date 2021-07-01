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

import org.mycore.access.xml.MCRFacts;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;

public class MCRRoleCondition extends MCRSimpleCondition {

    @Override
    public boolean matches(MCRFacts facts) {
        if (super.matches(facts)) {
            return true;
        }
        MCRSession session = MCRSessionMgr.getCurrentSession();
        MCRUserInformation user = session.getUserInformation();
        if (user.isUserInRole(value)) {
            facts.add(this);
            return true;
        }
        return false;
    }
}
