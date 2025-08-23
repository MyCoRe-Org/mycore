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

package org.mycore.datamodel.objectinfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.backend.jpa.MCRObjectIDPK;
import org.mycore.backend.jpa.objectinfo.MCRObjectInfoEntity;
import org.mycore.backend.jpa.objectinfo.MCRObjectInfoEntityManager;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRJPATestHelper;
import org.mycore.test.MyCoReTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true")
})
public class MCRObjectInfoEntityManagerTest {

    @Test
    public void removeAll() {
        MCRObjectInfoEntity entity = new MCRObjectInfoEntity();
        entity.setId(MCRObjectID.getInstance("junit_test_00000001"));
        final EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        entityManager.persist(entity);
        MCRJPATestHelper.startNewTransaction();
        assertNotEquals(0, getObjectEntities().size());
        MCRObjectInfoEntityManager.removeAll();
        MCRJPATestHelper.startNewTransaction();
        assertEquals(0, getObjectEntities().size());
    }

    private static List<MCRObjectInfoEntity> getObjectEntities() {
        final TypedQuery<MCRObjectInfoEntity> listEntities = MCREntityManagerProvider.getCurrentEntityManager()
            .createQuery("FROM MCRObjectInfoEntity", MCRObjectInfoEntity.class);
        final List<MCRObjectInfoEntity> entities = listEntities.getResultList();
        return entities;
    }

    @Test
    public void getByID() {
        MCRObjectInfoEntity entity = new MCRObjectInfoEntity();
        entity.setId(MCRObjectID.getInstance("junit_test_00000001"));
        MCREntityManagerProvider.getCurrentEntityManager().persist(entity);
        final MCRObjectInfoEntity byID = MCRObjectInfoEntityManager.getByID(entity.getId());
        assertNotNull(byID);
        assertEquals(entity.getId(), byID.getId());
    }

    @Test
    public void update() {
        MCRObjectInfoEntity entity = new MCRObjectInfoEntity();
        entity.setId(MCRObjectID.getInstance("junit_test_00000001"));
        MCREntityManagerProvider.getCurrentEntityManager().persist(entity);
        final MCRObjectInfoEntity loadedEntity = MCREntityManagerProvider.getCurrentEntityManager()
            .find(MCRObjectInfoEntity.class, new MCRObjectIDPK(entity.getId()));
        MCRJPATestHelper.startNewTransaction();
        MCRObject obj = new MCRObject();
        obj.setId(entity.getId());
        MCRObjectInfoEntityManager.update(obj);
        final MCRObjectInfoEntity updatedEntity = MCREntityManagerProvider.getCurrentEntityManager()
            .find(MCRObjectInfoEntity.class, new MCRObjectIDPK(entity.getId()));
        assertNotEquals(loadedEntity.getCreateDate(), updatedEntity.getCreateDate());
        assertNotEquals(loadedEntity.getModifyDate(), updatedEntity.getModifyDate());
    }

    @Test
    public void create() {
        MCRObject obj = new MCRObject();
        obj.setId(MCRObjectID.getInstance("junit_test_00000001"));
        MCRObjectInfoEntityManager.create(obj);
        final MCRObjectInfoEntity entity = MCREntityManagerProvider.getCurrentEntityManager()
            .find(MCRObjectInfoEntity.class, new MCRObjectIDPK(obj.getId()));
        assertNotNull(entity);
    }

    @Test
    public void remove() {
        MCRObjectInfoEntity entity = new MCRObjectInfoEntity();
        entity.setId(MCRObjectID.getInstance("junit_test_00000001"));
        final EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        entityManager.persist(entity);
        MCRJPATestHelper.startNewTransaction();
        MCRObject obj = new MCRObject();
        obj.setId(entity.getId());
        MCRObjectInfoEntityManager.remove(obj);
        final MCRObjectInfoEntity removedEntity = MCREntityManagerProvider.getCurrentEntityManager()
            .find(MCRObjectInfoEntity.class, new MCRObjectIDPK(obj.getId()));
        assertNull(removedEntity);
    }

    @Test
    public void delete() {
        MCRObjectInfoEntity entity = new MCRObjectInfoEntity();
        entity.setId(MCRObjectID.getInstance("junit_test_00000001"));
        final EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        entityManager.persist(entity);
        MCRJPATestHelper.startNewTransaction();
        MCRObject obj = new MCRObject();
        obj.setId(entity.getId());
        final Instant now = Instant.now();
        MCRObjectInfoEntityManager.delete(obj, now, "junit");
        final MCRObjectInfoEntity deletedEntity = MCREntityManagerProvider.getCurrentEntityManager()
            .find(MCRObjectInfoEntity.class, new MCRObjectIDPK(obj.getId()));
        assertNotNull(deletedEntity);
        assertEquals(now, deletedEntity.getDeleteDate());
        assertEquals("junit", deletedEntity.getDeletedBy());
    }
}
