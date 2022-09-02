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

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

public class MCRObjectEntityManagerTest extends MCRJPATestCase {
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
        MCRObjectEntity entity = new MCRObjectEntity();
        entity.setId(MCRObjectID.getInstance("junit_test_00000001"));
        final EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        entityManager.persist(entity);
        startNewTransaction();
        Assert.assertNotEquals(0, getObjectEntities().size());
        MCRObjectEntityManager.removeAll();
        startNewTransaction();
        Assert.assertEquals(0, getObjectEntities().size());
    }

    private static List<MCRObjectEntity> getObjectEntities() {
        final TypedQuery<MCRObjectEntity> listEntities = MCREntityManagerProvider.getCurrentEntityManager().createQuery(
            "FROM MCRObjectEntity", MCRObjectEntity.class);
        final List<MCRObjectEntity> entities = listEntities.getResultList();
        return entities;
    }

    @Test
    public void getByID() {
        MCRObjectEntity entity = new MCRObjectEntity();
        entity.setId(MCRObjectID.getInstance("junit_test_00000001"));
        MCREntityManagerProvider.getCurrentEntityManager().persist(entity);
        final MCRObjectEntity byID = MCRObjectEntityManager.getByID(entity.getId());
        Assert.assertNotNull(byID);
        Assert.assertEquals(entity.getId(), byID.getId());
    }

    @Test
    public void update() {
        MCRObjectEntity entity = new MCRObjectEntity();
        entity.setId(MCRObjectID.getInstance("junit_test_00000001"));
        MCREntityManagerProvider.getCurrentEntityManager().persist(entity);
        final MCRObjectEntity loadedEntity = MCREntityManagerProvider.getCurrentEntityManager()
            .find(MCRObjectEntity.class, new MCRObjectIDPK(entity.getId()));
        startNewTransaction();
        MCRObject obj = new MCRObject();
        obj.setId(entity.getId());
        MCRObjectEntityManager.update(obj);
        final MCRObjectEntity updatedEntity = MCREntityManagerProvider.getCurrentEntityManager()
            .find(MCRObjectEntity.class, new MCRObjectIDPK(entity.getId()));
        Assert.assertNotEquals(loadedEntity.getCreateDate(), updatedEntity.getCreateDate());
        Assert.assertNotEquals(loadedEntity.getModifyDate(), updatedEntity.getModifyDate());
    }

    @Test
    public void create() {
        MCRObject obj = new MCRObject();
        obj.setId(MCRObjectID.getInstance("junit_test_00000001"));
        MCRObjectEntityManager.create(obj);
        final MCRObjectEntity entity = MCREntityManagerProvider.getCurrentEntityManager()
            .find(MCRObjectEntity.class, new MCRObjectIDPK(obj.getId()));
        Assert.assertNotNull(entity);
    }

    @Test
    public void remove() {
        MCRObjectEntity entity = new MCRObjectEntity();
        entity.setId(MCRObjectID.getInstance("junit_test_00000001"));
        final EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        entityManager.persist(entity);
        startNewTransaction();
        MCRObject obj = new MCRObject();
        obj.setId(entity.getId());
        MCRObjectEntityManager.remove(obj);
        final MCRObjectEntity removedEntity = MCREntityManagerProvider.getCurrentEntityManager()
            .find(MCRObjectEntity.class, new MCRObjectIDPK(obj.getId()));
        Assert.assertNull(removedEntity);
    }

    @Test
    public void delete() {
        MCRObjectEntity entity = new MCRObjectEntity();
        entity.setId(MCRObjectID.getInstance("junit_test_00000001"));
        final EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        entityManager.persist(entity);
        startNewTransaction();
        MCRObject obj = new MCRObject();
        obj.setId(entity.getId());
        final Instant now = Instant.now();
        MCRObjectEntityManager.delete(obj, now, "junit");
        final MCRObjectEntity deletedEntity = MCREntityManagerProvider.getCurrentEntityManager()
            .find(MCRObjectEntity.class, new MCRObjectIDPK(obj.getId()));
        Assert.assertNotNull(deletedEntity);
        Assert.assertEquals(now, deletedEntity.getDeleteDate());
        Assert.assertEquals("junit", deletedEntity.getDeletedBy());
    }
}
