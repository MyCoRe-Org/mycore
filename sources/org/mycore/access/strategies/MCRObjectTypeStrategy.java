/*
 * $RCSfile$
 * $Revision$ $Date$
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.mycore.access.MCRAccessManager;

/**
 * Use this class if you want to have a fallback to some default access rules.
 * 
 * First a check is done for the MCRObjectID. If no rule for the ID is specified
 * it will be tried to check the permission agains the rule ID
 * <code>default_&lt;ObjectType&gt;</code> if it exists. If not the last
 * fallback is done against <code>default</code>.
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRObjectTypeStrategy implements MCRAccessCheckStrategy {
    private static final Pattern TYPE_PATTERN = Pattern.compile("[^_]*_([^_]*)_*");

    private static final Logger LOGGER = Logger.getLogger(MCRObjectIDStrategy.class);

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.strategies.MCRAccessCheckStrategy#checkPermission(java.lang.String,
     *      java.lang.String)
     */
    public boolean checkPermission(String id, String permission) {
        String objectType = getObjectType(id);

        if (MCRAccessManager.getAccessImpl().hasRule(id, permission)) {
            LOGGER.debug("using access rule defined for object.");
            return MCRAccessManager.getAccessImpl().checkPermission(id, permission);
        } else if (MCRAccessManager.getAccessImpl().hasRule("default_" + objectType, permission)) {
            LOGGER.debug("using access rule defined for object type.");
            return MCRAccessManager.getAccessImpl().checkPermission("default_" + objectType, permission);
        }
        LOGGER.debug("using system default access rule.");
        return MCRAccessManager.getAccessImpl().checkPermission("default", permission);
    }

    private static String getObjectType(String id) {
        Matcher m = TYPE_PATTERN.matcher(id);
        if (m.find() && (m.groupCount() == 1)) {
            return m.group(1);
        }
        return "";
    }

}