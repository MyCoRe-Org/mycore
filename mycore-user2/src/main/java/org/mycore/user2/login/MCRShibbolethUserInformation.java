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

package org.mycore.user2.login;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mycore.common.MCRUserInformation;
import org.mycore.user2.MCRRealm;
import org.mycore.user2.MCRRealmFactory;
import org.mycore.user2.MCRUserAttributeMapper;
import org.mycore.user2.annotation.MCRUserAttribute;
import org.mycore.user2.annotation.MCRUserAttributeJavaConverter;
import org.mycore.user2.utils.MCRRolesConverter;

/**
 * 
 * @author Ren\u00E9 Adler (eagle)
 */
public class MCRShibbolethUserInformation implements MCRUserInformation {
    private String userId;

    private String realmId;

    private Map<String, Object> attributes;

    @MCRUserAttribute
    private String realName;

    private Set<String> roles = new HashSet<>();

    public MCRShibbolethUserInformation(String userId, String realmId, Map<String, Object> attributes)
        throws Exception {
        this.userId = userId;
        this.realmId = realmId;
        this.attributes = attributes;

        MCRUserAttributeMapper attributeMapper = MCRRealmFactory.getAttributeMapper(this.realmId);
        if (attributeMapper != null) {
            attributeMapper.mapAttributes(this, attributes);
        }
    }

    /* (non-Javadoc)
     * @see org.mycore.common.MCRUserInformation#getUserID()
     */
    @Override
    public String getUserID() {
        return userId + "@" + realmId;
    }

    /* (non-Javadoc)
     * @see org.mycore.common.MCRUserInformation#isUserInRole(java.lang.String)
     */
    @Override
    public boolean isUserInRole(String role) {
        return roles.contains(role);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.MCRUserInformation#getUserAttribute(java.lang.String)
     */
    @Override
    public String getUserAttribute(String attribute) {
        String key;
        switch (attribute) {
            case MCRUserInformation.ATT_REAL_NAME:
                return this.realName;
            case MCRRealm.USER_INFORMATION_ATTR:
                return this.realmId;
            default:
                key = attribute;
                break;
        }

        Object value = attributes.get(key);

        return value != null ? value.toString() : null;
    }

    // This is used for MCRUserAttributeMapper

    Collection<String> getRoles() {
        return roles;
    }

    @MCRUserAttribute(name = "roles", separator = ";")
    @MCRUserAttributeJavaConverter(MCRRolesConverter.class)
    void setRoles(Collection<String> roles) {
        this.roles.addAll(roles);
    }
}
