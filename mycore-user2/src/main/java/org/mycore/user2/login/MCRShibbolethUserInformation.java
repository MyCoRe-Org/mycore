/**
 * 
 */
package org.mycore.user2.login;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mycore.common.MCRUserInformation;

/**
 * @author daniel
 *
 */
public class MCRShibbolethUserInformation implements MCRUserInformation {
    private String userId;

    private Map<String, String> attributes;

    private Set<String> roles;

    public MCRShibbolethUserInformation(String userId, Set<String> roles, Map<String, String> attributes) {
        this.userId = userId;
        this.roles = new HashSet<>(roles);
        this.attributes = new HashMap<String, String>(attributes);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.MCRUserInformation#getUserID()
     */
    @Override
    public String getUserID() {
        return userId;
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
                key = "displayName";
                break;
            default:
                key = attribute;
                break;
        }
        return attributes.get(key);
    }

}
