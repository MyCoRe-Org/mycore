package org.mycore.ocfl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTransactionManager;
import org.mycore.ocfl.MCROCFLTestCaseHelper;
import org.mycore.ocfl.niofs.MCROCFLFileSystemTransaction;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.test.MyCoReTest;

import io.ocfl.api.exception.NotFoundException;
import io.ocfl.api.model.FileChange;
import io.ocfl.api.model.FileChangeHistory;
import io.ocfl.api.model.FileChangeType;
import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.VersionInfo;

@MyCoReTest
@ExtendWith({ MCRPermutationExtension.class, MCROCFLSetupExtension.class })
public class MCROCFLRepositoryTest {

    protected MCROCFLRepository repository;

    @PermutedParam
    private boolean remote;

    @PermutedParam
    private boolean purge;

    @TestTemplate
    public void directoryChangeHistory() throws URISyntaxException, IOException {
        assertThrows(NotFoundException.class, () -> {
            repository.directoryChangeHistory(MCROCFLTestCaseHelper.DERIVATE_2_OBJECT_ID, "/");
        });

        MCRTransactionManager.beginTransactions(MCROCFLFileSystemTransaction.class);
        MCROCFLTestCaseHelper.loadDerivate(MCROCFLTestCaseHelper.DERIVATE_2);
        MCRTransactionManager.commitTransactions(MCROCFLFileSystemTransaction.class);

        FileChangeHistory changeHistory =
            repository.directoryChangeHistory(MCROCFLTestCaseHelper.DERIVATE_2_OBJECT_ID, "/");
        check(changeHistory, FileChangeType.UPDATE, 1);

        // adding a file should not change version history of directory
        write("file1");
        changeHistory = repository.directoryChangeHistory(MCROCFLTestCaseHelper.DERIVATE_2_OBJECT_ID, "/");
        check(changeHistory, FileChangeType.UPDATE, 1);

        // add subfolder: a/file1
        write("a/file1");
        changeHistory = repository.directoryChangeHistory(MCROCFLTestCaseHelper.DERIVATE_2_OBJECT_ID, "/a");
        check(changeHistory, FileChangeType.UPDATE, 1);

        // add file: a/file2
        write("a/file2");
        changeHistory = repository.directoryChangeHistory(MCROCFLTestCaseHelper.DERIVATE_2_OBJECT_ID, "/a");
        check(changeHistory, FileChangeType.UPDATE, 1);

        // rm a/file2
        remove("a/file2");
        changeHistory = repository.directoryChangeHistory(MCROCFLTestCaseHelper.DERIVATE_2_OBJECT_ID, "/a");
        check(changeHistory, FileChangeType.UPDATE, 1);

        // rm a/file1
        remove("a/file1");
        changeHistory = repository.directoryChangeHistory(MCROCFLTestCaseHelper.DERIVATE_2_OBJECT_ID, "/a");
        check(changeHistory, FileChangeType.REMOVE, 2);

        // add file: a/file3
        write("a/file3");
        changeHistory = repository.directoryChangeHistory(MCROCFLTestCaseHelper.DERIVATE_2_OBJECT_ID, "/a");
        check(changeHistory, FileChangeType.UPDATE, 3);
    }

    private void write(String path) {
        repository.updateObject(ObjectVersionId.head(MCROCFLTestCaseHelper.DERIVATE_2_OBJECT_ID), new VersionInfo(),
            updater -> {
                updater.writeFile(new ByteArrayInputStream(new byte[] { 1, 3, 3, 7 }), path);
            });
    }

    private void remove(String path) {
        repository.updateObject(ObjectVersionId.head(MCROCFLTestCaseHelper.DERIVATE_2_OBJECT_ID), new VersionInfo(),
            updater -> {
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
