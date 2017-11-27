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

package org.mycore.user2;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRTransientUser extends MCRUser {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger(MCRTransientUser.class);

    private MCRUserInformation userInfo;

    public MCRTransientUser(MCRUserInformation userInfo) {
        super();
        this.userInfo = userInfo;
        setLocked(true);

        String userName = userInfo.getUserID();
        if (userName.contains("@"))
            userName = userName.substring(0, userName.indexOf("@"));
        String realmId = getUserAttribute(MCRRealm.USER_INFORMATION_ATTR);

        super.setUserName(userName);
        super.setRealmID(realmId);
        super.setLastLogin(new Date(MCRSessionMgr.getCurrentSession().getLoginTime()));

        if (realmId != null && !MCRRealmFactory.getLocalRealm().equals(MCRRealmFactory.getRealm(realmId))) {
            MCRUserAttributeMapper attributeMapper = MCRRealmFactory.getAttributeMapper(realmId);
            if (attributeMapper != null) {
                Map<String, Object> attributes = new HashMap<>();
                for (String key : attributeMapper.getAttributeNames()) {
                    attributes.put(key, userInfo.getUserAttribute(key));
                }

                try {
                    attributeMapper.mapAttributes(this, attributes);
                } catch (Exception e) {
                    throw new MCRException(e.getMessage(), e);
                }
            }
        } else {
            super.setRealName(getUserAttribute(MCRUserInformation.ATT_REAL_NAME));
            for (MCRRole role : MCRRoleManager.listSystemRoles()) {
                LOGGER.debug("Test is in role: {}", role.getName());
                if (userInfo.isUserInRole(role.getName())) {
                    assignRole(role.getName());
                }
            }

        }
    }

    @Override
    public String getUserAttribute(String attribute) {
        return this.userInfo.getUserAttribute(attribute);
    }

}
