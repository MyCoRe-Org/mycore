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

package org.mycore.common.events;

import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUserInformation;

/**
 * A EventHandler which runs as {@link MCRSystemUserInformation#JANITOR}.
 */
public class MCRJanitorEventHandlerBase extends MCREventHandlerBase {

    @Override
    public void doHandleEvent(MCREvent evt) {
        MCRUserInformation prevUserInformation = MCRSessionMgr.getCurrentSession().getUserInformation();
        try {
            MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.GUEST);
            MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.JANITOR);
            super.doHandleEvent(evt);
        } finally {
            MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.GUEST);
            MCRSessionMgr.getCurrentSession().setUserInformation(prevUserInformation);
        }
    }

}
