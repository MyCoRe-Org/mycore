package org.mycore.ocfl.niofs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mycore.ocfl.MCROCFLTestCaseHelper.BLACK_PNG;
import static org.mycore.ocfl.MCROCFLTestCaseHelper.DERIVATE_1;
import static org.mycore.ocfl.MCROCFLTestCaseHelper.EMPTY_DIRECTORY;
import static org.mycore.ocfl.MCROCFLTestCaseHelper.WHITE_PNG;
import static org.mycore.ocfl.niofs.MCROCFLVirtualObject.FILES_DIRECTORY;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.MCROCFLTestCaseHelper;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.test.MyCoReTest;

import io.ocfl.api.exception.NotFoundException;
import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.OcflObjectVersion;

@MyCoReTest
@ExtendWith({ MCRPermutationExtension.class, MCROCFLSetupExtension.class })
public class MCROCFLVirtualObjectTest {

    protected MCROCFLRepository repository;

    @PermutedParam
    private boolean remote;

    @PermutedParam
    private boolean purge;

    @TestTemplate
    public void toPhysicalPath() throws IOException {
        MCRVersionedPath source = WHITE_PNG;
        MCRVersionedPath target = MCRVersionedPath.head(MCROCFLTestCaseHelper.DERIVATE_1, "moved.png");
        if (remote) {
            assertThrows(MCROCFLVirtualObject.CannotDeterminePhysicalPathException.class,
                () -> getVirtualObject().toPhysicalPath(source));
        } else {
            assertNotNull(getVirtualObject().toPhysicalPath(source));
        }
        MCRTransactionManager.beginTransactions();
        Files.move(source, target);
        assertNotNull(getVirtualObject().toPhysicalPath(target));
        MCRTransactionManager.commitTransactions();
        if (remote) {
            assertThrows(MCROCFLVirtualObject.CannotDeterminePhysicalPathException.class,
                () -> getVirtualObject().toPhysicalPath(target));
        } else {
            assertNotNull(getVirtualObject().toPhysicalPath(target));
        }
    }

    @TestTemplate
    public void rename() throws IOException {
        MCRVersionedPath rootDir = MCRVersionedPath.head(MCROCFLTestCaseHelper.DERIVATE_1, "");
        MCRVersionedPath existingDir = MCRVersionedPath.head(MCROCFLTestCaseHelper.DERIVATE_1, "existingDir");
        MCRVersionedPath newDir = MCRVersionedPath.head(MCROCFLTestCaseHelper.DERIVATE_1, "newDir");
        MCRVersionedPath nonExistentDir = MCRVersionedPath.head(MCROCFLTestCaseHelper.DERIVATE_1, "nonExistentDir");
        MCRVersionedPath nonExistentDir2 = MCRVersionedPath.head(MCROCFLTestCaseHelper.DERIVATE_1, "nonExistentDir2");
        MCRVersionedPath notEmptyDir = MCRVersionedPath.head(MCROCFLTestCaseHelper.DERIVATE_1, "notEmptyDir");
        MCRVersionedPath fileInNotEmptyDir =
            MCRVersionedPath.head(MCROCFLTestCaseHelper.DERIVATE_1, "notEmptyDir/file.txt");

        // Create initial directory structures for testing
        MCRTransactionManager.beginTransactions();
        Files.createDirectory(existingDir);
        Files.createDirectory(notEmptyDir);
        Files.write(fileInNotEmptyDir, new byte[] { 1, 2, 3, 4 });
        MCRTransactionManager.commitTransactions();

        // Test 1: Directory must exist
        assertFalse(getVirtualObject().exists(nonExistentDir), "nonExistentDir should not exist");
        MCRTransactionManager.beginTransactions();
        assertThrows(NoSuchFileException.class, () -> {
            Files.move(nonExistentDir, nonExistentDir2);
        }, "Renaming should fail if the directory does not exist");
        MCRTransactionManager.commitTransactions();

        // Test 2: Directory must be empty
        assertTrue(getVirtualObject().exists(notEmptyDir), "notEmptyDir should exist");
        MCRTransactionManager.beginTransactions();
        assertThrows(DirectoryNotEmptyException.class, () -> {
            Files.move(notEmptyDir, nonExistentDir);
        }, "Renaming should fail if the directory is not empty");
        MCRTransactionManager.commitTransactions();

        // Test 3: New directory name must be available
        assertTrue(getVirtualObject().exists(existingDir), "existingDir should exist");
        assertFalse(getVirtualObject().exists(newDir), "newDir should not exist yet");
        MCRTransactionManager.beginTransactions();
        Files.createDirectory(newDir);
        assertThrows(
            FileAlreadyExistsException.class, () -> {
                Files.move(existingDir, newDir);
            }, "Renaming should fail if the new directory name is already taken");
        MCRTransactionManager.commitTransactions();

        // Test 4: Directory should not be the root directory
        MCRTransactionManager.beginTransactions();
        assertThrows(IOException.class, () -> {
            Files.move(rootDir, nonExistentDir);
        }, "Renaming should fail if trying to rename the root directory");
        MCRTransactionManager.commitTransactions();

        // Test 5: Successfully move an empty directory
        MCRTransactionManager.beginTransactions();
        Files.move(EMPTY_DIRECTORY, nonExistentDir);
        MCRTransactionManager.commitTransactions();
        assertFalse(getVirtualObject().exists(EMPTY_DIRECTORY), "empty dir should not exist");
        assertTrue(getVirtualObject().exists(nonExistentDir), "nonExistentDir should exist");
    }

