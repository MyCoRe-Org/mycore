package org.mycore.ocfl.niofs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
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
    }

    @Before
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

        // TODO this is not supported yet. We cannot recover to a rename.
        fileTracker.write("new_path1", "1");
        // assertChanges(1);
        // assertChangeRename("path1", "new_path1");
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
