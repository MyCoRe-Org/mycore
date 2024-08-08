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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.mycore.access.MCRAccessCacheHelper;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;

/**
 * Service that provides methods to active or deactivate access key for context.
 *
 * @param <T> the context
 */
public abstract class MCRAccessKeyContextService<T> {

    private final MCRAccessKeyService accessKeyService;

    /**
     * Constructs new {@link MCRAccessKeyContextService} instance.
     *
     * @param accessKeyService the access key service
     */
    public MCRAccessKeyContextService(MCRAccessKeyService accessKeyService) {
        this.accessKeyService = accessKeyService;
    }

    /**
     * Activates a access key for object by raw value to current context.
     *
     * @param objectId the object ID
     * @param rawValue the raw value to active access key
     * @throws MCRAccessKeyException if no access key has been activated
     */
    public void activeObjectAccessKeyForReferenceByRawValue(MCRObjectID objectId, String rawValue) {
        if (!MCRMetadataManager.exists(objectId)) {
            throw new MCRAccessKeyException("No access key could be activated");
        }
        final T context = getCurrentContext();
        if ("derivate".equals(objectId.getTypeId())) {
            addObjectAccessKeyToContext(context, objectId, rawValue);
        } else {
            boolean success = false;
            try {
                addObjectAccessKeyToContext(context, objectId, rawValue);
                MCRAccessCacheHelper.clearPermissionCache(objectId.toString());
                success = true;
            } catch (MCRAccessKeyException e) {
                //
            }
            final List<MCRObjectID> derivateIds = MCRMetadataManager.getDerivateIds(objectId, 0, TimeUnit.SECONDS);
            for (final MCRObjectID derivateId : derivateIds) {
                try {
                    addObjectAccessKeyToContext(context, derivateId, rawValue);
                    success = true;
                } catch (MCRAccessKeyException e) {
                    //
                }
            }
            if (!success) {
                throw new MCRAccessKeyException("No access key could be activated");
            }
        }
    }

    private void addObjectAccessKeyToContext(T context, MCRObjectID objectId, String rawValue) {
        final String value = accessKeyService.getValue(objectId.toString(), rawValue);
        final MCRAccessKeyDto accessKeyDto
            = accessKeyService.getAccessKeyByReferenceAndValue(objectId.toString(), value);
        if (accessKeyDto == null) {
            throw new MCRAccessKeyNotFoundException("Access key is invalid.");
        } else if (checkAddAccessKeyToContextIsAllowed(accessKeyDto)) {
            addAccessKeyValueAttributToContext(context, getAttributeName(objectId.toString()), value);
            MCRAccessManager.invalidPermissionCacheByID(objectId.toString());
        } else {
            throw new MCRAccessKeyException("Access key is not allowed.");
        }
    }

    /**
     * Returns an access key for an reference of the current context.
     *
     * @param reference the reference
     * @return access key or null
     */
    public MCRAccessKeyDto getActivatedAccessKeyForReference(String reference) {
        return Optional
            .ofNullable(getAccessKeyValueAttributeFromContext(getCurrentContext(), getAttributeName(reference)))
            .map(v -> accessKeyService.getAccessKeyByReferenceAndValue(reference, v)).orElse(null);
    }

    /**
     * Deactivates access key for reference for current context.
     *
     * @param reference the reference
     */
    public void deactivateAccessKeyForReference(String reference) {
        deleteAccessKeyValueAttributeFromContext(getCurrentContext(), getAttributeName(reference));
        MCRAccessCacheHelper.clearPermissionCache(reference);
    }

    /**
     * Returns the {@link MCRAccessKeyService}.
     *
     * @return the service.
     */
    protected MCRAccessKeyService getService() {
        return accessKeyService;
    }

    private String getAttributeName(String reference) {
        return getAttributePrefix() + reference;
    }

    /**
     * Returns the current context.
     *
     * @return the context.
     */
    abstract T getCurrentContext();

    /**
     * Adds attribute by name to context with value.
     *
     * @param context the context
     * @param attributeName the attribute name
     * @param value the value
     */
    abstract void addAccessKeyValueAttributToContext(T context, String attributeName, String value);

    /**
     * Checks if add access key is allowed to add to context.
     *
     * @param accessKeyDto the access key DTO
     * @return
     */
    abstract boolean checkAddAccessKeyToContextIsAllowed(MCRAccessKeyDto accessKeyDto);

    /**
     * Deletes attribute by name from context.
     *
     * @param context the context
     * @param attributeName the attribute name
     */
    abstract void deleteAccessKeyValueAttributeFromContext(T context, String attributeName);

    /**
     * Returns attribute value by name from context.
     *
     * @param context the context
     * @param attributeName the attribute name
     * @return the attribute value
     */
    abstract String getAccessKeyValueAttributeFromContext(T context, String attributeName);

    /**
     * Returns attribute name prefix value.
     *
     * @return the attribute name prefix
     */
    abstract String getAttributePrefix();
}