    @TestTemplate
    public void isAdded() throws IOException {
        MCRVersionedPath root = MCRVersionedPath.head(MCROCFLTestCaseHelper.DERIVATE_1, "/");
        MCRVersionedPath testFile = MCRVersionedPath.head(MCROCFLTestCaseHelper.DERIVATE_1, "testFile");

        assertFalse(getVirtualObject().isAdded(root), "'root' should not be added");
        assertFalse(getVirtualObject().isAdded(WHITE_PNG), "'white.png' should not be added");
        assertFalse(getVirtualObject().isAdded(testFile), "'testFile' should not be added");

        MCRTransactionManager.beginTransactions();
        Files.write(testFile, new byte[] { 1 });
        assertTrue(getVirtualObject().isAdded(testFile), "'testFile' should be added");
        root.getFileSystem().removeRoot(MCROCFLTestCaseHelper.DERIVATE_1);
        assertFalse(getVirtualObject().isAdded(testFile), "'testFile' should not be added");
        MCRTransactionManager.commitTransactions();

        if (purge) {
            // if the object was purged, then we can add it again
            MCRTransactionManager.beginTransactions();
            root.getFileSystem().createRoot(MCROCFLTestCaseHelper.DERIVATE_1);
            assertTrue(getVirtualObject().isAdded(root), "'root' should be added");
            MCRTransactionManager.commitTransactions();
        } else {
            MCRTransactionManager.beginTransactions();
            assertThrows(FileAlreadyExistsException.class,
                () -> root.getFileSystem().createRoot(MCROCFLTestCaseHelper.DERIVATE_1),
                "derivate cannot be created because it was soft deleted and therefore should still exist.'");
        }
    }

