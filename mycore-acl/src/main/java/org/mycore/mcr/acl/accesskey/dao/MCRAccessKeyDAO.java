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

package org.mycore.mcr.acl.accesskey.dao;

import java.util.Collection;

import jakarta.persistence.EntityManager;

import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

/**
 * DAO for {@link MCRAccessKey}.
 */
public class MCRAccessKeyDAO {

    /**
     * Returns {@link MCRAccessKey} Collection for {@link MCRObjectID}.
     * 
     * @param objectId the MCRObjectID
     * @return MCRAccessKey Collection
     */
    public Collection<MCRAccessKey> findAll(MCRObjectID objectId) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        return em.createNamedQuery("MCRAccessKey.getWithObjectId", MCRAccessKey.class)
            .setParameter("objectId", objectId)
            .getResultList()
            .stream()
            .peek(a -> em.detach(a))
            .toList();
    }

    /**
     * Returns {@link MCRAccessKey} for {@link MCRObjectID} and secret.
     * 
     * @param objectId the MCRObjectID
     * @param secret the secret
     * @return the MCRAccessKey or null
     */
    public MCRAccessKey findBySecret(MCRObjectID objectId, String secret) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        return em.createNamedQuery("MCRAccessKey.getWithSecret", MCRAccessKey.class)
            .setParameter("objectId", objectId)
            .setParameter("secret", secret)
            .getResultList()
            .stream()
            .peek(a -> em.detach(a))
            .findFirst()
            .orElse(null);
    }

    /**
     * Returns {@link MCRAccessKey} for {@link MCRObjectID} specified by type.
     * 
     * @param objectId the MCRObjectID
     * @param type the type
     * @return MCRAccessKey Collection
     */
    public Collection<MCRAccessKey> findAllByType(MCRObjectID objectId, String type) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        return em.createNamedQuery("MCRAccessKey.getWithType", MCRAccessKey.class)
            .setParameter("objectId", objectId)
            .setParameter("type", type)
            .getResultList()
            .stream()
            .peek(a -> em.detach(a))
            .toList();
    }

    /**
     * Creates {@link MCRAccessKey}.
     * 
     * @param accessKey the MCRAccessKey
     */
    public void create(MCRAccessKey accessKey) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.persist(accessKey);
        em.detach(accessKey);
    }

    /**
     * Updates {@link MCRAccessKey}.
     * 
     * @param accessKey the MCRAccessKey
     */
    public void update(MCRAccessKey accessKey) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.merge(accessKey);
    }

    /**
     * Deletes {@link MCRAccessKey}.
     * 
     * @param accessKey the MCRAccessKey
     */
    public void delete(MCRAccessKey accessKey) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.remove(em.contains(accessKey) ? accessKey : em.merge(accessKey));
    }

    /**
     * Deletes all {@link MCRAccessKey}
     */
    public void deleteAll() {
        MCREntityManagerProvider.getCurrentEntityManager()
            .createNamedQuery("MCRAccessKey.clear")
            .executeUpdate();
    }

    /**
     * Deletes all {@link MCRAccessKey} for {@link MCRObjectID}.
     * 
     * @param objectId the MCRObjectID
     */
    public void deleteAll(MCRObjectID objectId) {
        MCREntityManagerProvider.getCurrentEntityManager()
            .createNamedQuery("MCRAccessKey.clearWithObjectId")
            .setParameter("objectId", objectId)
            .executeUpdate();
    }
}
