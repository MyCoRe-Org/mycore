package org.mycore.ocfl.niofs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mycore.ocfl.MCROCFLTestCaseHelper.DERIVATE_1;
import static org.mycore.ocfl.MCROCFLTestCaseHelper.DERIVATE_2;
import static org.mycore.ocfl.MCROCFLTestCaseHelper.WHITE_PNG;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTransactionManager;
import org.mycore.common.digest.MCRDigest;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRPermutationExtension.class, MCROCFLSetupExtension.class })
public class MCROCFLBasicFileAttributesTest {

    protected MCROCFLRepository repository;

    @PermutedParam
    private boolean remote;

    @PermutedParam
    private boolean purge;

    @TestTemplate
    public void testDirectoryAttributes() throws IOException, InterruptedException {
        MCRPath root = MCRPath.getPath(DERIVATE_1, "/");
        MCRFileAttributes<?> rootAttributes = Files.readAttributes(root, MCRFileAttributes.class);
        assertTrue(rootAttributes.isDirectory(), "root should be a directory");
        assertNull(rootAttributes.digest(), "directory digest should be null");

        Thread.sleep(1);
        MCRTransactionManager.requireTransactions(MCROCFLFileSystemTransaction.class);
        Files.write(MCRPath.getPath(DERIVATE_1, "file1"), new byte[] { 1, 3, 3, 7 });
        MCRFileAttributes<?> rootAttributes2 = Files.readAttributes(root, MCRFileAttributes.class);
        assertEquals(rootAttributes.creationTime(), rootAttributes2.creationTime(),
            "should have the same creation time after writing a file");
        assertTrue(
            rootAttributes2.lastModifiedTime().toInstant().isAfter(rootAttributes.lastModifiedTime().toInstant()),
            "modified time should be later");
        MCRTransactionManager.commitTransactions(MCROCFLFileSystemTransaction.class);
    }

    @TestTemplate
    public void testFileAttributes() throws IOException {
        MCRFileAttributes<?> sourceAttributes = Files.readAttributes(WHITE_PNG, MCRFileAttributes.class);
        assertFalse(sourceAttributes.isDirectory(), "'white.png' should not be a directory");
        assertTrue(sourceAttributes.isRegularFile(), "'white.png' should not be a regular file");
        assertNotNull(sourceAttributes.digest(), "'white.png' should have a digest");

        MCRPath newFile = MCRPath.getPath(DERIVATE_1, "file1");
        MCRTransactionManager.beginTransactions();
        Files.write(newFile, new byte[] { 1, 3, 3, 7 });
        MCRFileAttributes<?> newFileAttributes = Files.readAttributes(newFile, MCRFileAttributes.class);
        assertFalse(newFileAttributes.isDirectory(), "'file1' should not be a directory");
        assertTrue(newFileAttributes.isRegularFile(), "'file1' should not be a regular file");
        MCRDigest newFileDigest = newFileAttributes.digest();
        assertNotNull(newFileDigest, "'file1' should have a digest");
        MCRTransactionManager.commitTransactions();

        MCRFileAttributes<?> newFileAttributesAfterCommit = Files.readAttributes(newFile, MCRFileAttributes.class);
        MCRDigest afterCommitDigest = newFileAttributesAfterCommit.digest();
        assertNotNull(afterCommitDigest, "'file1' should have a digest");
        assertEquals(newFileDigest, afterCommitDigest);
    }

    @TestTemplate
    public void moveAndCheckAttributes() throws IOException {
        Path target = MCRPath.getPath(DERIVATE_2, "moved.png");
        MCRTransactionManager.beginTransactions();
        Files.move(WHITE_PNG, target);
        BasicFileAttributes targetFileAttributes = Files.readAttributes(target, BasicFileAttributes.class);
        assertNotNull(targetFileAttributes, "'moved.png' should have basic file attributes");
        assertTrue(targetFileAttributes.isRegularFile(), "'moved.png' should be a regular file");
        assertThrows(NoSuchFileException.class,
                () -> Files.readAttributes(WHITE_PNG, BasicFileAttributes.class));
        MCRTransactionManager.commitTransactions();
    }

}
