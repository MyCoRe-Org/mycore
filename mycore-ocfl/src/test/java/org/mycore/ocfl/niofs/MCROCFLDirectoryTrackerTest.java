package org.mycore.ocfl.niofs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRPermutationExtension.class, MCROCFLSetupExtension.class })
public class MCROCFLDirectoryTrackerTest {

    private MCROCFLDirectoryTracker directoryTracker;

    protected MCROCFLRepository repository;

    @PermutedParam
    private boolean remote;

    @PermutedParam
    private boolean purge;

    @BeforeEach
    public void setUp() throws Exception {
        // directoryTracker
        Map<MCRVersionedPath, Boolean> paths = new HashMap<>();
        paths.put(MCRVersionedPath.getPath("der_1", "v1", "path1"), false);
        paths.put(MCRVersionedPath.getPath("der_1", "v1", "path2"), false);
        paths.put(MCRVersionedPath.getPath("der_1", "v1", "path3"), true);
        directoryTracker = new MCROCFLDirectoryTracker(paths);
    }

    @TestTemplate
    public void update() {
        MCRVersionedPath path4 = MCRVersionedPath.getPath("der_1", "v1", "path4");
        directoryTracker.update(path4, true);
        assertChanges(1);
        assertChange(path4, MCROCFLDirectoryTracker.ChangeType.ADD_KEEP);
    }

    @TestTemplate
    public void remove() {
        MCRVersionedPath path1 = MCRVersionedPath.getPath("der_1", "v1", "path1");
        directoryTracker.remove(path1);
        assertChanges(1);
        assertChange(path1, MCROCFLDirectoryTracker.ChangeType.REMOVE_KEEP);
    }

    private void assertChanges(int numberOfChanges) {
        Assertions.assertEquals(numberOfChanges, directoryTracker.changes().size());
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
