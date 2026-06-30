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

package org.mycore.dedup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.dedup.backend.MCRDeDupKey;
import org.mycore.dedup.backend.MCRDeDupNoDuplicate;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true")
})
public class MCRDeDupKeyManagerTest {

    private final MCRDeDupKeyManager manager = new MCRDeDupKeyManager();

    private static MCRObjectID id(int number) {
        return MCRObjectID.getInstance(MCRObjectID.formatID("mcr", "test", number));
    }

    private static MCRDeDupCriterion criterion(String type, String value) {
        return new MCRDeDupCriterion(type, value);
    }

    private static void assertPair(MCRPossibleDuplicate pair, MCRObjectID a, MCRObjectID b) {
        Set<String> ids = Set.of(pair.objectId1(), pair.objectId2());
        assertEquals(Set.of(a.toString(), b.toString()), ids, "unexpected duplicate pair " + pair);
    }

    @Test
    public void twoObjectsSharingACriterionAreDuplicates() {
        manager.storeKeys(id(1), Set.of(criterion("identifier", "doi:1")));
        manager.storeKeys(id(2), Set.of(criterion("identifier", "doi:1")));

        List<MCRPossibleDuplicate> duplicatesOf1 = manager.findDuplicates(id(1));
        assertEquals(1, duplicatesOf1.size());
        assertPair(duplicatesOf1.get(0), id(1), id(2));
        assertEquals(criterion("identifier", "doi:1"), duplicatesOf1.get(0).criterion());

        List<MCRPossibleDuplicate> all = manager.findAllDuplicates();
        assertEquals(1, all.size());
        assertPair(all.get(0), id(1), id(2));
    }

    @Test
    public void objectsWithoutSharedCriteriaAreNoDuplicates() {
        manager.storeKeys(id(1), Set.of(criterion("identifier", "doi:1")));
        manager.storeKeys(id(2), Set.of(criterion("identifier", "doi:2")));

        assertTrue(manager.findDuplicates(id(1)).isEmpty());
        assertTrue(manager.findAllDuplicates().isEmpty());
    }

    @Test
    public void objectWithoutKeysHasNoDuplicates() {
        assertTrue(manager.findDuplicates(id(1)).isEmpty());
        assertTrue(manager.findAllDuplicates().isEmpty());
    }

    @Test
    public void twoSharedCriteriaProduceTwoEntriesForTheSamePair() {
        Set<MCRDeDupCriterion> criteria = Set.of(
            criterion("identifier", "doi:1"),
            criterion("title-author", "meier: a title"));
        manager.storeKeys(id(1), criteria);
        manager.storeKeys(id(2), criteria);

        List<MCRPossibleDuplicate> all = manager.findAllDuplicates();
        assertEquals(2, all.size(), "expected one entry per matching criterion");
        all.forEach(pair -> assertPair(pair, id(1), id(2)));
        assertEquals(Set.of(criterion("identifier", "doi:1"), criterion("title-author", "meier: a title")),
            Set.of(all.get(0).criterion(), all.get(1).criterion()));

        assertEquals(2, manager.findDuplicates(id(1)).size());
    }

    @Test
    public void noDuplicateMarkingSuppressesPairAcrossAllCriteria() {
        Set<MCRDeDupCriterion> criteria = Set.of(
            criterion("identifier", "doi:1"),
            criterion("title-author", "meier: a title"));
        manager.storeKeys(id(1), criteria);
        manager.storeKeys(id(2), criteria);

        manager.addNoDuplicate(id(1), id(2), "junit");

        assertTrue(manager.findDuplicates(id(1)).isEmpty(),
            "both matching criteria must be suppressed by the no-duplicate marking");
        assertTrue(manager.findAllDuplicates().isEmpty());
    }

    @Test
    public void noDuplicateMarkingIsOrderIndependent() {
        manager.storeKeys(id(1), Set.of(criterion("identifier", "doi:1")));
        manager.storeKeys(id(2), Set.of(criterion("identifier", "doi:1")));

        manager.addNoDuplicate(id(2), id(1), "junit");

        assertTrue(manager.findDuplicates(id(1)).isEmpty());
        assertTrue(manager.findDuplicates(id(2)).isEmpty());
        assertTrue(manager.findAllDuplicates().isEmpty());
    }

    @Test
    public void storeKeysReplacesPreviousKeys() {
        manager.storeKeys(id(1), Set.of(criterion("identifier", "doi:1")));
        manager.storeKeys(id(2), Set.of(criterion("identifier", "doi:1")));
        assertEquals(1, manager.findAllDuplicates().size());

        manager.storeKeys(id(1), Set.of(criterion("identifier", "doi:2")));

        assertTrue(manager.findAllDuplicates().isEmpty(), "old key of id(1) must have been replaced");
    }

    @Test
    public void removeKeysRemovesObjectFromDuplicates() {
        manager.storeKeys(id(1), Set.of(criterion("identifier", "doi:1")));
        manager.storeKeys(id(2), Set.of(criterion("identifier", "doi:1")));
        manager.storeKeys(id(3), Set.of(criterion("identifier", "doi:1")));
        assertEquals(3, manager.findAllDuplicates().size());

        manager.removeKeys(id(2));

        List<MCRPossibleDuplicate> all = manager.findAllDuplicates();
        assertEquals(1, all.size());
        assertPair(all.get(0), id(1), id(3));
    }

