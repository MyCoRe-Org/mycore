package org.mycore.ocfl.niofs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.datamodel.niofs.MCRVersionedPath;

import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.OcflObjectVersion;

public class MCRVirtualObjectTest extends MCROCFLTestCase {

    public MCRVirtualObjectTest(boolean remote) {
        super(remote);
    }

    @Test
    public void toPhysicalPath() throws IOException {
        MCRVersionedPath source = MCRVersionedPath.head(DERIVATE_1, "white.png");
        MCRVersionedPath target = MCRVersionedPath.head(DERIVATE_1, "moved.png");

        assertNotNull(getVirtualObject().toPhysicalPath(source));
        assertThrows(NoSuchFileException.class, () -> getVirtualObject().toPhysicalPath(target));

        MCRTransactionHelper.beginTransaction();
        Files.move(source, target);
        assertThrows(NoSuchFileException.class, () -> getVirtualObject().toPhysicalPath(source));
        assertNotNull(getVirtualObject().toPhysicalPath(target));
        MCRTransactionHelper.commitTransaction();

        assertThrows(NoSuchFileException.class, () -> getVirtualObject().toPhysicalPath(source));
        assertNotNull(getVirtualObject().toPhysicalPath(target));
    }

    @Test
    public void rename() throws IOException {
        MCRVersionedPath rootDir = MCRVersionedPath.head(DERIVATE_1, "");
        MCRVersionedPath existingDir = MCRVersionedPath.head(DERIVATE_1, "existingDir");
        MCRVersionedPath newDir = MCRVersionedPath.head(DERIVATE_1, "newDir");
        MCRVersionedPath nonExistentDir = MCRVersionedPath.head(DERIVATE_1, "nonExistentDir");
        MCRVersionedPath nonExistentDir2 = MCRVersionedPath.head(DERIVATE_1, "nonExistentDir2");
        MCRVersionedPath notEmptyDir = MCRVersionedPath.head(DERIVATE_1, "notEmptyDir");
        MCRVersionedPath fileInNotEmptyDir = MCRVersionedPath.head(DERIVATE_1, "notEmptyDir/file.txt");
        MCRVersionedPath emptyDir = MCRVersionedPath.head(DERIVATE_1, "empty");

        // Create initial directory structures for testing
        MCRTransactionHelper.beginTransaction();
        Files.createDirectory(existingDir);
        Files.createDirectory(notEmptyDir);
        Files.write(fileInNotEmptyDir, new byte[] { 1, 2, 3, 4 });
        MCRTransactionHelper.commitTransaction();

        // Test 1: Directory must exist
        assertFalse("nonExistentDir should not exist", getVirtualObject().exists(nonExistentDir));
        MCRTransactionHelper.beginTransaction();
        assertThrows("Renaming should fail if the directory does not exist", NoSuchFileException.class, () -> {
            Files.move(nonExistentDir, nonExistentDir2);
        });
        MCRTransactionHelper.commitTransaction();

        // Test 2: Directory must be empty
        assertTrue("notEmptyDir should exist", getVirtualObject().exists(notEmptyDir));
        MCRTransactionHelper.beginTransaction();
        assertThrows("Renaming should fail if the directory is not empty", DirectoryNotEmptyException.class, () -> {
            Files.move(notEmptyDir, nonExistentDir);
        });
        MCRTransactionHelper.commitTransaction();

        // Test 3: New directory name must be available
        assertTrue("existingDir should exist", getVirtualObject().exists(existingDir));
        assertFalse("newDir should not exist yet", getVirtualObject().exists(newDir));
        MCRTransactionHelper.beginTransaction();
        Files.createDirectory(newDir);
        assertThrows("Renaming should fail if the new directory name is already taken",
            FileAlreadyExistsException.class, () -> {
                Files.move(existingDir, newDir);
            });
        MCRTransactionHelper.commitTransaction();

        // Test 4: Directory should not be the root directory
        MCRTransactionHelper.beginTransaction();
        assertThrows("Renaming should fail if trying to rename the root directory", IOException.class, () -> {
            Files.move(rootDir, nonExistentDir);
        });
        MCRTransactionHelper.commitTransaction();

        // Test 5: Successfully move an empty directory
        MCRTransactionHelper.beginTransaction();
        Files.move(emptyDir, nonExistentDir);
        MCRTransactionHelper.commitTransaction();
        assertFalse("empty dir should not exist", getVirtualObject().exists(emptyDir));
        assertTrue("nonExistentDir should exist", getVirtualObject().exists(nonExistentDir));
    }

