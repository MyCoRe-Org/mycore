package org.mycore.ocfl.niofs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.Test;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.common.digest.MCRDigest;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRPath;

import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.VersionInfo;

public class MCROCFLBasicFileAttributesTest extends MCROCFLTestCase {

    @Test
    public void testDirectoryAttributes() throws IOException, InterruptedException {
        // add some versions
        repository.updateObject(ObjectVersionId.head(DERIVATE_1), new VersionInfo(), updater -> {
            updater.writeFile(new ByteArrayInputStream(new byte[] { 1, 3, 3, 7 }), "file1");
        });
        repository.updateObject(ObjectVersionId.head(DERIVATE_1), new VersionInfo(), updater -> {
            updater.writeFile(new ByteArrayInputStream(new byte[] { 1, 3, 3, 7 }), "file2");
        });

        MCRPath root = MCRPath.getPath(DERIVATE_1, "/");
        MCRFileAttributes<?> rootAttributes = Files.readAttributes(root, MCRFileAttributes.class);
        assertTrue("root should be a directory", rootAttributes.isDirectory());
        assertNull("directory digest should be null", rootAttributes.digest());

        Thread.sleep(1);
        MCRTransactionHelper.requireTransaction(MCROCFLFileSystemTransaction.class);
        Files.write(MCRPath.getPath(DERIVATE_1, "file3"), new byte[] { 1, 3, 3, 7 });
        MCRFileAttributes<?> rootAttributes2 = Files.readAttributes(root, MCRFileAttributes.class);
        assertEquals("should have the same creation time after writing a file", rootAttributes.creationTime(),
            rootAttributes2.creationTime());
        assertTrue("modified time should be later",
            rootAttributes2.lastModifiedTime().toInstant().isAfter(rootAttributes.lastModifiedTime().toInstant()));
        MCRTransactionHelper.commitTransaction();
    }

    @Test
    public void testFileAttributes() throws IOException {
        Path whitePng = MCRPath.getPath(DERIVATE_1, "white.png");
        MCRFileAttributes<?> sourceAttributes = Files.readAttributes(whitePng, MCRFileAttributes.class);
        assertFalse("'white.png' should not be a directory", sourceAttributes.isDirectory());
        assertTrue("'white.png' should not be a regular file", sourceAttributes.isRegularFile());
        assertNotNull("'white.png' should have a digest", sourceAttributes.digest());

        MCRPath newFile = MCRPath.getPath(DERIVATE_1, "file1");
        MCRTransactionHelper.beginTransaction();
        Files.write(newFile, new byte[] { 1, 3, 3, 7 });
        MCRFileAttributes<?> newFileAttributes = Files.readAttributes(newFile, MCRFileAttributes.class);
        assertFalse("'file1' should not be a directory", newFileAttributes.isDirectory());
        assertTrue("'file1' should not be a regular file", newFileAttributes.isRegularFile());
        MCRDigest newFileDigest = newFileAttributes.digest();
        assertNotNull("'file1' should have a digest", newFileDigest);
        MCRTransactionHelper.commitTransaction();

        MCRFileAttributes<?> newFileAttributesAfterCommit = Files.readAttributes(newFile, MCRFileAttributes.class);
        MCRDigest afterCommitDigest = newFileAttributesAfterCommit.digest();
        assertNotNull("'file1' should have a digest", afterCommitDigest);
        assertEquals(newFileDigest, afterCommitDigest);
    }

    @Test
    public void moveAndCheckAttributes() throws IOException {
        Path source = MCRPath.getPath(DERIVATE_1, "white.png");
        Path target = MCRPath.getPath(DERIVATE_2, "moved.png");
        MCRTransactionHelper.beginTransaction();
        Files.move(source, target);
        BasicFileAttributes targetFileAttributes = Files.readAttributes(target, BasicFileAttributes.class);
        assertNotNull("'moved.png' should have basic file attributes", targetFileAttributes);
        assertTrue("'moved.png' should be a regular file", targetFileAttributes.isRegularFile());
        assertThrows(NoSuchFileException.class, () -> Files.readAttributes(source, BasicFileAttributes.class));
        MCRTransactionHelper.commitTransaction();
    }

}