    @TestTemplate
    public void delete() throws IOException {
        MCRVersionedPath directory = MCRVersionedPath.head(MCROCFLTestCaseHelper.DERIVATE_1, "testDir");
        MCRVersionedPath subFile = MCRVersionedPath.head(MCROCFLTestCaseHelper.DERIVATE_1, "testDir/subFile.txt");

        // Create initial directory and file for testing
        MCRTransactionManager.beginTransactions();
        Files.createDirectory(directory);
        Files.write(subFile, new byte[] { 1, 2, 3, 4 });
        MCRTransactionManager.commitTransactions();

        // Ensure the initial state
        assertTrue(getVirtualObject().exists(directory), "Directory should exist");
        assertTrue(getVirtualObject().exists(subFile), "Sub-file should exist");

        // Delete the sub-file
        MCRTransactionManager.beginTransactions();
        Files.delete(subFile);
        // Verify the sub-file has been deleted and the directory still exists
        assertFalse(getVirtualObject().exists(subFile), "Sub-file should no longer exist");
        assertTrue(getVirtualObject().exists(directory), "Directory should still exist");
        MCRTransactionManager.commitTransactions();

        // Verify the sub-file has been deleted and the directory still exists
        assertFalse(getVirtualObject().exists(subFile), "Sub-file should no longer exist");
        assertTrue(getVirtualObject().exists(directory), "Directory should still exist");
    }

    @TestTemplate
    public void deleteIfExists() throws IOException {
        Assertions.assertTrue(Files.exists(WHITE_PNG), "'white.png' should exist before deletion");
        MCRTransactionManager.beginTransactions();
        Files.deleteIfExists(WHITE_PNG);
        Assertions.assertFalse(Files.exists(WHITE_PNG), "'white.png' should not exist after deletion");
        MCRTransactionManager.commitTransactions();

        MCRTransactionManager.beginTransactions();
        Files.write(WHITE_PNG, new byte[] { 1, 2, 3, 4 });
        Assertions.assertTrue(Files.exists(WHITE_PNG), "'white.png' should exist before deletion");
        Files.deleteIfExists(WHITE_PNG);
        Assertions.assertFalse(Files.exists(WHITE_PNG), "'white.png' should not exist after deletion");
        MCRTransactionManager.commitTransactions();
    }

