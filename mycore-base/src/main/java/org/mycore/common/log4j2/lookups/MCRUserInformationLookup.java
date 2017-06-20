/**
 *
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
    name = "mcruser", category = StrLookup.CATEGORY)
public class MCRUserInformationLookup implements StrLookup {

    private static final Pattern ROLE_SEPARATOR = Pattern.compile(",");

    private static final String ROLE_PREFIX = "role:";

    @Override
    public String lookup(String key) {
        String value = getValue(key);
        return value;
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
