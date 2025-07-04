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

package org.mycore.access.strategies;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;

/**
 * Use this class if you want to have a fallback to some default access rules.
 * <p>
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
 */
public class MCRObjectBaseStrategy implements MCRCombineableAccessCheckStrategy {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Pattern BASE_PATTERN = Pattern.compile("([^_]*_[^_]*)_*");

    private final MCRObjectTypeStrategy typeStrategy = new MCRObjectTypeStrategy();

    /*
     * (non-Javadoc)
     *
     * @see org.mycore.access.strategies.MCRAccessCheckStrategy#checkPermission(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public boolean checkPermission(String id, String permission) {
        LOGGER.debug("check permission {} for MCRBaseID {}", permission, id);
        if (id == null || id.length() == 0 || permission == null || permission.length() == 0) {
            return false;
        }
        if (MCRAccessManager.requireRulesInterface().hasRule(id, permission)) {
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
        return objectBase != null && MCRAccessManager.requireRulesInterface()
            .hasRule("default_" + objectBase, permission);
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
