package org.mycore.ocfl.niofs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.Test;
import org.mycore.common.MCRTransactionManager;
import org.mycore.common.digest.MCRDigest;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRPath;

public class MCROCFLBasicFileAttributesTest extends MCROCFLNioTestCase {

    public MCROCFLBasicFileAttributesTest(boolean remote, boolean purge) {
        super(remote, purge);
    }

    @Test
    public void testDirectoryAttributes() throws IOException, InterruptedException {
        MCRPath root = MCRPath.getPath(DERIVATE_1, "/");
        MCRFileAttributes<?> rootAttributes = Files.readAttributes(root, MCRFileAttributes.class);
        assertTrue("root should be a directory", rootAttributes.isDirectory());
        assertNull("directory digest should be null", rootAttributes.digest());

        Thread.sleep(1);
        MCRTransactionManager.requireTransactions(MCROCFLFileSystemTransaction.class);
        Files.write(MCRPath.getPath(DERIVATE_1, "file1"), new byte[] { 1, 3, 3, 7 });
        MCRFileAttributes<?> rootAttributes2 = Files.readAttributes(root, MCRFileAttributes.class);
        assertEquals("should have the same creation time after writing a file", rootAttributes.creationTime(),
            rootAttributes2.creationTime());
        assertTrue("modified time should be later",
            rootAttributes2.lastModifiedTime().toInstant().isAfter(rootAttributes.lastModifiedTime().toInstant()));
        MCRTransactionManager.commitTransactions(MCROCFLFileSystemTransaction.class);
    }

    @Test
    public void testFileAttributes() throws IOException {
        Path whitePng = MCRPath.getPath(DERIVATE_1, "white.png");
        MCRFileAttributes<?> sourceAttributes = Files.readAttributes(whitePng, MCRFileAttributes.class);
        assertFalse("'white.png' should not be a directory", sourceAttributes.isDirectory());
        assertTrue("'white.png' should not be a regular file", sourceAttributes.isRegularFile());
        assertNotNull("'white.png' should have a digest", sourceAttributes.digest());

        MCRPath newFile = MCRPath.getPath(DERIVATE_1, "file1");
        MCRTransactionManager.beginTransactions();
        Files.write(newFile, new byte[] { 1, 3, 3, 7 });
        MCRFileAttributes<?> newFileAttributes = Files.readAttributes(newFile, MCRFileAttributes.class);
        assertFalse("'file1' should not be a directory", newFileAttributes.isDirectory());
        assertTrue("'file1' should not be a regular file", newFileAttributes.isRegularFile());
        MCRDigest newFileDigest = newFileAttributes.digest();
        assertNotNull("'file1' should have a digest", newFileDigest);
        MCRTransactionManager.commitTransactions();

        MCRFileAttributes<?> newFileAttributesAfterCommit = Files.readAttributes(newFile, MCRFileAttributes.class);
        MCRDigest afterCommitDigest = newFileAttributesAfterCommit.digest();
        assertNotNull("'file1' should have a digest", afterCommitDigest);
        assertEquals(newFileDigest, afterCommitDigest);
    }

    @Test
    public void moveAndCheckAttributes() throws IOException {
        Path source = MCRPath.getPath(DERIVATE_1, "white.png");
        Path target = MCRPath.getPath(DERIVATE_2, "moved.png");
        MCRTransactionManager.beginTransactions();
        Files.move(source, target);
        BasicFileAttributes targetFileAttributes = Files.readAttributes(target, BasicFileAttributes.class);
        assertNotNull("'moved.png' should have basic file attributes", targetFileAttributes);
        assertTrue("'moved.png' should be a regular file", targetFileAttributes.isRegularFile());
        assertThrows(NoSuchFileException.class, () -> Files.readAttributes(source, BasicFileAttributes.class));
        MCRTransactionManager.commitTransactions();
    }

}
