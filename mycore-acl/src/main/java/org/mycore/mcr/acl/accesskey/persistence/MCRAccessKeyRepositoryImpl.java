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

package org.mycore.mcr.acl.accesskey.persistence;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKeyNamedQueries;

import jakarta.persistence.EntityManager;

/**
 * Implementation of the {@link MCRAccessKeyRepository} interface for managing access key entities.
 */
public class MCRAccessKeyRepositoryImpl implements MCRAccessKeyRepository {

    @Override
    public Collection<MCRAccessKey> findAll() {
        return getEntityManager().createNamedQuery(MCRAccessKeyNamedQueries.NAME_FIND_ALL, MCRAccessKey.class)
            .getResultList();
    }

    @Override
    public Collection<MCRAccessKey> findByReference(String reference) {
        return getEntityManager()
            .createNamedQuery(MCRAccessKeyNamedQueries.NAME_FIND_BY_REFERENCE, MCRAccessKey.class)
            .setParameter(MCRAccessKeyNamedQueries.PARAM_REFERENCE, reference).getResultList();
    }

    @Override
    public Collection<MCRAccessKey> findByType(String type) {
        return getEntityManager()
            .createNamedQuery(MCRAccessKeyNamedQueries.NAME_FIND_BY_TYPE, MCRAccessKey.class)
            .setParameter(MCRAccessKeyNamedQueries.PARAM_TYPE, type).getResultList();
    }

    @Override
    public Collection<MCRAccessKey> findByReferenceAndType(String reference, String type) {
        return getEntityManager()
            .createNamedQuery(MCRAccessKeyNamedQueries.NAME_FIND_BY_REFERENCE_AND_TYPE, MCRAccessKey.class)
            .setParameter(MCRAccessKeyNamedQueries.PARAM_REFERENCE, reference)
            .setParameter(MCRAccessKeyNamedQueries.PARAM_TYPE, type).getResultList();
    }

    @Override
    public Optional<MCRAccessKey> findByUuid(UUID uuid) {
        return getEntityManager().createNamedQuery(MCRAccessKeyNamedQueries.NAME_FIND_BY_UUID, MCRAccessKey.class)
            .setParameter(MCRAccessKeyNamedQueries.PARAM_UUID, uuid).getResultStream().findFirst();
    }

    @Override
    public Optional<MCRAccessKey> findByReferenceAndSecret(String reference, String secret) {
        return getEntityManager()
            .createNamedQuery(MCRAccessKeyNamedQueries.NAME_FIND_BY_REFERENCE_AND_SECRET, MCRAccessKey.class)
            .setParameter(MCRAccessKeyNamedQueries.PARAM_REFERENCE, reference)
            .setParameter(MCRAccessKeyNamedQueries.PARAM_SECRET, secret).getResultStream().findFirst();
    }

    @Override
    public MCRAccessKey save(MCRAccessKey accessKey) {
        if (accessKey.getId() == null) {
            getEntityManager().persist(accessKey);
            return accessKey;
        } else {
            return getEntityManager().merge(accessKey);
        }
    }

    @Override
    public void delete(MCRAccessKey accessKey) {
        getEntityManager().remove(getEntityManager().merge(accessKey));
    }

    @Override
    public boolean existsByReferenceAndSecret(String reference, String secret) {
        return findByReferenceAndSecret(reference, secret).isPresent();
    }

    @Override
    public void detach(MCRAccessKey accessKey) {
        getEntityManager().detach(accessKey);
    }

    @Override
    public void flush() {
        getEntityManager().flush();
    }

    private EntityManager getEntityManager() {
        return MCREntityManagerProvider.getCurrentEntityManager();
    }

}
