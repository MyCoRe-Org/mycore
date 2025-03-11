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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.mycore.access.MCRAccessCacheHelper;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.metadata.MCRDerivate;
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
     * Activates a access key for reference by secret to current context.
     *
     * @param reference the object or resource reference for which the access key will be activated
     * @param secret the secret to active access key
     * @throws UnsupportedOperationException if the reference is not supported
     * @throws MCRAccessKeyException if no access key has been activated
     */
    public void activateAccessKey(String reference, String secret) {
        if (MCRObjectID.isValid(reference)) {
            activateAccessKeyForObject(MCRObjectID.getInstance(reference), secret);
        } else {
            throw new UnsupportedOperationException("Reference: " + reference + " is not supported");
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
            .map(v -> accessKeyService.findAccessKeyByReferenceAndSecret(reference, v)).orElse(null);
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

    private void addAccessKeyForObjectToContext(T context, MCRObjectID objectId, String secret) {
        final String processedSecret = accessKeyService.processSecret(objectId.toString(), secret);
        final MCRAccessKeyDto accessKeyDto
            = accessKeyService.findAccessKeyByReferenceAndSecret(objectId.toString(), processedSecret);
        if (accessKeyDto == null) {
            throw new MCRAccessKeyNotFoundException("Access key is invalid.");
        } else if (isAccessKeyAllowedInContext(accessKeyDto)) {
            setAccessKeyAttribute(context, getAttributeName(objectId.toString()), processedSecret);
            MCRAccessManager.invalidPermissionCacheByID(objectId.toString());
        } else {
            throw new MCRAccessKeyException("Access key is not allowed.");
        }
    }

    private void activateAccessKeyForObject(MCRObjectID objectId, String secret) {
        if (!MCRMetadataManager.exists(objectId)) {
            throw new MCRAccessKeyException("No access key could be activated");
        }
        final T context = getCurrentContext();
        if (MCRDerivate.OBJECT_TYPE.equals(objectId.getTypeId())) {
            addAccessKeyForObjectToContext(context, objectId, secret);
        } else {
            boolean success = false;
            try {
                addAccessKeyForObjectToContext(context, objectId, secret);
                MCRAccessCacheHelper.clearPermissionCache(objectId.toString());
                success = true;
            } catch (MCRAccessKeyException e) {
                // ignored
            }
            final List<MCRObjectID> derivateIds = MCRMetadataManager.getDerivateIds(objectId, 0, TimeUnit.SECONDS);
            for (final MCRObjectID derivateId : derivateIds) {
                try {
                    addAccessKeyForObjectToContext(context, derivateId, secret);
                    success = true;
                } catch (MCRAccessKeyException e) {
                    // ignored
                }
            }
            if (!success) {
                throw new MCRAccessKeyException("No access key could be activated");
            }
        }
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
     * @param secret the secret of the access key
     */
    abstract void setAccessKeyAttribute(T context, String attributeName, String secret);

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
     * Retrieves the secret of the access key attribute from the given context.
     *
     * @param context the context from which the attribute secret is retrieved
     * @param attributeName the name of the attribute
     * @return the secret of the access key attribute, or {@code null} if no attribute is found
     */
    abstract String getAccessKeyFromContext(T context, String attributeName);

    /**
     * Returns the prefix to be used for access key attribute names.
     *
     * @return the prefix for attribute names
     */
    abstract String getAccessKeyAttributePrefix();
}
