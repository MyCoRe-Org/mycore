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

package org.mycore.datamodel.metadata.history;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRJPATestCase;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRMetadataHistoryManagerTest extends MCRJPATestCase {

    private static final Instant HISTORY_START = Instant.parse("2017-06-19T10:28:36.565Z");

    private MCRObjectID testObject;

    private Instant lastDelete;

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.mods", Boolean.TRUE.toString());
        return testProperties;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        testObject = MCRObjectID.getInstance("mir_mods_00000355");
        create(testObject, HISTORY_START);
        delete(testObject, Instant.parse("2017-06-19T10:34:27.915Z"));
        create(testObject, Instant.parse("2017-06-19T10:52:59.711Z"));
        lastDelete = Instant.parse("2017-06-19T10:52:59.718Z");
        delete(testObject, lastDelete);
        startNewTransaction();
    }

    @Test
    public void testGetHighestStoredID() {
        assertEquals(testObject,
            MCRMetadataHistoryManager.getHighestStoredID(testObject.getProjectId(), testObject.getTypeId()).get());
    }

    @Test
    public void testGetHistoryStart() {
        assertEquals(HISTORY_START, MCRMetadataHistoryManager.getHistoryStart().get());
    }

    @Test
    public void testGetDeletedItems() {
        Map<MCRObjectID, Instant> deletedItems = MCRMetadataHistoryManager.getDeletedItems(Instant.ofEpochMilli(0),
            Optional.empty());
        assertEquals("Expected a single deletion event.", 1, deletedItems.size());
    }

    @Test
    public void testGetLastDeletedDate() {
        assertEquals(lastDelete, MCRMetadataHistoryManager.getLastDeletedDate(testObject).get());
    }

    private void create(MCRObjectID id, Instant time) {
        MCRMetaHistoryItem created = MCRMetaHistoryItem.createdNow(id);
        created.setTime(time);
        store(created);
    }

    private void delete(MCRObjectID id, Instant time) {
        MCRMetaHistoryItem deleted = MCRMetaHistoryItem.deletedNow(id);
        deleted.setTime(time);
        store(deleted);
    }

    private void store(MCRMetaHistoryItem item) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.persist(item);
    }

}
