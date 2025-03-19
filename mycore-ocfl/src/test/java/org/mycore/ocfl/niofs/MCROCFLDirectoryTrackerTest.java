package org.mycore.ocfl.niofs;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mycore.datamodel.niofs.MCRVersionedPath;

public class MCROCFLDirectoryTrackerTest extends MCROCFLNioTestCase {

    private MCROCFLDirectoryTracker directoryTracker;

    public MCROCFLDirectoryTrackerTest(boolean remote, boolean purge) {
        super(remote, purge);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Map<MCRVersionedPath, Boolean> paths = new HashMap<>();
        paths.put(MCRVersionedPath.getPath("der_1", "v1", "path1"), false);
        paths.put(MCRVersionedPath.getPath("der_1", "v1", "path2"), false);
        paths.put(MCRVersionedPath.getPath("der_1", "v1", "path3"), true);
        directoryTracker = new MCROCFLDirectoryTracker(paths);
    }

    @Test
    public void update() {
        MCRVersionedPath path4 = MCRVersionedPath.getPath("der_1", "v1", "path4");
        directoryTracker.update(path4, true);
        assertChanges(1);
        assertChange(path4, MCROCFLDirectoryTracker.ChangeType.ADD_KEEP);
    }

    @Test
    public void remove() {
        MCRVersionedPath path1 = MCRVersionedPath.getPath("der_1", "v1", "path1");
        directoryTracker.remove(path1);
        assertChanges(1);
        assertChange(path1, MCROCFLDirectoryTracker.ChangeType.REMOVE_KEEP);
    }

    private void assertChanges(int numberOfChanges) {
        assertEquals(numberOfChanges, directoryTracker.changes().size());
    }

    private void assertChange(MCRVersionedPath path, MCROCFLDirectoryTracker.ChangeType changeType) {
        List<MCROCFLDirectoryTracker.Change> changes = directoryTracker.changes();
        for (MCROCFLDirectoryTracker.Change change : changes) {
            if (change.keepFile().equals(directoryTracker.toKeepFile(path)) && changeType.equals(change.type())) {
                return;
            }
        }
        throw new AssertionError("Change of type " + changeType + " for path " + path + " not found");
    }

}
