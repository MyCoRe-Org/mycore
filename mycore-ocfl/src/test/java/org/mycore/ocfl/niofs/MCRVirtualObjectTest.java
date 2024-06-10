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
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.datamodel.niofs.MCRVersionedPath;

import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.OcflObjectVersion;

public class MCRVirtualObjectTest extends MCROCFLTestCase {

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

        OcflObjectVersion derivate1 = repository.getObject(ObjectVersionId.head(DERIVATE_1));
        assertEquals("there should be 1 file in " + DERIVATE_1, 1, derivate1.getFiles().size());
        assertNotNull("should have a .keep file", derivate1.getFile(".keep"));

        MCRTransactionHelper.beginTransaction();
        Files.write(whitePng, new byte[] { 1, 3, 3, 7 });
        MCRTransactionHelper.commitTransaction();

        derivate1 = repository.getObject(ObjectVersionId.head(DERIVATE_1));
        assertEquals("there should be 1 file in " + DERIVATE_1, 1, derivate1.getFiles().size());
        assertNotNull("should have a 'white.png' file", derivate1.getFile("white.png"));
    }

    @Test
    public void isFileAddedOrModified() throws IOException {
        MCRVersionedPath whitePng = MCRVersionedPath.head(DERIVATE_1, "white.png");
        MCRVersionedPath movedPng = MCRVersionedPath.head(DERIVATE_1, "moved.png");

        // Ensure the file exists and is not modified initially
        assertTrue("white.png should exist", getVirtualObject().exists(whitePng));

        // Modify the file
        MCRTransactionHelper.beginTransaction();
        assertFalse("File should not be marked as modified initially", getVirtualObject().isAddedOrModified(whitePng));
        Files.write(whitePng, new byte[] { 1, 3, 3, 7 });
        assertTrue("File should be marked as modified after content change",
            getVirtualObject().isAddedOrModified(whitePng));
        MCRTransactionHelper.commitTransaction();

        // Rename the file
        MCRTransactionHelper.beginTransaction();
        Files.move(whitePng, movedPng);
        assertTrue("Renamed file should be marked as modified", getVirtualObject().isAddedOrModified(movedPng));
        assertThrows(NoSuchFileException.class, () -> getVirtualObject().isAddedOrModified(whitePng));
        MCRTransactionHelper.commitTransaction();

        // Add a new file (recreate white.png)
        MCRTransactionHelper.beginTransaction();
        Files.write(whitePng, new byte[] { 5, 6, 7, 8 });
        assertTrue("Newly added file should be marked as modified", getVirtualObject().isAddedOrModified(whitePng));
        MCRTransactionHelper.commitTransaction();

        // Truncate the file
        MCRTransactionHelper.beginTransaction();
        Files.write(whitePng, new byte[] {}, StandardOpenOption.TRUNCATE_EXISTING);
        assertTrue("Truncated file should be marked as modified", getVirtualObject().isAddedOrModified(whitePng));
        assertEquals("Truncated file should have size 0", 0, Files.size(whitePng));
        MCRTransactionHelper.commitTransaction();

        // Delete the file
        MCRTransactionHelper.beginTransaction();
        Files.delete(whitePng);
        assertThrows(NoSuchFileException.class, () -> getVirtualObject().isAddedOrModified(whitePng));
        MCRTransactionHelper.commitTransaction();

        // Check no modification scenario (recreate white.png with same initial content)
        MCRTransactionHelper.beginTransaction();
        Files.write(whitePng, new byte[] { 1, 2, 3, 4 });
        MCRTransactionHelper.commitTransaction();
        MCRTransactionHelper.beginTransaction();
        Files.write(whitePng, new byte[] { 4, 3, 2, 1 });
        assertTrue("File should be marked as modified", getVirtualObject().isAddedOrModified(whitePng));
        Files.write(whitePng, new byte[] { 1, 2, 3, 4 });
        assertFalse("File should not be marked as modified after re-creation with same content",
            getVirtualObject().isAddedOrModified(whitePng));
        MCRTransactionHelper.commitTransaction();
    }

    @Test
    public void isDirectoryAddedOrModified() throws IOException {
        MCRVersionedPath rootDir = MCRVersionedPath.head(DERIVATE_1, "/");
        MCRVersionedPath testDir = MCRVersionedPath.head(DERIVATE_1, "testDir");
        MCRVersionedPath movedDir = MCRVersionedPath.head(DERIVATE_1, "movedDir");
        MCRVersionedPath testFile = MCRVersionedPath.head(DERIVATE_1, "testDir/testFile.txt");
        MCRVersionedPath movedFile = MCRVersionedPath.head(DERIVATE_1, "movedDir/testFile.txt");

        // Ensure the directory exists and is not modified initially
        MCRTransactionHelper.beginTransaction();
        Files.createDirectory(testDir);
        assertTrue("testDir should exist", getVirtualObject().exists(testDir));
        assertTrue("testDir should be marked as modified", getVirtualObject().isAddedOrModified(testDir));
        assertTrue("Adding directory should mark root as modified", getVirtualObject().isAddedOrModified(rootDir));
        MCRTransactionHelper.commitTransaction();

        // Rename the directory
        MCRTransactionHelper.beginTransaction();
        Files.move(testDir, movedDir);
        assertTrue("movedDir should be marked as modified", getVirtualObject().isAddedOrModified(movedDir));
        assertThrows(NoSuchFileException.class, () -> getVirtualObject().isAddedOrModified(testDir));
        assertTrue("Renaming directory should mark root as modified", getVirtualObject().isAddedOrModified(rootDir));
        MCRTransactionHelper.commitTransaction();

        // Create a new file in the directory
        MCRTransactionHelper.beginTransaction();
        Files.write(testFile, new byte[] { 1, 2, 3, 4 });
        assertTrue("Directory should be marked as modified after creating a file",
            getVirtualObject().isAddedOrModified(testDir));
        MCRTransactionHelper.commitTransaction();

        // Add a new file to the renamed directory
        MCRTransactionHelper.beginTransaction();
        Files.write(movedFile, new byte[] { 5, 6, 7, 8 });
        assertTrue("Newly added file should mark directory as modified",
            getVirtualObject().isAddedOrModified(movedDir));
        assertFalse("Adding sub file should not mark root as modified", getVirtualObject().isAddedOrModified(rootDir));
        MCRTransactionHelper.commitTransaction();

        // Delete the file from the directory
        MCRTransactionHelper.beginTransaction();
        Files.delete(movedFile);
        assertTrue("Deleting file should mark directory as modified", getVirtualObject().isAddedOrModified(movedDir));
        assertFalse("Deleting sub file should not mark root as modified",
            getVirtualObject().isAddedOrModified(rootDir));
        MCRTransactionHelper.commitTransaction();

        // Delete the directory
        MCRTransactionHelper.beginTransaction();
        Files.delete(movedDir);
        assertThrows(NoSuchFileException.class, () -> getVirtualObject().isAddedOrModified(movedDir));
        assertTrue("Deleting directory should mark root as modified", getVirtualObject().isAddedOrModified(rootDir));
        MCRTransactionHelper.commitTransaction();
    }

    private static MCROCFLVirtualObject getVirtualObject() {
        return MCROCFLFileSystemProvider.get().virtualObjectProvider().get(DERIVATE_1);
    }

}
