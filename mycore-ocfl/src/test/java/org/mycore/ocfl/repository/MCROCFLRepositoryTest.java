package org.mycore.ocfl.repository;

import io.ocfl.api.exception.NotFoundException;
import io.ocfl.api.model.FileChange;
import io.ocfl.api.model.FileChangeHistory;
import io.ocfl.api.model.FileChangeType;
import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.VersionInfo;
import org.junit.Test;
import org.mycore.ocfl.niofs.MCROCFLTestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class MCROCFLRepositoryTest extends MCROCFLTestCase {

    public MCROCFLRepositoryTest(boolean remote) {
        super(remote);
    }

    @Test
    public void directoryChangeHistory() throws URISyntaxException, IOException {
        assertThrows(NotFoundException.class, () -> {
            repository.directoryChangeHistory(DERIVATE_2, "/");
        });

        loadObject(DERIVATE_2);
        FileChangeHistory changeHistory = repository.directoryChangeHistory(DERIVATE_2, "/");
        check(changeHistory, FileChangeType.UPDATE, 1);

        // adding a file should not change version history of directory
        write("file1");
        changeHistory = repository.directoryChangeHistory(DERIVATE_2, "/");
        check(changeHistory, FileChangeType.UPDATE, 1);

        // add subfolder: a/file1
        write("a/file1");
        changeHistory = repository.directoryChangeHistory(DERIVATE_2, "/a");
        check(changeHistory, FileChangeType.UPDATE, 1);

        // add file: a/file2
        write("a/file2");
        changeHistory = repository.directoryChangeHistory(DERIVATE_2, "/a");
        check(changeHistory, FileChangeType.UPDATE, 1);

        // rm a/file2
        remove("a/file2");
        changeHistory = repository.directoryChangeHistory(DERIVATE_2, "/a");
        check(changeHistory, FileChangeType.UPDATE, 1);

        // rm a/file1
        remove("a/file1");
        changeHistory = repository.directoryChangeHistory(DERIVATE_2, "/a");
        check(changeHistory, FileChangeType.REMOVE, 2);

        // add file: a/file3
        write("a/file3");
        changeHistory = repository.directoryChangeHistory(DERIVATE_2, "/a");
        check(changeHistory, FileChangeType.UPDATE, 3);
    }

    private void write(String path) {
        repository.updateObject(ObjectVersionId.head(DERIVATE_2), new VersionInfo(), updater -> {
            updater.writeFile(new ByteArrayInputStream(new byte[] { 1, 3, 3, 7 }), path);
        });
    }

    private void remove(String path) {
        repository.updateObject(ObjectVersionId.head(DERIVATE_2), new VersionInfo(), updater -> {
            updater.removeFile(path);
        });
    }

    private void check(FileChangeHistory changeHistory, FileChangeType mostRecentChangeType, long numChanges) {
        assertEquals(numChanges, changeHistory.getFileChanges().size());
        FileChange mostRecent = changeHistory.getMostRecent();
        assertNotNull(mostRecent.getObjectVersionId());
        assertNotNull(mostRecent.getPath());
        assertNotNull(mostRecent.getStorageRelativePath());
        assertNotNull(mostRecent.getTimestamp());
        assertNotNull(mostRecent.getVersionInfo());
        assertEquals(mostRecentChangeType, mostRecent.getChangeType());
    }

}
