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

package org.mycore.datamodel.objectinfo;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.backend.jpa.MCRObjectIDPK;
import org.mycore.backend.jpa.objectinfo.MCRObjectInfoEntity;
import org.mycore.backend.jpa.objectinfo.MCRObjectInfoEntityManager;
import org.mycore.common.MCRJPATestCase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

public class MCRObjectInfoEntityManagerTest extends MCRJPATestCase {
    @Override
    protected Map<String, String> getTestProperties() {
        final Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        return testProperties;
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void removeAll() {
        MCRObjectInfoEntity entity = new MCRObjectInfoEntity();
        entity.setId(MCRObjectID.getInstance("junit_test_00000001"));
        final EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        entityManager.persist(entity);
        startNewTransaction();
        Assert.assertNotEquals(0, getObjectEntities().size());
        MCRObjectInfoEntityManager.removeAll();
        startNewTransaction();
        Assert.assertEquals(0, getObjectEntities().size());
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
        Assert.assertNotNull(byID);
        Assert.assertEquals(entity.getId(), byID.getId());
    }

    @Test
    public void update() {
        MCRObjectInfoEntity entity = new MCRObjectInfoEntity();
        entity.setId(MCRObjectID.getInstance("junit_test_00000001"));
        MCREntityManagerProvider.getCurrentEntityManager().persist(entity);
        final MCRObjectInfoEntity loadedEntity = MCREntityManagerProvider.getCurrentEntityManager()
            .find(MCRObjectInfoEntity.class, new MCRObjectIDPK(entity.getId()));
        startNewTransaction();
        MCRObject obj = new MCRObject();
        obj.setId(entity.getId());
        MCRObjectInfoEntityManager.update(obj);
        final MCRObjectInfoEntity updatedEntity = MCREntityManagerProvider.getCurrentEntityManager()
            .find(MCRObjectInfoEntity.class, new MCRObjectIDPK(entity.getId()));
        Assert.assertNotEquals(loadedEntity.getCreateDate(), updatedEntity.getCreateDate());
        Assert.assertNotEquals(loadedEntity.getModifyDate(), updatedEntity.getModifyDate());
    }

    @Test
    public void create() {
        MCRObject obj = new MCRObject();
        obj.setId(MCRObjectID.getInstance("junit_test_00000001"));
        MCRObjectInfoEntityManager.create(obj);
        final MCRObjectInfoEntity entity = MCREntityManagerProvider.getCurrentEntityManager()
            .find(MCRObjectInfoEntity.class, new MCRObjectIDPK(obj.getId()));
        Assert.assertNotNull(entity);
    }

    @Test
    public void remove() {
        MCRObjectInfoEntity entity = new MCRObjectInfoEntity();
        entity.setId(MCRObjectID.getInstance("junit_test_00000001"));
        final EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        entityManager.persist(entity);
        startNewTransaction();
        MCRObject obj = new MCRObject();
        obj.setId(entity.getId());
        MCRObjectInfoEntityManager.remove(obj);
        final MCRObjectInfoEntity removedEntity = MCREntityManagerProvider.getCurrentEntityManager()
            .find(MCRObjectInfoEntity.class, new MCRObjectIDPK(obj.getId()));
        Assert.assertNull(removedEntity);
    }

    @Test
    public void delete() {
        MCRObjectInfoEntity entity = new MCRObjectInfoEntity();
        entity.setId(MCRObjectID.getInstance("junit_test_00000001"));
        final EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        entityManager.persist(entity);
        startNewTransaction();
        MCRObject obj = new MCRObject();
        obj.setId(entity.getId());
        final Instant now = Instant.now();
        MCRObjectInfoEntityManager.delete(obj, now, "junit");
        final MCRObjectInfoEntity deletedEntity = MCREntityManagerProvider.getCurrentEntityManager()
            .find(MCRObjectInfoEntity.class, new MCRObjectIDPK(obj.getId()));
        Assert.assertNotNull(deletedEntity);
        Assert.assertEquals(now, deletedEntity.getDeleteDate());
        Assert.assertEquals("junit", deletedEntity.getDeletedBy());
    }
}
