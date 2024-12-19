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

package org.mycore.mcr.acl.accesskey.service;

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
     * Prefix for user attribute name for secret.
     */
    public static final String ACCESS_KEY_USER_ATTRIBUTE_PREFIX = "acckey_";

    /**
     * Creates new {@link MCRAccessKeyUserService} with {@link MCRAccessKeyService}.
     *
     * @param accessKeyService the access key service
     */
    public MCRAccessKeyUserService(MCRAccessKeyService accessKeyService) {
        super(accessKeyService);
    }

    @Override
    protected MCRUser getCurrentContext() {
        return MCRUserManager.getCurrentUser();
    }

    @Override
    protected void setAccessKeyAttribute(MCRUser context, String attributeName, String secret) {
        context.setUserAttribute(attributeName, secret);
        MCRUserManager.updateUser(context);
    }

    @Override
    protected boolean isAccessKeyAllowedInContext(MCRAccessKeyDto accessKeyDto) {
        return true;
    }

    @Override
    protected void removeAccessKeyFromContext(MCRUser context, String attributeName) {
        context.getAttributes().removeIf(ua -> ua.getName().equals(attributeName));
        MCRUserManager.updateUser(context);
    }

    @Override
    protected String getAccessKeyFromContext(MCRUser context, String attributeName) {
        return context.getUserAttribute(attributeName);
    }

    @Override
    String getAccessKeyAttributePrefix() {
        return ACCESS_KEY_USER_ATTRIBUTE_PREFIX;
    }

    /**
     * Cleans up user attributes related to access keys by checking their validity.
     *
     * This method iterates over all users who have access key attributes and ensures that only valid access key
     * attributes are retained. It removes access key attributes if they are invalid or have been deleted.
     */
    public void cleanUpUserAttributes() {
        final Set<MCRUserAttribute> validAttributes = new HashSet<>();
        final Set<MCRUserAttribute> deadAttributes = new HashSet<>();
        final int limit = 1024;
        int offset = 0;
        List<MCRUser> users;
        do {
            users = listUsersWithAccessKey(offset, limit);
            for (MCRUser user : users) {
                cleanUpAttributesForUser(user, validAttributes, deadAttributes);
            }
            offset += limit;
        } while (users.size() == limit);
    }

    private static List<MCRUser> listUsersWithAccessKey(int offset, int limit) {
        return MCRUserManager.listUsers(null, null, null, null, ACCESS_KEY_USER_ATTRIBUTE_PREFIX + "*", null, offset,
            limit);
    }

    private String extractReference(MCRUserAttribute attribute) {
        return attribute.getName().substring(attribute.getName().indexOf('_') + 1);
    }

    private void cleanUpAttributesForUser(MCRUser user, Set<MCRUserAttribute> validAttributes,
        Set<MCRUserAttribute> deadAttributes) {
        final List<MCRUserAttribute> attributes = user.getAttributes().stream()
            .filter(attr -> attr.getName().startsWith(ACCESS_KEY_USER_ATTRIBUTE_PREFIX))
            .filter(attr -> !validAttributes.contains(attr)).collect(Collectors.toList());
        for (MCRUserAttribute attribute : attributes) {
            final String reference = extractReference(attribute);
            if (deadAttributes.contains(attribute)) {
                removeAccessKeyFromContext(user, ACCESS_KEY_USER_ATTRIBUTE_PREFIX + reference);
            } else if (getService().findAccessKeyByReferenceAndSecret(reference, attribute.getValue()) != null) {
                validAttributes.add(attribute);
            } else {
                removeAccessKeyFromContext(user, ACCESS_KEY_USER_ATTRIBUTE_PREFIX + reference);
                deadAttributes.add(attribute);
            }
        }
    }

}