    @Test
    public void isAdded() throws IOException {
        MCRVersionedPath root = MCRVersionedPath.head(DERIVATE_1, "/");
        MCRVersionedPath whitePng = MCRVersionedPath.head(DERIVATE_1, "white.png");
        MCRVersionedPath testFile = MCRVersionedPath.head(DERIVATE_1, "testFile");

        assertFalse("'root' should not be added", getVirtualObject().isAdded(root));
        assertFalse("'white.png' should not be added", getVirtualObject().isAdded(whitePng));
        assertFalse("'testFile' should not be added", getVirtualObject().isAdded(testFile));

        MCRTransactionHelper.beginTransaction();
        Files.write(testFile, new byte[] { 1 });
        assertTrue("'testFile' should be added", getVirtualObject().isAdded(testFile));
        root.getFileSystem().removeRoot(DERIVATE_1);
        assertFalse("'testFile' should not be added", getVirtualObject().isAdded(testFile));
        MCRTransactionHelper.commitTransaction();

        MCRTransactionHelper.beginTransaction();
        root.getFileSystem().createRoot(DERIVATE_1);
        assertTrue("'root' should be added", getVirtualObject().isAdded(root));
        MCRTransactionHelper.commitTransaction();
    }

    @Test
    public void delete() throws IOException {
        MCRVersionedPath directory = MCRVersionedPath.head(DERIVATE_1, "testDir");
        MCRVersionedPath subFile = MCRVersionedPath.head(DERIVATE_1, "testDir/subFile.txt");

        // Create initial directory and file for testing
        MCRTransactionHelper.beginTransaction();
        Files.createDirectory(directory);
        Files.write(subFile, new byte[] { 1, 2, 3, 4 });
        MCRTransactionHelper.commitTransaction();

        // Ensure the initial state
        assertTrue("Directory should exist", getVirtualObject().exists(directory));
        assertTrue("Sub-file should exist", getVirtualObject().exists(subFile));

        // Delete the sub-file
        MCRTransactionHelper.beginTransaction();
        Files.delete(subFile);
        // Verify the sub-file has been deleted and the directory still exists
        assertFalse("Sub-file should no longer exist", getVirtualObject().exists(subFile));
        assertTrue("Directory should still exist", getVirtualObject().exists(directory));
        MCRTransactionHelper.commitTransaction();

        // Verify the sub-file has been deleted and the directory still exists
        assertFalse("Sub-file should no longer exist", getVirtualObject().exists(subFile));
        assertTrue("Directory should still exist", getVirtualObject().exists(directory));
    }

    @Test
    public void newDirectoryStream() throws IOException {
        MCRVersionedPath rootDir = MCRVersionedPath.head(DERIVATE_1, "");
        MCRVersionedPath blackPng = MCRVersionedPath.head(DERIVATE_1, "black.png");
        MCRVersionedPath whitePng = MCRVersionedPath.head(DERIVATE_1, "white.png");
        MCRVersionedPath emptyDir = MCRVersionedPath.head(DERIVATE_1, "empty");
        MCRVersionedPath newFile = MCRVersionedPath.head(DERIVATE_1, "empty/newFile.png");

        Set<String> rootDirectoryListing;
        Set<String> emptyDirectoryListing;

        // Ensure the initial state
        assertTrue("black.png should exist", getVirtualObject().exists(blackPng));
        assertTrue("white.png should exist", getVirtualObject().exists(whitePng));
        assertTrue("empty directory should exist", getVirtualObject().exists(emptyDir));

        // Verify root
        rootDirectoryListing = listDirectory(rootDir);
        assertTrue("root should contain black.png", rootDirectoryListing.contains("black.png"));
        assertTrue("root should contain white.png", rootDirectoryListing.contains("white.png"));
        assertTrue("root should contain the empty directory", rootDirectoryListing.contains("empty"));
        assertEquals("root should contain exactly 3 items", 3, rootDirectoryListing.size());

        // Verify empty
        emptyDirectoryListing = listDirectory(emptyDir);
        assertEquals("empty should contain exactly 0 items", 0, emptyDirectoryListing.size());

        // Add a new file to empty directory
        MCRTransactionHelper.beginTransaction();
        Files.write(newFile, new byte[] { 127, 127, 127 });
        emptyDirectoryListing = listDirectory(emptyDir);
        assertEquals("empty should contain exactly 1 item", 1, emptyDirectoryListing.size());
        MCRTransactionHelper.commitTransaction();
        emptyDirectoryListing = listDirectory(emptyDir);
        assertEquals("empty should contain exactly 1 item", 1, emptyDirectoryListing.size());
        rootDirectoryListing = listDirectory(rootDir);
        assertEquals("root should contain exactly 3 items", 3, rootDirectoryListing.size());
    }

