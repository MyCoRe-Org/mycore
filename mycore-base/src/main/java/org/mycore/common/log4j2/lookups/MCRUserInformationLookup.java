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

package org.mycore.common.log4j2.lookups;

import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.mycore.common.MCRSessionMgr;

/**
 * Allows to access information on the current user. This lookup returns <code>null</code> if <code>key == null</code>
 * or {@link MCRSessionMgr#hasCurrentSession()} returns <code>false</code>. <code>key</code> may be either
 * <dl>
 * <dt>id
 * <dt>
 * <dd>returns the current user id</dd>
 * <dt>role:{role1},{role2},...,{roleN}<dt>
 * <dd>returns the first role the current user is in<dd>
 * </dl>
 *
 * @author Thomas Scheffler (yagee)
 */
@Plugin(
    name = "mcruser",
    category = StrLookup.CATEGORY)
public class MCRUserInformationLookup implements StrLookup {

    private static final Pattern ROLE_SEPARATOR = Pattern.compile(",");

    private static final String ROLE_PREFIX = "role:";

    @Override
    public String lookup(String key) {
        return getValue(key);
    }

    @Override
    public String lookup(LogEvent event, String key) {
        return lookup(key);
    }

    private static String getValue(String key) {
        if (key == null || !MCRSessionMgr.hasCurrentSession()) {
            return null;
        }
        if ("id".equals(key)) {
            return MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
        } else if (key.startsWith(ROLE_PREFIX)) {
            Optional<String> firstMatchingRole = ROLE_SEPARATOR
                .splitAsStream(key.substring(ROLE_PREFIX.length()))
                .filter(role -> MCRSessionMgr.getCurrentSession().getUserInformation().isUserInRole(role))
                .findFirst();
            if (firstMatchingRole.isPresent()) {
                return firstMatchingRole.get();
            }
        }
        return null;
    }

}