    @Test
    public void threeMembersProduceAllPairsMinusMarkedNoDuplicate() {
        Set<MCRDeDupCriterion> criteria = Set.of(criterion("identifier", "doi:1"));
        manager.storeKeys(id(1), criteria);
        manager.storeKeys(id(2), criteria);
        manager.storeKeys(id(3), criteria);

        assertEquals(3, manager.findAllDuplicates().size());

        manager.addNoDuplicate(id(1), id(2), "junit");

        List<MCRPossibleDuplicate> all = manager.findAllDuplicates();
        assertEquals(2, all.size());
        all.forEach(pair -> assertTrue(
            !(pair.objectId1().equals(id(1).toString()) && pair.objectId2().equals(id(2).toString())),
            "pair id(1)/id(2) must be suppressed"));
    }

    @Test
    public void longValuesAreTruncatedAndStillMatch() {
        manager.storeKeys(id(1), Set.of(criterion("identifier", "x".repeat(300))));

        List<MCRDeDupKey> keys = MCREntityManagerProvider.getCurrentEntityManager()
            .createQuery("SELECT k FROM MCRDeDupKey k WHERE k.objectId = :id", MCRDeDupKey.class)
            .setParameter("id", id(1).toString())
            .getResultList();
        assertEquals(1, keys.size());
        assertEquals(MCRDeDupKey.MAX_VALUE_LENGTH, keys.get(0).getValue().length());

        String prefix = "a".repeat(MCRDeDupKey.MAX_VALUE_LENGTH);
        manager.storeKeys(id(2), Set.of(criterion("identifier", prefix + "TAIL1")));
        manager.storeKeys(id(3), Set.of(criterion("identifier", prefix + "TAIL2")));

        List<MCRPossibleDuplicate> duplicatesOf2 = manager.findDuplicates(id(2));
        assertEquals(1, duplicatesOf2.size(), "values must match after truncation to the column length");
        assertPair(duplicatesOf2.get(0), id(2), id(3));
    }

    @Test
    public void findsExistingObjectsSharingGivenCriteria() {
        MCRDeDupCriterion shared = criterion("identifier", "doi:1");
        MCRDeDupCriterion secondShared = criterion("title-author", "meier: a title");
        manager.storeKeys(id(1), Set.of(shared, secondShared));
        manager.storeKeys(id(2), Set.of(shared));
        manager.storeKeys(id(3), Set.of(criterion("identifier", "doi:3")));

        Map<MCRObjectID, Set<MCRDeDupCriterion>> duplicates = manager.findDuplicates(Set.of(shared, secondShared));

        assertEquals(Set.of(id(1), id(2)), duplicates.keySet());
        assertEquals(Set.of(shared, secondShared), duplicates.get(id(1)));
        assertEquals(Set.of(shared), duplicates.get(id(2)));
    }

    @Test
    public void findsExistingObjectsWithTruncatedGivenCriteria() {
        String prefix = "a".repeat(MCRDeDupKey.MAX_VALUE_LENGTH);
        manager.storeKeys(id(1), Set.of(criterion("identifier", prefix + "stored-tail")));

        Map<MCRObjectID, Set<MCRDeDupCriterion>> duplicates = manager.findDuplicates(
            Set.of(criterion("identifier", prefix + "session-tail")));

        assertEquals(Set.of(id(1)), duplicates.keySet());
        assertEquals(Set.of(criterion("identifier", prefix)), duplicates.get(id(1)));
    }

    @Test
    public void findDuplicatesForGivenCriteriaDoesNotApplyNoDuplicateMarkings() {
        MCRDeDupCriterion shared = criterion("identifier", "doi:1");
        manager.storeKeys(id(1), Set.of(shared));
        manager.addNoDuplicate(id(1), id(2), "junit");

        Map<MCRObjectID, Set<MCRDeDupCriterion>> duplicates = manager.findDuplicates(Set.of(shared));

        assertEquals(Set.of(id(1)), duplicates.keySet());
        assertEquals(Set.of(shared), duplicates.get(id(1)));
    }

    @Test
    public void findDuplicatesForEmptyCriteriaReturnsEmptyMap() {
        manager.storeKeys(id(1), Set.of(criterion("identifier", "doi:1")));

        assertTrue(manager.findDuplicates(Set.of()).isEmpty());
    }

    @Test
    public void addListAndRemoveNoDuplicate() {
        manager.addNoDuplicate(id(2), id(1), "junit");

        List<MCRDeDupNoDuplicate> list = manager.listNoDuplicates();
        assertEquals(1, list.size());
        MCRDeDupNoDuplicate noDuplicate = list.get(0);
        assertEquals("junit", noDuplicate.getCreator());
        assertEquals(id(1).toString(), noDuplicate.getObjectId1(), "ids must be stored in normalized order");
        assertEquals(id(2).toString(), noDuplicate.getObjectId2());

        manager.removeNoDuplicate(noDuplicate.getId());
        assertTrue(manager.listNoDuplicates().isEmpty());
    }

    @Test
    public void removeNoDuplicatesRemovesAllMarkingsOfAnObject() {
        manager.addNoDuplicate(id(1), id(2), "junit");
        manager.addNoDuplicate(id(1), id(3), "junit");
        manager.addNoDuplicate(id(2), id(3), "junit");

        manager.removeNoDuplicates(id(1));

        List<MCRDeDupNoDuplicate> list = manager.listNoDuplicates();
        assertEquals(1, list.size());
        assertEquals(id(2).toString(), list.get(0).getObjectId1());
        assertEquals(id(3).toString(), list.get(0).getObjectId2());
    }
}