    @TestTemplate
    public void newDirectoryStream() throws IOException {
        MCRVersionedPath rootDir = MCRVersionedPath.head(MCROCFLTestCaseHelper.DERIVATE_1, "");
        MCRVersionedPath newFile = MCRVersionedPath.head(MCROCFLTestCaseHelper.DERIVATE_1, "empty/newFile.png");

        Set<String> rootDirectoryListing;
        Set<String> emptyDirectoryListing;

        // Ensure the initial state
        assertTrue(getVirtualObject().exists(WHITE_PNG), "white.png should exist");
        assertTrue(getVirtualObject().exists(BLACK_PNG), "black.png should exist");
        assertTrue(getVirtualObject().exists(EMPTY_DIRECTORY), "empty directory should exist");

        // Verify root
        rootDirectoryListing = listDirectory(rootDir);
        assertTrue(rootDirectoryListing.contains("black.png"), "root should contain black.png");
        assertTrue(rootDirectoryListing.contains("white.png"), "root should contain white.png");
        assertTrue(rootDirectoryListing.contains("empty"), "root should contain the empty directory");
        assertEquals(3, rootDirectoryListing.size(), "root should contain exactly 3 items");

        // Verify empty
        emptyDirectoryListing = listDirectory(EMPTY_DIRECTORY);
        assertEquals(0, emptyDirectoryListing.size(), "empty should contain exactly 0 items");

        // Add a new file to empty directory
        MCRTransactionManager.beginTransactions();
        Files.write(newFile, new byte[] { 127, 127, 127 });
        emptyDirectoryListing = listDirectory(EMPTY_DIRECTORY);
        assertEquals(1, emptyDirectoryListing.size(), "empty should contain exactly 1 item");
        MCRTransactionManager.commitTransactions();
        emptyDirectoryListing = listDirectory(EMPTY_DIRECTORY);
        assertEquals(1, emptyDirectoryListing.size(), "empty should contain exactly 1 item");
        rootDirectoryListing = listDirectory(rootDir);
        assertEquals(3, rootDirectoryListing.size(), "root should contain exactly 3 items");
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

    @TestTemplate
    public void purge() throws IOException {
        MCRTransactionManager.beginTransactions();
        assertTrue(getVirtualObject().exists(WHITE_PNG), "white.png should exist");
        getVirtualObject().purge();
        assertFalse(getVirtualObject().exists(WHITE_PNG), "white.png should not exist");
        getVirtualObject().create();
        assertFalse(getVirtualObject().exists(WHITE_PNG), "white.png should not exist");
        MCRTransactionManager.commitTransactions();
        assertFalse(getVirtualObject().exists(WHITE_PNG), "white.png should not exist");

        OcflObjectVersion derivate1 = repository.getObject(ObjectVersionId.head(
            MCROCFLTestCaseHelper.DERIVATE_1_OBJECT_ID));
        assertEquals(1, derivate1.getFiles().size(),
            "there should be 1 file in " + MCROCFLTestCaseHelper.DERIVATE_1);
        assertNotNull(derivate1.getFile(FILES_DIRECTORY + ".keep"), "should have a .keep file");

        MCRTransactionManager.beginTransactions();
        Files.write(WHITE_PNG, new byte[] { 1, 3, 3, 7 });
        MCRTransactionManager.commitTransactions();

        derivate1 = repository.getObject(ObjectVersionId.head(MCROCFLTestCaseHelper.DERIVATE_1_OBJECT_ID));
        assertEquals(1, derivate1.getFiles().size(),
            "there should be 1 file in " + MCROCFLTestCaseHelper.DERIVATE_1);
        assertNotNull(derivate1.getFile(FILES_DIRECTORY + "white.png"), "should have a 'white.png' file");

        assertTrue(
            MCROCFLFileSystemProvider.get().virtualObjectProvider().exists(MCROCFLTestCaseHelper.DERIVATE_1),
            "derivate should exist");
        MCRTransactionManager.beginTransactions();
        getVirtualObject().purge();
        MCRTransactionManager.commitTransactions();
        assertFalse(
            MCROCFLFileSystemProvider.get().virtualObjectProvider().exists(MCROCFLTestCaseHelper.DERIVATE_1),
            "derivate should not exist");

        if (purge) {
            assertThrows(NotFoundException.class, MCROCFLVirtualObjectTest::getVirtualObject);
        } else {
            assertFalse(getVirtualObject().exists(WHITE_PNG), "white.png should not exist");
        }
    }

    @TestTemplate
    public void getSize() throws IOException {
        assertEquals(554, Files.size(WHITE_PNG), "original white.png should have 554 bytes");

        // write 4 bytes
        MCRTransactionManager.beginTransactions();
        Files.write(WHITE_PNG, new byte[] { 5, 6, 7, 8 });
        assertEquals(4, Files.size(WHITE_PNG), "written white.png should have 4 bytes");
        MCRTransactionManager.commitTransactions();

        // check v1
        MCRVersionedPath v1WhitePng = MCRVersionedPath.getPath(MCROCFLTestCaseHelper.DERIVATE_1, "v1", "white.png");
        assertEquals(554, Files.size(v1WhitePng), "v1 white.png should have 554 bytes");

        // check v2
        MCRVersionedPath v2WhitePng = MCRVersionedPath.getPath(MCROCFLTestCaseHelper.DERIVATE_1, "v2", "white.png");
        assertEquals(4, Files.size(v2WhitePng), "v2 white.png should have 4 bytes");
    }

    @TestTemplate
    public void getFileKey() throws IOException {
        MCRVersionedPath notFoundPng = MCRVersionedPath.head(MCROCFLTestCaseHelper.DERIVATE_1, "notFound.png");

        Object v1WhitePngFileKey = getFileKey(WHITE_PNG);
        Object v1BlackPngFileKey = getFileKey(BLACK_PNG);

        if (remote) {
            assertNull(v1WhitePngFileKey, "fileKey of original white.png should be null on remote repositories");
            assertNull(v1BlackPngFileKey, "fileKey of original black.png should be null on remote repositories");
        } else {
            assertNotNull(v1WhitePngFileKey, "fileKey of original white.png should not be null");
            assertNotNull(v1BlackPngFileKey, "fileKey of original black.png should not be null");
        }
        assertThrows(NoSuchFileException.class, () -> getFileKey(notFoundPng),
            "fileKey of notFound.png should not exist yet");
    }

    @TestTemplate
    public void copy() throws IOException, MCROCFLInactiveTransactionException {
        MCRVersionedPath path1 = MCRVersionedPath.head(DERIVATE_1, "file1");
        MCRVersionedPath path2 = MCRVersionedPath.head(DERIVATE_1, "file2");
        MCRVersionedPath path3 = MCRVersionedPath.head(DERIVATE_1, "file3");
        byte[] path1Data = { 1 };
        byte[] path3Data = { 2 };

        MCRTransactionManager.beginTransactions();
        Files.write(path1, path1Data);
        MCRTransactionManager.commitTransactions();

        MCRTransactionManager.beginTransactions();
        Files.copy(path1, path2);
        Assertions.assertTrue(Files.exists(path1), "'path1' should exist");
        Assertions.assertTrue(Files.exists(path2), "'path2' should exist");
        Assertions.assertArrayEquals(path1Data, Files.readAllBytes(path1),
            "'path1' should have the content of the temp store");
        Assertions.assertArrayEquals(path1Data, Files.readAllBytes(path2),
            "'path2' should have the content of the temp store");
        MCRTransactionManager.commitTransactions();

        MCRTransactionManager.beginTransactions();
        Assertions.assertArrayEquals(path1Data, Files.readAllBytes(path1),
            "'path1' should have the content of the temp store");
        Assertions.assertArrayEquals(path1Data, Files.readAllBytes(path2),
            "'path2' should have the content of the temp store");
        Files.copy(path1, path3);
        Assertions.assertArrayEquals(path1Data, Files.readAllBytes(path3),
            "'path3' should have the content of the temp store");
        Files.write(path3, path3Data);
        Files.copy(path3, path2, StandardCopyOption.REPLACE_EXISTING);
        Assertions.assertArrayEquals(path3Data, Files.readAllBytes(path2),
            "'path2' should have the content of the transactional store");
        MCRTransactionManager.commitTransactions();
    }

    @TestTemplate
    public void externalCopy() throws IOException {
        MCRVersionedPath whitePng = WHITE_PNG;
        MCRVersionedPath blackPng = BLACK_PNG;
        MCRVersionedPath target = MCRVersionedPath.head(MCROCFLTestCaseHelper.DERIVATE_2, "white.png");
        long whiteSize = Files.size(whitePng);
        long blackSize = Files.size(blackPng);

        // copy from one derivate to another
        assertFalse(Files.exists(target),
            "white.png should not exist in " + MCROCFLTestCaseHelper.DERIVATE_2);
        MCRTransactionManager.beginTransactions();
        Files.copy(whitePng, target);
        assertTrue(Files.exists(target), "white.png should exist in " + MCROCFLTestCaseHelper.DERIVATE_2);
        assertEquals(whiteSize, Files.size(target), "white.png should have the same size");
        MCRTransactionManager.commitTransactions();
        assertEquals(whiteSize, Files.size(target), "white.png should have the same size");
        assertTrue(Files.exists(target), "white.png should exist in " + MCROCFLTestCaseHelper.DERIVATE_2);

        // overwrite from one derivate to another
        MCRTransactionManager.beginTransactions();
        Files.copy(blackPng, target, StandardCopyOption.REPLACE_EXISTING);
        assertEquals(blackSize, Files.size(target), "white.png should have the same size");
        MCRTransactionManager.commitTransactions();
        assertEquals(blackSize, Files.size(target), "white.png should have the same size");
    }

    private static Object getFileKey(MCRVersionedPath path) throws IOException {
        return Files.readAttributes(path, BasicFileAttributes.class).fileKey();
    }

    private static MCROCFLVirtualObject getVirtualObject() {
        return MCROCFLFileSystemProvider.get().virtualObjectProvider().get(MCROCFLTestCaseHelper.DERIVATE_1);
    }

}
