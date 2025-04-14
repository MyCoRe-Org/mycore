package org.mycore.ocfl.niofs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.ocfl.niofs.MCROCFLFileTracker.Change;
import org.mycore.ocfl.niofs.MCROCFLFileTracker.ChangeType;

public class MCROCFLFileTrackerTest {

    private MCROCFLFileTracker<String, String> fileTracker;

    private static final HashMap<String, String> DATA;

    static {
        DATA = new LinkedHashMap<>();
        DATA.put("path1", "1");
        DATA.put("path2", "2");
        DATA.put("path3", "3");
        DATA.put("path4", "4");
        DATA.put("path5", "5");
    }

    @BeforeEach
    public void setUp() {
        Map<String, String> original = new HashMap<>();
        original.put("path1", DATA.get("path1"));
        original.put("path2", DATA.get("path2"));
        original.put("path3", DATA.get("path3"));
        fileTracker = new MCROCFLFileTracker<>(original, DATA::get);
    }

    @Test
    public void testWrite() {
        fileTracker.write("path4");
        assertChanges(1);
        assertChange("path4", ChangeType.ADDED_OR_MODIFIED);

        fileTracker.write("path4");
        assertChanges(1);
        assertChange("path4", ChangeType.ADDED_OR_MODIFIED);
    }

    @Test
    public void testDelete() {
        fileTracker.delete("path2");
        assertChanges(1);
        assertChange("path2", ChangeType.DELETED);
    }

    @Test
    public void testRename() {
        fileTracker.rename("path3", "path33");
        assertChanges(1);
        assertChangeRename("path3", "path33");
    }

    @Test
    public void testChanges() {
        assertChanges(0);
    }

    @Test
    public void testNotFound() {
        fileTracker.rename("path4", "path5");
        assertChanges(0);
        fileTracker.delete("path4");
        assertChanges(0);
    }

    @Test
    public void testRenameDelete() {
        fileTracker.rename("path1", "path2");
        fileTracker.delete("path1");
        assertChanges(1);
        assertChangeRename("path1", "path2");

        fileTracker.delete("path2");
        assertChanges(2);
        assertChange("path1", ChangeType.DELETED);
        assertChange("path2", ChangeType.DELETED);
    }

    @Test
    public void testRenameSameTarget() {
        fileTracker.rename("path1", "path2");
        fileTracker.rename("path3", "path2");
        assertChanges(2);
        assertChange("path1", ChangeType.DELETED);
        assertChangeRename("path3", "path2");
    }

    @Test
    public void testRenameSame() {
        fileTracker.rename("path1", "new_path1");
        fileTracker.rename("new_path1", "new_path2");
        fileTracker.rename("new_path2", "path1");
        assertChanges(0);
    }

    @Test
    public void testRenameMultipleTimes() {
        fileTracker.rename("path1", "new_path1");
        fileTracker.rename("new_path1", "new_path2");
        fileTracker.rename("new_path2", "new_path3");
        assertChanges(1);
        assertChangeRename("path1", "new_path3");
    }

    @Test
    public void testRenameWrite() {
        // writing same content to renamed file
        fileTracker.rename("path1", "new_path1");
        fileTracker.write("new_path1", DATA.get("path1"));
        assertChanges(1);
        assertChangeRename("path1", "new_path1");

        // writing different content to renamed file
        fileTracker.write("new_path1", "other_digest");
        assertChanges(2);
        assertChange("path1", ChangeType.DELETED);
        assertChange("new_path1", ChangeType.ADDED_OR_MODIFIED);

        fileTracker.write("new_path1", DATA.get("path1"));
        assertChanges(1);
        assertChangeRename("path1", "new_path1");
    }

    @Test
    public void testCombine() {
        fileTracker.write("path4", "4");
        fileTracker.write("path5", "5");
        assertChanges(2);

        fileTracker.rename("path1", "new_path1");
        fileTracker.rename("path2", "new_path2");
        assertChanges(4);

        fileTracker.delete("path3");
        assertChanges(5);

        assertChange("path4", ChangeType.ADDED_OR_MODIFIED);
        assertChange("path5", ChangeType.ADDED_OR_MODIFIED);
        assertChangeRename("path1", "new_path1");
        assertChangeRename("path2", "new_path2");
        assertChange("path3", ChangeType.DELETED);
    }

    @Test
    public void testIsModified() {
        fileTracker.write("path1", "other_digest");
        assertTrue(fileTracker.isAddedOrModified("path1"));
        assertChanges(1);
        assertChange("path1", ChangeType.ADDED_OR_MODIFIED);
        fileTracker.write("path1", DATA.get("path1"));
        assertFalse(fileTracker.isAddedOrModified("path1"));
        assertChanges(0);
    }

    @Test
    public void testIsAdded() {
        // test existing
        assertFalse(fileTracker.isAdded("path1"));
        // test new
        fileTracker.write("path5");
        assertTrue(fileTracker.isAdded("path5"));
        // test delete
        fileTracker.delete("path2");
        assertFalse(fileTracker.isAdded("path2"));
        // test add deleted again
        fileTracker.write("path2");
        assertFalse(fileTracker.isAdded("path2"));
    }