    private static Set<String> listDirectory(MCRVersionedPath directory) throws IOException {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            Set<String> fileNames = new HashSet<>();
            for (Path path : directoryStream) {
                fileNames.add(path.getFileName().toString());
            }
            return fileNames;
        }
    }

    @Test
    public void purge() throws IOException {
        MCRVersionedPath whitePng = MCRVersionedPath.head(DERIVATE_1, "white.png");

        MCRTransactionHelper.beginTransaction();
        assertTrue("white.png should exist", getVirtualObject().exists(whitePng));
        getVirtualObject().purge();
        assertFalse("white.png should not exist", getVirtualObject().exists(whitePng));
        getVirtualObject().create();
        assertFalse("white.png should not exist", getVirtualObject().exists(whitePng));
        MCRTransactionHelper.commitTransaction();
        assertFalse("white.png should not exist", getVirtualObject().exists(whitePng));

        OcflObjectVersion derivate1 = repository.getObject(ObjectVersionId.head(DERIVATE_1_OBJECT_ID));
        assertEquals("there should be 1 file in " + DERIVATE_1, 1, derivate1.getFiles().size());
        assertNotNull("should have a .keep file", derivate1.getFile(".keep"));

        MCRTransactionHelper.beginTransaction();
        Files.write(whitePng, new byte[] { 1, 3, 3, 7 });
        MCRTransactionHelper.commitTransaction();

        derivate1 = repository.getObject(ObjectVersionId.head(DERIVATE_1_OBJECT_ID));
        assertEquals("there should be 1 file in " + DERIVATE_1, 1, derivate1.getFiles().size());
        assertNotNull("should have a 'white.png' file", derivate1.getFile("white.png"));
    }

    @Test
    public void getSize() throws IOException {
        MCRVersionedPath headWhitePng = MCRVersionedPath.head(DERIVATE_1, "white.png");
        assertEquals("original white.png should have 554 bytes", 554, Files.size(headWhitePng));

        // write 4 bytes
        MCRTransactionHelper.beginTransaction();
        Files.write(headWhitePng, new byte[] { 5, 6, 7, 8 });
        assertEquals("written white.png should have 4 bytes", 4, Files.size(headWhitePng));
        MCRTransactionHelper.commitTransaction();

        // check v1
        MCRVersionedPath v1WhitePng = MCRVersionedPath.getPath(DERIVATE_1, "v1", "white.png");
        assertEquals("v1 white.png should have 554 bytes", 554, Files.size(v1WhitePng));

        // check v2
        MCRVersionedPath v2WhitePng = MCRVersionedPath.getPath(DERIVATE_1, "v2", "white.png");
        assertEquals("v2 white.png should have 4 bytes", 4, Files.size(v2WhitePng));
    }

    @Test
    public void getFileKey() throws IOException {
        MCRVersionedPath v1WhitePng = MCRVersionedPath.head(DERIVATE_1, "white.png");
        MCRVersionedPath v1BlackPng = MCRVersionedPath.head(DERIVATE_1, "black.png");
        MCRVersionedPath notFoundPng = MCRVersionedPath.head(DERIVATE_1, "notFound.png");

        Object v1WhitePngFileKey = getFileKey(v1WhitePng);
        Object v1BlackPngFileKey = getFileKey(v1BlackPng);

        assertNotNull("fileKey of original white.png should not be null", v1WhitePngFileKey);
        assertNotNull("fileKey of original black.png should not be null", v1BlackPngFileKey);
        assertThrows("fileKey of notFound.png should not exist yet", NoSuchFileException.class,
            () -> getFileKey(notFoundPng));
    }

    private static Object getFileKey(MCRVersionedPath path) throws IOException {
        return Files.readAttributes(path, BasicFileAttributes.class).fileKey();
    }

    private static MCROCFLVirtualObject getVirtualObject() {
        return MCROCFLFileSystemProvider.get().virtualObjectProvider().get(DERIVATE_1);
    }

}
