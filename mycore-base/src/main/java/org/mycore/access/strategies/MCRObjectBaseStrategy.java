/*
 * 
 * $Revision: 26482 $ $Date: 2013-03-13 10:16:11 +0100 (Mi, 13. Mär 2013) $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.access.strategies;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Use this class if you want to have a fallback to some default access permissions.
 * 
 * First a check is done for the MCRObjectID. If no rule for the ID is specified
 * it will be tried to check the permission<code>&lt;permission&gt;-&lt;ID&gt;</code>. 
 * if it is not yet it test agains the permission
 * <code>&lt;permission&gt;-default</code> if it exists.
 * 
 * @author Thomas Scheffler (yagee)
 * @author Jens Kupferschmidt
 * 
 * @version $Revision: 26482 $ $Date: 2013-03-13 10:16:11 +0100 (Mi, 13. Mär 2013) $
 */
public class MCRObjectBaseStrategy implements MCRAccessCheckStrategy {
    private static final Logger LOGGER = Logger.getLogger(MCRObjectBaseStrategy.class);

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.strategies.MCRAccessCheckStrategy#checkPermission(java.lang.String,
     *      java.lang.String)
     */
    public boolean checkPermission(String id, String permission) {
        LOGGER.debug("check permission " + permission + " for MCRBaseID " + id);
        if (id == null | id.length() == 0 || permission == null || permission.length() == 0)
            return false;
        MCRAccessInterface ai = MCRAccessManager.getAccessImpl();
        if (ai.hasRule(id, permission)) {
            LOGGER.debug("using access rule defined for object.");
            return ai.checkPermission(id, permission);
        }
        try {
            MCRObjectID mid = MCRObjectID.getInstance(id);
            if (ai.checkPermission(permission + "-" + mid.getBase())) {
                LOGGER.debug("using check permission for id " + id + " and permission " + permission);
                return true;
            }
        } catch (MCRException e) {
        }
        LOGGER.debug("using system default permission for " + permission);
        return MCRAccessManager.getAccessImpl().checkPermission("default", permission);
    }

}
