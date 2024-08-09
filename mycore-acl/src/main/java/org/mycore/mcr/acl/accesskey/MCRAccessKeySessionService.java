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

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;

/**
 * Implements {@link MCRAccessKeyContextService} for {@link MCRSession} context.
 */
public class MCRAccessKeySessionService extends MCRAccessKeyContextService<MCRSession> {

    /**
     * Prefix for session attribute name for value.
     */
    public static final String ACCESS_KEY_SESSION_ATTRIBUTE_PREFIX = "acckey_";

    public MCRAccessKeySessionService(MCRAccessKeyService accessKeyService) {
        super(accessKeyService);
    }

    @Override
    MCRSession getCurrentContext() {
        return MCRSessionMgr.getCurrentSession();
    }

    @Override
    protected void addAccessKeyValueAttributToContext(MCRSession context, String attributeName, String value) {
        context.put(attributeName, value);
    }

    @Override
    protected boolean checkAddAccessKeyToContextIsAllowed(MCRAccessKeyDto accessKeyDto) {
        return MCRAccessKeyConfig.getAllowedSessionPermissionTypes().contains(accessKeyDto.getPermission());
    }

    @Override
    protected void deleteAccessKeyValueAttributeFromContext(MCRSession context, String attributeName) {
        context.deleteObject(attributeName);
    }

    @Override
    protected String getAccessKeyValueAttributeFromContext(MCRSession context, String attributeName) {
        return (String) context.get(attributeName);
    }

    @Override
    protected String getAttributePrefix() {
        return ACCESS_KEY_SESSION_ATTRIBUTE_PREFIX;
    }

}
