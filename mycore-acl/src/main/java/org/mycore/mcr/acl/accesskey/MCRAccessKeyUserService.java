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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserAttribute;
import org.mycore.user2.MCRUserManager;

/**
 * Implements {@link MCRAccessKeyContextService} for {@link MCRUser} context.
 */
public class MCRAccessKeyUserService extends MCRAccessKeyContextService<MCRUser> {

    /**
     * Prefix for user attribute name for value.
     */
    public static final String ACCESS_KEY_USER_ATTRIBUTE_PREFIX = "acckey_";

    public MCRAccessKeyUserService(MCRAccessKeyService accessKeyService) {
        super(accessKeyService);
    }

    @Override
    protected MCRUser getCurrentContext() {
        return MCRUserManager.getCurrentUser();
    }

    @Override
    protected void addAccessKeyValueAttributToContext(MCRUser context, String attributeName, String value) {
        context.setUserAttribute(attributeName, value);
        MCRUserManager.updateUser(context);
    }

    @Override
    protected boolean checkAddAccessKeyToContextIsAllowed(MCRAccessKeyDto accessKeyDto) {
        return true;
    }

    @Override
    protected void deleteAccessKeyValueAttributeFromContext(MCRUser context, String attributeName) {
        context.getAttributes().removeIf(ua -> ua.getName().equals(attributeName));
        MCRUserManager.updateUser(context);
    }

    @Override
    protected String getAccessKeyValueAttributeFromContext(MCRUser context, String attributeName) {
        return context.getUserAttribute(attributeName);
    }

    @Override
    String getAttributePrefix() {
        return ACCESS_KEY_USER_ATTRIBUTE_PREFIX;
    }

    /**
     * Cleans all access key secret attributes of users if the corresponding key does not exist.
     */
    public void cleanUpUserAttributes() {
        final Set<MCRUserAttribute> validAttributes = new HashSet<>();
        final Set<MCRUserAttribute> deadAttributes = new HashSet<>();
        int offset = 0;
        final int limit = 1024;
        List<MCRUser> users = new ArrayList<>();
        do {
            users = listUsersWithAccessKey(offset, limit);
            for (final MCRUser user : users) {
                final List<MCRUserAttribute> attributes = user.getAttributes().stream()
                    .filter(attribute -> attribute.getName().startsWith(ACCESS_KEY_USER_ATTRIBUTE_PREFIX))
                    .filter(attribute -> !validAttributes.contains(attribute)).collect(Collectors.toList());
                for (MCRUserAttribute attribute : attributes) {
                    final String attributeName = attribute.getName();
                    final String reference = attributeName.substring(attributeName.indexOf("_") + 1);
                    if (deadAttributes.contains(attribute)) {
                        deleteAccessKeyValueAttributeFromContext(user, ACCESS_KEY_USER_ATTRIBUTE_PREFIX + reference);
                    } else if (getService().getAccessKeyByReferenceAndValue(reference, attribute.getValue()) != null) {
                        validAttributes.add(attribute);
                    } else {
                        deleteAccessKeyValueAttributeFromContext(user, ACCESS_KEY_USER_ATTRIBUTE_PREFIX + reference);
                        deadAttributes.add(attribute);
                    }
                }
            }
            offset += limit;
        } while (users.size() == limit);
    }

    private static List<MCRUser> listUsersWithAccessKey(int offset, int limit) {
        return MCRUserManager.listUsers(null, null, null, null, ACCESS_KEY_USER_ATTRIBUTE_PREFIX + "*", null, offset,
            limit);
    }

}
