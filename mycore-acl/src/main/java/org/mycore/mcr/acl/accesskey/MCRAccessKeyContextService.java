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
     * Activates a access key for reference by raw value to current context.
     *
     * @param reference the object or resource reference for which the access key will be activated
     * @param rawValue the raw value to active access key
     * @throws UnsupportedOperationException if the reference is not supported
     * @throws MCRAccessKeyException if no access key has been activated
     */
    public void activateAccessKey(String reference, String rawValue) {
        if (MCRObjectID.isValid(reference)) {
            activateAccessKeyForObject(MCRObjectID.getInstance(reference), rawValue);
        } else {
            throw new UnsupportedOperationException("Reference: " + reference + " is not supported");
        }
    }

    private void activateAccessKeyForObject(MCRObjectID objectId, String rawValue) {
        if (!MCRMetadataManager.exists(objectId)) {
            throw new MCRAccessKeyException("No access key could be activated");
        }
        final T context = getCurrentContext();
        if ("derivate".equals(objectId.getTypeId())) {
            addAccessKeyForObjectToContext(context, objectId, rawValue);
        } else {
            boolean success = false;
            try {
                addAccessKeyForObjectToContext(context, objectId, rawValue);
                MCRAccessCacheHelper.clearPermissionCache(objectId.toString());
                success = true;
            } catch (MCRAccessKeyException e) {
                //
            }
            final List<MCRObjectID> derivateIds = MCRMetadataManager.getDerivateIds(objectId, 0, TimeUnit.SECONDS);
            for (final MCRObjectID derivateId : derivateIds) {
                try {
                    addAccessKeyForObjectToContext(context, derivateId, rawValue);
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

    private void addAccessKeyForObjectToContext(T context, MCRObjectID objectId, String rawValue) {
        final String value = accessKeyService.processValue(objectId.toString(), rawValue);
        final MCRAccessKeyDto accessKeyDto
            = accessKeyService.findAccessKeyByReferenceAndValue(objectId.toString(), value);
        if (accessKeyDto == null) {
            throw new MCRAccessKeyNotFoundException("Access key is invalid.");
        } else if (isAccessKeyAllowedInContext(accessKeyDto)) {
            setAccessKeyAttribute(context, getAttributeName(objectId.toString()), value);
            MCRAccessManager.invalidPermissionCacheByID(objectId.toString());
        } else {
            throw new MCRAccessKeyException("Access key is not allowed.");
        }
    }

    /**
     * Retrieves the currently activated access key for the given reference.
     *
     * @param reference the reference for which the access key is retrieved
     * @return the activated {@link MCRAccessKeyDto}, or {@code null} if no key is active
     */
    public MCRAccessKeyDto findActiveAccessKey(String reference) {
        return Optional.ofNullable(getAccessKeyFromContext(getCurrentContext(), getAttributeName(reference)))
            .map(v -> accessKeyService.findAccessKeyByReferenceAndValue(reference, v)).orElse(null);
    }

    /**
     * Deactivates the access key for the given reference in the current context.
     *
     * @param reference the reference for which the access key will be deactivated
     */
    public void deactivateAccessKey(String reference) {
        removeAccessKeyFromContext(getCurrentContext(), getAttributeName(reference));
        MCRAccessCacheHelper.clearPermissionCache(reference);
    }

    /**
     * Returns the {@link MCRAccessKeyService} used to manage access keys.
     *
     * @return the access key service
     */
    protected MCRAccessKeyService getService() {
        return accessKeyService;
    }

    private String getAttributeName(String reference) {
        return getAccessKeyAttributePrefix() + reference;
    }

    /**
     * Returns the current context in which access keys are managed.
     *
     * @return the current context
     */
    abstract T getCurrentContext();

    /**
     * Adds an access key attribute to the given context.
     *
     * @param context the context in which the access key attribute is added
     * @param attributeName the name of the attribute to be added
     * @param value the value of the access key
     */
    abstract void setAccessKeyAttribute(T context, String attributeName, String value);

    /**
     * Checks if adding the access key to the context is allowed based on the provided access key DTO.
     *
     * @param accessKeyDto the access key data transfer object
     * @return {@code true} if adding the access key is allowed, {@code false} otherwise
     */
    abstract boolean isAccessKeyAllowedInContext(MCRAccessKeyDto accessKeyDto);

    /**
     * Deletes the access key attribute from the given context.
     *
     * @param context the context from which the access key attribute is removed
     * @param attributeName the name of the attribute to be removed
     */
    abstract void removeAccessKeyFromContext(T context, String attributeName);

    /**
     * Retrieves the value of the access key attribute from the given context.
     *
     * @param context the context from which the attribute value is retrieved
     * @param attributeName the name of the attribute
     * @return the value of the access key attribute, or {@code null} if no attribute is found
     */
    abstract String getAccessKeyFromContext(T context, String attributeName);

    /**
     * Returns the prefix to be used for access key attribute names.
     *
     * @return the prefix for attribute names
     */
    abstract String getAccessKeyAttributePrefix();
}
