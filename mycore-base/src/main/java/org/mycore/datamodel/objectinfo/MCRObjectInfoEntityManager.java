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

package org.mycore.datamodel.objectinfo;

import java.time.Instant;
import java.util.Optional;

import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.backend.jpa.MCRObjectIDPK;
import org.mycore.backend.jpa.objectinfo.MCRObjectInfoEntity;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;

import jakarta.persistence.EntityManager;

public class MCRObjectInfoEntityManager {

    static void removeAll(){
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.createQuery("delete from MCRObjectInfoEntity ").executeUpdate();
    }

    /**
     * loads a {@link MCRObjectInfoEntity} from the database
     * @param id the id of the {@link MCRObjectInfoEntity}
     * @return null if MCRObjectEntity is not found
     */
    static MCRObjectInfoEntity getByID(MCRObjectID id) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        return em.find(MCRObjectInfoEntity.class, new MCRObjectIDPK(id));
    }

    /**
     * updates an object info of an object, if the object does not have an object info entity yet, then it will be
     * created
     * @param obj the object which info should be updated
     */
    static void update(MCRObject obj) {
        MCRObjectID id = obj.getId();
        MCRObjectInfoEntity entity = getByID(id);
        if (entity != null) {
            applyMetadataToInfo(obj, entity);
        } else {
            create(obj);
        }
    }

    /**
     * creates a new {@link MCRObjectInfoEntity} for an object
     * @param obj the object
     */
    static void create(MCRObject obj) {
        MCRObjectInfoEntity entity = new MCRObjectInfoEntity();
        applyMetadataToInfo(obj, entity);
        MCREntityManagerProvider.getCurrentEntityManager().persist(entity);
    }

    /**
     * Removes object information from the Database
     * @param object the object info which should be removed from the database
     */
    static void remove(MCRObject object) {
        MCRObjectInfoEntity byID = getByID(object.getId());
        if (byID == null) {
            return;
        }
        MCREntityManagerProvider.getCurrentEntityManager().remove(byID);
    }

    /**
     * Updates the state of the object info to be deleted (does not {@link #remove(MCRObject)} the object)
     * @param object the object which is deleted
     * @param deletedBy the user which deleted the object
     * @param deletedDate the date at which the object was deleted
     */
    public static void delete(MCRObject object, Instant deletedDate, String deletedBy) {
        MCRObjectID id = object.getId();
        MCRObjectInfoEntity info = getByID(id);
        if (info == null) {
            info = new MCRObjectInfoEntity();
            info.setId(object.getId());
            MCREntityManagerProvider.getCurrentEntityManager().persist(info);
        }
        applyMetadataToInfo(object, info);
        applyDeleted(deletedDate, deletedBy, info);
    }

    private static void applyMetadataToInfo(MCRObject obj, MCRObjectInfoEntity info) {
        MCRObjectID id = obj.getId();
        info.setId(id);
        info.setObjectProject(id.getProjectId());
        info.setObjectType(id.getTypeId());
        info.setObjectNumber(id.getNumberAsInteger());
        MCRObjectService service = obj.getService();

        info.setCreateDate(service.getDate(MCRObjectService.DATE_TYPE_CREATEDATE).toInstant());
        info.setModifyDate(service.getDate(MCRObjectService.DATE_TYPE_MODIFYDATE).toInstant());
        service.getFlags(MCRObjectService.FLAG_TYPE_CREATEDBY).stream().findFirst().ifPresent(info::setCreatedBy);
        service.getFlags(MCRObjectService.FLAG_TYPE_MODIFIEDBY).stream().findFirst().ifPresent(info::setModifiedBy);
        info.setState(Optional.ofNullable(service.getState()).map(MCRCategoryID::toString).orElse(null));
        info.setDeletedBy(null);
        info.setDeleteDate(null);
    }

    private static void applyDeleted(Instant deletionDate, String deletedBy, MCRObjectInfoEntity entity) {
        entity.setDeleteDate(deletionDate);
        entity.setDeletedBy(deletedBy);
    }
}
