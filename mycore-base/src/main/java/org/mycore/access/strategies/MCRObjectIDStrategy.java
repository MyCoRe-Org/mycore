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

package org.mycore.access.strategies;

import org.mycore.access.MCRAccessManager;

/**
 * Use this class if you want to check against a MCRObjectID.
 * 
 * Be aware that you must provide a access rule - for each permission - for
 * every MCRObject.
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRObjectIDStrategy implements MCRCombineableAccessCheckStrategy {

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.strategies.MCRAccessCheckStrategy#checkPermission(java.lang.String,
     *      java.lang.String)
     */
    public boolean checkPermission(String id, String permission) {
        return MCRAccessManager.getAccessImpl().checkPermission(id, permission);
    }

    @Override
    public boolean hasRuleMapping(String id, String permission) {
        return MCRAccessManager.hasRule(id, permission);
    }

}
