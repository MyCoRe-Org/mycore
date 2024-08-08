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

package org.mycore.mcr.acl.accesskey;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.mycore.common.config.MCRConfiguration2;

/**
 * This class provides configuration.
 */
public class MCRAccessKeyConfig {

    private static final String ACCESS_KEY_STRATEGY_PROP_PREFX = "MCR.ACL.AccessKey.Strategy";

    /**
     * Config property name for allowed object types.
     */
    public static final String ALLOWED_OBJECT_TYPES_PROP = ACCESS_KEY_STRATEGY_PROP_PREFX + ".AllowedObjectTypes";

    /**
     * Config property name for allowed session permission types.
     */
    public static final String ALLOWED_SESSION_PERMISSION_TYPES_PROP = ACCESS_KEY_STRATEGY_PROP_PREFX
        + ".AllowedSessionPermissionTypes";

    private static Set<String> allowedObjectTypes = MCRConfiguration2.getString(ALLOWED_OBJECT_TYPES_PROP).stream()
        .flatMap(MCRConfiguration2::splitValue).collect(Collectors.toSet());

    private static Set<String> allowedSessionPermissionTypes = MCRConfiguration2
        .getString(ALLOWED_SESSION_PERMISSION_TYPES_PROP).stream().flatMap(MCRConfiguration2::splitValue)
        .collect(Collectors.toSet());

    static {
        MCRConfiguration2.addPropertyChangeEventLister(ALLOWED_OBJECT_TYPES_PROP::equals,
            (p1, p2, p3) -> allowedObjectTypes = parseListStringToSet(p3));
        MCRConfiguration2.addPropertyChangeEventLister(ALLOWED_SESSION_PERMISSION_TYPES_PROP::equals,
            (p1, p2, p3) -> allowedSessionPermissionTypes = parseListStringToSet(p3));
    }

    private static Set<String> parseListStringToSet(Optional<String> listString) {
        return listString.stream().flatMap(MCRConfiguration2::splitValue).collect(Collectors.toSet());
    }

    private MCRAccessKeyConfig() {

    }

    /**
     * Returns a list of types for which access keys can be used.
     *
     * @return list of types
     */
    public static Set<String> getAllowedObjectTypes() {
        return allowedObjectTypes;
    }

    /**
     * Returns a list of permissions for which access keys may be activated in the session.
     *
     * @return list of permissions
     */
    public static Set<String> getAllowedSessionPermissionTypes() {
        return allowedSessionPermissionTypes;
    }

}
