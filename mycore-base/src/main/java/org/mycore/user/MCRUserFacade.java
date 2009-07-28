/**
 * $RCSfile: MCRUserFacade.java,v $
 * $Revision: 1.0 $ $Date: 01.09.2008 10:16:52 $
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.user;

import java.security.Principal;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSessionMgr;

public class MCRUserFacade {

    public static String getCurrentUser() {
        if (MCRConfiguration.instance().getBoolean("MCR.User.UseWebServer", false)) {
            Principal p = MCRSessionMgr.getCurrentSession().getUserPrincipal();
            if (p != null)
                return p.getName();
        }
        return MCRSessionMgr.getCurrentSession().getCurrentUserID();
    }

    public static boolean isUserInGroup(String role) {
        if (MCRConfiguration.instance().getBoolean("MCR.User.UseWebServer", false)) {
            Principal p = MCRSessionMgr.getCurrentSession().getUserPrincipal();
            if (p != null)
                return MCRSessionMgr.getCurrentSession().isPrincipalInRole(role);
        }
        return MCRUserMgr.instance().getCurrentUser().isMemberOf(new MCRGroup(role));
    }

}
