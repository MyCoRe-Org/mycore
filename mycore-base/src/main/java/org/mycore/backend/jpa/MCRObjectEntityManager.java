/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  MyCoRe is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MyCoRe is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.backend.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;

import java.time.Instant;
import java.util.Optional;

public class MCRObjectEntityManager {

    static void removeAll(){
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.createQuery("delete from MCRObjectEntity").executeUpdate();
    }

    static MCRObjectEntity getByID(MCRObjectID id) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRObjectEntity> getQuery = cb.createQuery(MCRObjectEntity.class);
        Root<MCRObjectEntity> object = getQuery.from(MCRObjectEntity.class);
        CriteriaQuery<MCRObjectEntity> objectByIDCriteriaQuery = getQuery.select(object)
            .where(cb.equal(object.get(MCRObjectEntity_.objectId), id.toString()));
        TypedQuery<MCRObjectEntity> typedQuery = em.createQuery(objectByIDCriteriaQuery);

        return typedQuery.getSingleResult();
    }

    /**
     * updates the entity of the object, if the object does not have a entity yet, then it will be created
     * @param obj
     */
    static void update(MCRObject obj) {
        MCRObjectID id = obj.getId();
        MCRObjectEntity entity = null;
        try {
            entity = getByID(id);
            applyMetadataToEntity(obj, entity);
            MCREntityManagerProvider.getCurrentEntityManager().persist(entity);
        } catch (NoResultException noResultException) {
            create(obj);
        }
    }

    /**
     * creates a new entity for the object
     * @param obj
     */
    static void create(MCRObject obj) {
        MCRObjectEntity entity = new MCRObjectEntity();
        applyMetadataToEntity(obj, entity);
        MCREntityManagerProvider.getCurrentEntityManager().persist(entity);
    }

    /**
     * Removes the entity of the object from the Database
     * @param object
     */
    static void remove(MCRObject object) {
        MCRObjectEntity byID = getByID(object.getId());
        MCREntityManagerProvider.getCurrentEntityManager().remove(byID);
    }

    /**
     * Updates the state of the entity to be deleted (does not {@link #remove(MCRObject)} the object)
     * @param object
     */
    public static void delete(MCRObject object, Instant deletedDate, String deletedBy) {
        MCRObjectID id = object.getId();
        MCRObjectEntity entity = getByID(id);
        applyMetadataToEntity(object, entity);
        applyDeleted(deletedDate, deletedBy, entity);
        MCREntityManagerProvider.getCurrentEntityManager().persist(entity);
    }

    private static void applyMetadataToEntity(MCRObject obj, MCRObjectEntity entity) {
        MCRObjectID id = obj.getId();
        entity.setObjectId(id.toString());
        entity.setObjectProject(id.getProjectId());
        entity.setObjectType(id.getTypeId());
        entity.setObjectNumber(id.getNumberAsInteger());
        MCRObjectService service = obj.getService();

        entity.setCreateDate(service.getDate(MCRObjectService.DATE_TYPE_CREATEDATE).toInstant());
        entity.setModifyDate(service.getDate(MCRObjectService.DATE_TYPE_MODIFYDATE).toInstant());
        entity.setCreatedBy(service.getFlags(MCRObjectService.FLAG_TYPE_CREATEDBY).stream().findFirst().get());
        entity.setModifiedBy(service.getFlags(MCRObjectService.FLAG_TYPE_MODIFIEDBY).stream().findFirst().get());
        entity.setState(Optional.ofNullable(service.getState()).map(MCRCategoryID::toString).orElse(null));
        entity.setDeletedBy(null);
        entity.setDeleteddate(null);
    }

    private static void applyDeleted(Instant deletionDate, String deletedBy, MCRObjectEntity entity) {
        entity.setDeleteddate(deletionDate);
        entity.setDeletedBy(deletedBy);
    }
}
