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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;

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
public class MCRObjectTypeStrategy implements MCRCombineableAccessCheckStrategy {
    private static final Pattern TYPE_PATTERN = Pattern.compile("[^_]*_([^_]*)_[0-9]*");

    private static final Logger LOGGER = LogManager.getLogger(MCRObjectIDStrategy.class);

    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    private static MCRObjectIDStrategy idStrategy = new MCRObjectIDStrategy();

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.strategies.MCRAccessCheckStrategy#checkPermission(java.lang.String,
     *      java.lang.String)
     */
    public boolean checkPermission(String id, String permission) {
        if (idStrategy.hasRuleMapping(id, permission)) {
            LOGGER.debug("using access rule defined for object.");
            return idStrategy.checkPermission(id, permission);
        }
        return checkObjectTypePermission(id, permission);
    }

    public static boolean checkObjectTypePermission(String id, String permission) {
        String objectType = getObjectType(id);
        if (hasTypePermission(objectType, permission)) {
            LOGGER.debug("using access rule defined for object type.");
            return MCRAccessManager.getAccessImpl().checkPermission("default_" + objectType, permission);
        }
        LOGGER.debug("using system default access rule.");
        return MCRAccessManager.getAccessImpl().checkPermission("default", permission);
    }

    private static boolean hasTypePermission(String objectType, String permission) {
        return objectType != null && MCRAccessManager.getAccessImpl().hasRule("default_" + objectType, permission);
    }

    private static String getObjectType(String id) {
        Matcher m = TYPE_PATTERN.matcher(id);
        if (m.find() && m.groupCount() == 1) {
            return m.group(1);
        } else {
            MCRCategoryID rootID;
            try {
                rootID = MCRCategoryID.rootID(id);
            } catch (Exception e) {
                LOGGER.debug("ID '{}' is not a valid category id.", id);
                return null;
            }
            if (DAO.exist(rootID)) {
                return "class";
            }
        }
        return null;
    }

    @Override
    public boolean hasRuleMapping(String id, String permission) {
        return idStrategy.hasRuleMapping(id, permission) || hasTypePermission(getObjectType(id), permission)
            || MCRAccessManager.hasRule("default", permission);
    }

}
