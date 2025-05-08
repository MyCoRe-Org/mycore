package org.mycore.ocfl.niofs.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTransactionManager;
import org.mycore.ocfl.niofs.MCROCFLInactiveTransactionException;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRPermutationExtension.class })
public class MCROCFLHybridStorageTest extends MCROCFLStorageTestCase {

    private MCROCFLHybridStorage storage;

    @PermutedParam
    private boolean remote;

    @PermutedParam
    private boolean purge;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        FileCountEvictionStrategy evictionStrategy = new FileCountEvictionStrategy(2);
        storage = new MCROCFLHybridStorage(rootPath, evictionStrategy);
    }

    @Override
    public MCROCFLHybridStorage getStorage() {
        return storage;
    }

    @TestTemplate
    public void exists() throws IOException {
        Assertions.assertFalse(storage.exists(path1), "'path1' should not exist before any operation");
        write(path1);
        Assertions.assertTrue(storage.exists(path1), "'path1' should exist after write operation");
        MCRTransactionManager.beginTransactions();
        Assertions.assertTrue(storage.exists(path1), "'path1' should exist after transaction start");
        MCRTransactionManager.commitTransactions();
    }

    @TestTemplate
    public void toPhysicalPath() throws IOException {
        String rollingDirectory = "/" + MCROCFLHybridStorage.ROLLING_DIRECTORY + "/";
        String transactionDirectory = "/" + MCROCFLHybridStorage.TRANSACTION_DIRECTORY + "/";
        Assertions.assertTrue(storage.toPhysicalPath(path1).toString().contains(rollingDirectory),
            "should access rolling store");
        MCRTransactionManager.beginTransactions();
        Assertions.assertTrue(storage.toPhysicalPath(path1).toString().contains(rollingDirectory),
            "should access rolling store because the transaction store is empty");
        write(path1);
        Assertions.assertTrue(storage.toPhysicalPath(path1).toString().contains(transactionDirectory),
            "should access transactional store because a file was written there");
        MCRTransactionManager.commitTransactions();
        Assertions.assertTrue(storage.toPhysicalPath(path1).toString().contains(rollingDirectory),
            "should access rolling store because there is no active transaction");
        MCRTransactionManager.beginTransactions();
        Assertions.assertTrue(storage.toPhysicalPath(path1).toString().contains(rollingDirectory),
            "should access rolling store because the transaction store is empty");
        MCRTransactionManager.commitTransactions();
    }

    @TestTemplate
    public void copyInputStream() throws IOException, MCROCFLInactiveTransactionException {
        storage.copy(new ByteArrayInputStream(new byte[] { 1 }), path1);
        Assertions.assertTrue(storage.exists(path1), "'path1' should exist");

        MCRTransactionManager.beginTransactions();
        Assertions.assertTrue(storage.exists(path1), "'path1' should exist");
        storage.copy(new ByteArrayInputStream(new byte[] { 2 }), path1);
        Assertions.assertTrue(storage.exists(path1), "'path1' should exist");
        Assertions.assertArrayEquals(new byte[] { 2 }, read(path1),
            "'path1' should have the content of the transactional store");
        MCRTransactionManager.commitTransactions();

        Assertions.assertArrayEquals(new byte[] { 1 }, read(path1),
            "'path1' should have the content of the rollover store");
    }

    @TestTemplate
    public void copyWithSourceAndTarget() throws IOException, MCROCFLInactiveTransactionException {
        byte[] path1Data = { 1 };
        byte[] path3Data = { 2 };

        write(path1, path1Data);
        storage.copy(path1, path2);
        Assertions.assertTrue(storage.exists(path1), "'path1' should exist");
        Assertions.assertTrue(storage.exists(path2), "'path2' should exist");
        Assertions.assertArrayEquals(path1Data, read(path1), "'path1' should have the content of the rollover store");
        Assertions.assertArrayEquals(path1Data, read(path2), "'path2' should have the content of the rollover store");

        MCRTransactionManager.beginTransactions();
        Assertions.assertArrayEquals(path1Data, read(path1), "'path1' should have the content of the rollover store");
        Assertions.assertArrayEquals(path1Data, read(path2), "'path2' should have the content of the rollover store");
        storage.copy(path1, path3);
        Assertions.assertArrayEquals(path1Data, read(path3), "'path3' should have the content of the rollover store");
        write(path3, path3Data);
        storage.copy(path3, path2);
        Assertions.assertArrayEquals(path3Data, read(path2),
            "'path2' should have the content of the transactional store");
        MCRTransactionManager.commitTransactions();
    }

    @TestTemplate
    public void move() throws IOException {
        storage.copy(new ByteArrayInputStream(new byte[] { 1 }), path1);
        storage.move(path1, path2);
        Assertions.assertTrue(storage.exists(path2), "'path2' should exist after move");

        MCRTransactionManager.beginTransactions();
        storage.move(path2, path3);
        Assertions.assertTrue(storage.exists(path3), "'path3' should exist after move");
        MCRTransactionManager.commitTransactions();
    }

    @TestTemplate
    public void deleteIfExists() throws IOException {
        write(path1);
        Assertions.assertTrue(storage.exists(path1), "'path1' should exist before deletion");
        storage.deleteIfExists(path1);
        Assertions.assertFalse(storage.exists(path1), "'path1' should not exist after deletion");

        MCRTransactionManager.beginTransactions();
        write(path1);
        Assertions.assertTrue(storage.exists(path1), "'path1' should exist before deletion");
        storage.deleteIfExists(path1);
        Assertions.assertFalse(storage.exists(path1), "'path1' should not exist after deletion");
        MCRTransactionManager.commitTransactions();
    }

}
