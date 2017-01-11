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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;

/**
 * Use this class if you want to have a fallback to some default access rules.
 * 
 * These are the rules that will be checked against if available
 * <ol>
 *  <li><code>{id}</code>, e.g. "MyCoRe_mods_12345678"</li>
 *  <li><code>default_{baseId}</code>, e.g. "default_MyCoRe_mods"</li>
 *  <li><code>default_{objectType}</code>, e.g. "default_mods"</li>
 *  <li><code>default</code></li>
 * </ol>
 *
 * This is the same behaviour as {@link MCRObjectTypeStrategy} but step 2 is inserted here. 
 * @author Thomas Scheffler (yagee)
 * @author Jens Kupferschmidt
 * 
 * @version $Revision: 26482 $ $Date: 2013-03-13 10:16:11 +0100 (Mi, 13. Mär 2013) $
 */
public class MCRObjectBaseStrategy implements MCRCombineableAccessCheckStrategy {
    private static final Logger LOGGER = LogManager.getLogger(MCRObjectBaseStrategy.class);

    private static final Pattern BASE_PATTERN = Pattern.compile("([^_]*_[^_]*)_*");

    private final MCRObjectTypeStrategy typeStrategy = new MCRObjectTypeStrategy();

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.strategies.MCRAccessCheckStrategy#checkPermission(java.lang.String,
     *      java.lang.String)
     */
    public boolean checkPermission(String id, String permission) {
        LOGGER.debug("check permission " + permission + " for MCRBaseID " + id);
        if (id == null || id.length() == 0 || permission == null || permission.length() == 0)
            return false;
        if (MCRAccessManager.getAccessImpl().hasRule(id, permission)) {
            LOGGER.debug("using access rule defined for object.");
            return MCRAccessManager.getAccessImpl().checkPermission(id, permission);
        }
        String objectBase = getObjectBase(id);
        if (hasBasePermission(objectBase, permission)) {
            LOGGER.debug("using access rule defined for object base.");
            return MCRAccessManager.getAccessImpl().checkPermission("default_" + objectBase, permission);
        }
        return MCRObjectTypeStrategy.checkObjectTypePermission(id, permission);
    }

    private boolean hasBasePermission(String objectBase, String permission) {
        return objectBase != null && MCRAccessManager.getAccessImpl().hasRule("default_" + objectBase, permission);
    }

    private static String getObjectBase(String id) {
        Matcher m = BASE_PATTERN.matcher(id);
        if (m.find() && m.groupCount() == 1) {
            return m.group(1);
        }
        return null;
    }

    @Override
    public boolean hasRuleMapping(String id, String permission) {
        return hasBasePermission(getObjectBase(id), permission) || typeStrategy.hasRuleMapping(id, permission);
    }

}
