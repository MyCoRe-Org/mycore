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

import java.util.Collection;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

public class MCRAccessKeyDAO {

    public Collection<MCRAccessKey> findAll(MCRObjectID objectId) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        final List<MCRAccessKey> accessKeys = em.createNamedQuery("MCRAccessKey.getWithObjectId", MCRAccessKey.class)
            .setParameter("objectId", objectId)
            .getResultList();
        for (MCRAccessKey accessKey : accessKeys) {
            em.detach(accessKey);
        }
        return accessKeys;
    }

    public MCRAccessKey findBySecret(MCRObjectID objectId, String secret) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        final MCRAccessKey accessKey = em.createNamedQuery("MCRAccessKey.getWithSecret", MCRAccessKey.class)
            .setParameter("objectId", objectId)
            .setParameter("secret", secret)
            .getResultList()
            .stream()
            .findFirst()
            .orElse(null);
        if (accessKey != null) {
            em.detach(accessKey);
        }
        return accessKey;
    }

    public Collection<MCRAccessKey> findAllByType(MCRObjectID objectId, String type) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        final List<MCRAccessKey> accessKeys = em.createNamedQuery("MCRAccessKey.getWithType", MCRAccessKey.class)
            .setParameter("objectId", objectId)
            .setParameter("type", type)
            .getResultList();
        for (MCRAccessKey accessKey : accessKeys) {
            em.detach(accessKey);
        }
        return accessKeys;

    }

    public void create(MCRAccessKey accessKey) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.persist(accessKey);
        em.detach(accessKey);
    }

    public void update(MCRAccessKey accessKey) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.merge(accessKey);
    }

    public void delete(MCRAccessKey accessKey) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.remove(em.contains(accessKey) ? accessKey : em.merge(accessKey));
    }

    public void deleteAll() {
        MCREntityManagerProvider.getCurrentEntityManager()
            .createNamedQuery("MCRAccessKey.clear")
            .executeUpdate();
    }

    public void deleteAll(MCRObjectID objectId) {
        MCREntityManagerProvider.getCurrentEntityManager()
            .createNamedQuery("MCRAccessKey.clearWithObjectId")
            .setParameter("objectId", objectId)
            .executeUpdate();
    }
}