    @Test
    public void testIsAddedOrModified() {
        // Test for added path
        fileTracker.write("path5");
        assertTrue(fileTracker.isAddedOrModified("path5"));
        // Test for modified path (existing path with different digest)
        fileTracker.write("path1", "new_digest");
        assertTrue(fileTracker.isAddedOrModified("path1"));
        // Test for unchanged path (existing path with same digest)
        assertFalse(fileTracker.isAddedOrModified("path2"));
        // Test for deleted path (should return false as it's not added or modified)
        fileTracker.delete("path2");
        assertFalse(fileTracker.isAddedOrModified("path2"));
        // Test add deleted again
        fileTracker.write("path2", "new_digest");
        assertFalse(fileTracker.isAddedOrModified(DATA.get("path2")));
    }

    @Test
    public void testDigest() {
        // existing path
        assertEquals(DATA.get("path1"), fileTracker.getDigest("path1"));
        fileTracker.write("path1", "other_digest");
        assertEquals("other_digest", fileTracker.getDigest("path1"));
        // new path
        fileTracker.write("path4");
        assertEquals(DATA.get("path4"), fileTracker.getDigest("path4"));
        // renamed path
        fileTracker.rename("path4", "path5");
        assertNull(fileTracker.getDigest("path4"));
        assertEquals(DATA.get("path4"), fileTracker.getDigest("path5"));
    }

    @Test
    public void testDeepClone() {
        // Modify the tracker
        fileTracker.rename("path1", "new_path1");
        fileTracker.delete("path2");
        fileTracker.write("path3", "modified");

        // Create a deep clone
        MCROCFLFileTracker<String, String> clone = fileTracker.deepClone();

        // Verify that the clone initially has the same changes
        assertEquals(fileTracker.paths(), clone.paths());
        assertEquals(fileTracker.changes().size(), clone.changes().size());

        // Record the clone's state (e.g., paths and changes)
        List<String> clonePathsBefore = clone.paths();
        List<MCROCFLFileTracker.Change<String>> cloneChangesBefore = clone.changes();

        // Modify the original further.
        fileTracker.write("new_path1", "other_digest");

        // Now assert that the clone's state remains unchanged.
        assertEquals(clonePathsBefore, clone.paths());
        assertEquals(cloneChangesBefore, clone.changes());

        // The original now differs from the clone.
        assertNotEquals(fileTracker.getDigest("new_path1"), clone.getDigest("new_path1"));
    }

    @Test
    public void testLazyDigestCalculation() {
        // Write a new file with no digest provided.
        fileTracker.write("path5");
        // At this point, the node should be created with a null digest.
        // Calling getDigest should trigger the digestCalculator (DATA::get).
        String digest = fileTracker.getDigest("path5");
        assertNotNull(digest);
        assertEquals(DATA.get("path5"), digest);
    }

    @Test
    public void testNonExistentRenameAndDelete() {
        // Calling rename on a non-existent file should do nothing.
        fileTracker.rename("non_existent", "some_target");
        assertEquals(0, fileTracker.changes().size());

        // Calling delete on a non-existent file should do nothing.
        fileTracker.delete("non_existent");
        assertEquals(0, fileTracker.changes().size());
    }

    @Test
    public void testConflictRename() {
        // First, add a new file with path "conflict".
        fileTracker.write("conflict", "conflict_digest");
        // Now, rename an existing file ("path1") to "conflict".
        // The tracker should remove the pre-existing "conflict" node.
        fileTracker.rename("path1", "conflict");

        // The change should indicate a rename from "path1" to "conflict".
        boolean foundRename = fileTracker.changes().stream().anyMatch(
            c -> c.type().equals(ChangeType.RENAMED)
                && c.source().equals("path1")
                && c.target().equals("conflict"));
        assertTrue(foundRename, "Expected a rename change for 'path1' -> 'conflict'");
    }

    @Test
    public void testReAddAfterDeletion() {
        // Delete an original file.
        fileTracker.delete("path2");
        assertTrue(fileTracker.changes().stream().anyMatch(
            c -> c.type().equals(ChangeType.DELETED) && c.source().equals("path2")));
        // Re-add the same file with its original digest.
        fileTracker.write("path2", DATA.get("path2"));
        // Expect no changes because the file is restored to its original state.
        assertEquals(0, fileTracker.changes().size());
    }

    private void assertChanges(int numberOfChanges) {
        assertEquals(numberOfChanges, fileTracker.changes().size());
    }

    private void assertChange(String path, ChangeType changeType) {
        List<Change<String>> changes = fileTracker.changes();
        for (Change<String> change : changes) {
            if (change.source().equals(path) && changeType.equals(change.type())) {
                return;
            }
        }
        throw new AssertionError("Change of type " + changeType + " for path " + path + " not found");
    }

    private void assertChangeRename(String source, String target) {
        List<Change<String>> changes = fileTracker.changes();
        for (Change<String> change : changes) {
            if (change.source().equals(source) && change.target().equals(target)
                && ChangeType.RENAMED.equals(change.type())) {
                return;
            }
        }
        throw new AssertionError("Change of type RENAME for path " + source + " to " + target + " not found");
    }

}
