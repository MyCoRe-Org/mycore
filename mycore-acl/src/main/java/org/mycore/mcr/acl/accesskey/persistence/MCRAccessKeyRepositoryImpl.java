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

package org.mycore.mcr.acl.accesskey.persistence;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

import jakarta.persistence.EntityManager;

/**
 * Implementation of the {@link MCRAccessKeyRepository} interface for managing access key entities.
 */
public class MCRAccessKeyRepositoryImpl implements MCRAccessKeyRepository {

    private static final String PARAM_REFERENCE = "reference";

    private static final String PARAM_PERMISSION = "permission";

    private static final String PARAM_UUID = "uuid";

    @Override
    public Collection<MCRAccessKey> findAll() {
        return getEntityManager().createNamedQuery("MCRAccessKey.findAll", MCRAccessKey.class).getResultList();
    }

    @Override
    public Collection<MCRAccessKey> findByReference(String reference) {
        return getEntityManager().createNamedQuery("MCRAccessKey.getWithObjectId", MCRAccessKey.class)
            .setParameter(PARAM_REFERENCE, reference).getResultList();
    }

    @Override
    public Collection<MCRAccessKey> findByReferenceAndPermission(String reference, String permission) {
        return getEntityManager().createNamedQuery("MCRAccessKey.getWithType", MCRAccessKey.class)
            .setParameter(PARAM_REFERENCE, reference).setParameter("type", permission).getResultList();
    }

    @Override
    public Optional<MCRAccessKey> findByUuid(UUID uuid) {
        return getEntityManager().createNamedQuery("MCRAccessKey.getWithUuid", MCRAccessKey.class)
            .setParameter(PARAM_UUID, uuid).getResultStream().findFirst();
    }

    @Override
    public Optional<MCRAccessKey> findByReferenceAndValue(String reference, String value) {
        return getEntityManager().createNamedQuery("MCRAccessKey.getWithSecret", MCRAccessKey.class)
            .setParameter(PARAM_REFERENCE, reference).setParameter("secret", value).getResultStream().findFirst();
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
        final EntityManager entityManager = getEntityManager();
        entityManager.remove(entityManager.merge(accessKey));
    }

    @Override
    public void deleteAll() {
        getEntityManager().createNamedQuery("MCRAccessKey.clear").executeUpdate();
    }

    @Override
    public long deleteByReference(String reference) {
        return getEntityManager().createNamedQuery("MCRAccessKey.clearWithObjectId")
            .setParameter(PARAM_REFERENCE, reference).executeUpdate();
    }

    @Override
    public long deleteByReferenceAndPermission(String reference, String permission) {
        return getEntityManager().createNamedQuery("MCRAccessKey.clearWithReferenceAndPermission")
            .setParameter(PARAM_REFERENCE, reference).setParameter(PARAM_PERMISSION, permission).executeUpdate();
    }

    @Override
    public boolean existsByReferenceAndValue(String reference, String value) {
        return findByReferenceAndValue(reference, value).isPresent();
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
