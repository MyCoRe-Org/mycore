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

package org.mycore.datamodel.metadata.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRJPATestHelper;
import org.mycore.test.MyCoReTest;

import jakarta.persistence.EntityManager;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.mods", string = "true")
})
public class MCRMetadataHistoryManagerTest {

    private static final Instant HISTORY_START = Instant.parse("2017-06-19T10:28:36.565Z");

    private MCRObjectID testObject;

    private Instant lastDelete;

    @Test
    public void testGetEmptyHistoryStart() {
        //MCR-1979
        assertFalse(MCRMetadataHistoryManager.getHistoryStart().isPresent(), "No earliest timestamp should be present");
    }

    @Test
    public void testGetHighestStoredID() {
        addTestData();
        assertEquals(testObject,
            MCRMetadataHistoryManager.getHighestStoredID(testObject.getProjectId(), testObject.getTypeId()).get());
    }

    @Test
    public void testGetHistoryStart() {
        addTestData();
        assertEquals(HISTORY_START, MCRMetadataHistoryManager.getHistoryStart().get());
    }

    @Test
    public void testGetDeletedItems() {
        addTestData();
        Map<MCRObjectID, Instant> deletedItems = MCRMetadataHistoryManager.getDeletedItems(Instant.ofEpochMilli(0),
            Optional.empty());
        assertEquals(1, deletedItems.size(), "Expected a single deletion event.");
    }

    @Test
    public void testGetLastDeletedDate() {
        addTestData();
        assertEquals(lastDelete, MCRMetadataHistoryManager.getLastDeletedDate(testObject).get());
    }

    private void addTestData() {
        testObject = MCRObjectID.getInstance("mir_mods_00000355");
        create(testObject, HISTORY_START);
        delete(testObject, Instant.parse("2017-06-19T10:34:27.915Z"));
        create(testObject, Instant.parse("2017-06-19T10:52:59.711Z"));
        lastDelete = Instant.parse("2017-06-19T10:52:59.718Z");
        delete(testObject, lastDelete);
        MCRJPATestHelper.startNewTransaction();
    }

    private void create(MCRObjectID id, Instant time) {
        MCRMetaHistoryItem created = MCRMetaHistoryItem.now(id, MCRMetadataHistoryEventType.CREATE);
        created.setTime(time);
        store(created);
    }

    private void delete(MCRObjectID id, Instant time) {
        MCRMetaHistoryItem deleted = MCRMetaHistoryItem.now(id, MCRMetadataHistoryEventType.DELETE);
        deleted.setTime(time);
        store(deleted);
    }

    private void store(MCRMetaHistoryItem item) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.persist(item);
    }

}
