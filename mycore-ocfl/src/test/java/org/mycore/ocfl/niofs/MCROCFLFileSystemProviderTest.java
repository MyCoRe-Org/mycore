package org.mycore.ocfl.niofs;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mycore.ocfl.MCROCFLTestCaseHelper.DERIVATE_1;
import static org.mycore.ocfl.MCROCFLTestCaseHelper.DERIVATE_2;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.test.MyCoReTest;

import io.ocfl.api.model.ObjectVersionId;

@MyCoReTest
@ExtendWith({ MCRPermutationExtension.class, MCROCFLSetupExtension.class })
public class MCROCFLFileSystemProviderTest {

    protected MCROCFLRepository repository;

    @PermutedParam
    private boolean remote;

    @PermutedParam
    private boolean purge;

    @TestTemplate
    public void checkAccess() {
        // check files
        Path whitePngPath = MCRPath.getPath(DERIVATE_1, "white.png");
        Path blackPngPath = MCRPath.getPath(DERIVATE_1, "black.png");
        Path purplePngPath = MCRPath.getPath(DERIVATE_1, "purple.png");
        Path emptyDirectoryPath = MCRPath.getPath(DERIVATE_1, "empty");
        Path keepFile = MCRPath.getPath(DERIVATE_1, "empty/.keep");
        assertTrue(Files.exists(whitePngPath), "'white.png' should exist");
        assertTrue(Files.exists(blackPngPath), "'black.png' should exist");
        assertTrue(Files.exists(emptyDirectoryPath), "'empty' directory should exists");
        Assertions.assertFalse(Files.exists(purplePngPath), "'purple.png' should not exist");
        Assertions.assertFalse(Files.exists(keepFile), "'empty/.keep' file should not exists");

        // check writable
        Assertions.assertFalse(Files.exists(MCRPath.getPath(DERIVATE_2, "")), "'" + DERIVATE_2 + "' should not exists");
        MCRTransactionManager.beginTransactions();
        Assertions.assertFalse(Files.exists(MCRPath.getPath(DERIVATE_2, "")), "'" + DERIVATE_2 + "' should not exists");
        MCRTransactionManager.commitTransactions();
    }

    @TestTemplate
    public void createDirectory() throws IOException {
        // create directory in existing ocfl object
        final MCRPath path1 = MCRPath.getPath(DERIVATE_1, "test");
        Assertions.assertFalse(Files.exists(path1), "'test' directory should not exists");
        MCRTransactionManager.beginTransactions();
        Files.createDirectory(path1);
        assertTrue(Files.exists(path1), "'test' directory should exist before committing");
        MCRTransactionManager.commitTransactions();
        assertTrue(Files.exists(path1), "'test' directory should exist after committing");

        // create already existing directory
        MCRTransactionManager.beginTransactions();
        assertThrows(FileAlreadyExistsException.class, () -> Files.createDirectory(path1));
        MCRTransactionManager.commitTransactions();

        // create directory in non-existing ocfl object
        final MCRPath path2 = MCRPath.getPath(DERIVATE_2, "test");
        Assertions.assertFalse(Files.exists(path2), "'test' directory should not exists");
        MCRTransactionManager.beginTransactions();
        Files.createDirectory(path2);
        assertTrue(Files.exists(path2), "'test' directory should exist after create");
        MCRTransactionManager.commitTransactions();
        assertTrue(Files.exists(path2), "'test' directory should exist after commit");
    }

    @TestTemplate
    public void newByteChannel() throws IOException, URISyntaxException {
        final MCRPath whitePng = MCRPath.getPath(DERIVATE_1, "white.png");
        final MCRPath testFile = MCRPath.getPath(DERIVATE_1, "testFile");
        // prepare
        byte[] expectedTestFileData = { 1, 3, 3, 7 };
        byte[] expectedWhitePngData = Files.readAllBytes(whitePng);

        // write
        Assertions.assertFalse(Files.exists(testFile), "'testFile' should not exists");
        MCRTransactionManager.beginTransactions();
        Files.write(testFile, expectedTestFileData);
        assertTrue(Files.exists(testFile), "'testFile' should exists after writing");
        MCRTransactionManager.commitTransactions();
        assertTrue(Files.exists(testFile), "'testFile' should exists after committing");

        // read
        byte[] testFileData = Files.readAllBytes(testFile);
        Assertions.assertArrayEquals(expectedTestFileData, testFileData, "byte array should be equal");
        try (SeekableByteChannel byteChannel = Files.newByteChannel(testFile)) {
            byte[] seekBytes = new byte[4];
            ByteBuffer byteBuffer = ByteBuffer.wrap(seekBytes);
            byteChannel.read(byteBuffer);
            Assertions.assertArrayEquals(expectedTestFileData, seekBytes, "byte array should be equal");
        }

        // read range
        try (SeekableByteChannel byteChannel = Files.newByteChannel(testFile)) {
            byte[] seekBytes = new byte[2];
            ByteBuffer byteBuffer = ByteBuffer.wrap(seekBytes);
            byteChannel.position(1);
            byteChannel.read(byteBuffer);
            Assertions.assertArrayEquals(new byte[] { 3, 3 }, seekBytes, "byte array should be equal");
        }

        // read v1
        ObjectVersionId versionId = ObjectVersionId.version(DERIVATE_1, 1);
        MCRVersionedPath whitePngV1 = MCRVersionedPath.getPath(
            versionId.getObjectId(), versionId.getVersionNum().toString(), "white.png");
        byte[] whitePngData = Files.readAllBytes(whitePngV1);
        Assertions.assertArrayEquals(expectedWhitePngData, whitePngData, "byte array should be equal");
    }

    @TestTemplate
    public void write() throws IOException {
        final MCRPath testFile = MCRPath.getPath(DERIVATE_1, "testFile");

        // simple write
        MCRTransactionManager.beginTransactions();
        Files.write(testFile, new byte[] { 1, 2 });
        MCRTransactionManager.commitTransactions();
        Assertions.assertArrayEquals(new byte[] { 1, 2 }, Files.readAllBytes(testFile), "byte array should be equal");

        // append
        MCRTransactionManager.beginTransactions();
        Files.write(testFile, new byte[] { 3, 4 }, StandardOpenOption.APPEND);
        MCRTransactionManager.commitTransactions();
        Assertions.assertArrayEquals(new byte[] { 1, 2, 3, 4 }, Files.readAllBytes(testFile),
            "byte array should be equal");

        // truncate
        MCRTransactionManager.beginTransactions();
        Files.write(testFile, new byte[] { 4, 3, 2, 1 }, StandardOpenOption.TRUNCATE_EXISTING);
        MCRTransactionManager.commitTransactions();
        Assertions.assertArrayEquals(new byte[] { 4, 3, 2, 1 }, Files.readAllBytes(testFile),
            "byte array should be equal");
    }

    @TestTemplate
    public void move() throws IOException {
        MCRTransactionManager.beginTransactions();
        Path source = MCRPath.getPath(DERIVATE_1, "white.png");
        Path target = MCRPath.getPath(DERIVATE_1, "moved.png");
        Files.move(source, target);
        Assertions.assertFalse(Files.exists(source), "'white.png' should not exist");
        assertTrue(Files.exists(target), "'moved.png' should exist");
        MCRTransactionManager.commitTransactions();
        Assertions.assertFalse(Files.exists(source), "'white.png' should not exist after committing");
        assertTrue(Files.exists(target), "'moved.png' should exist after committing");

        // move from different derivate
        Path newSource = MCRPath.getPath(DERIVATE_1, "moved.png");
        Path newTarget = MCRPath.getPath(DERIVATE_2, "white.png");
        MCRTransactionManager.beginTransactions();
        Files.move(newSource, newTarget);
        MCRTransactionManager.commitTransactions();
        Assertions.assertFalse(Files.exists(newSource), "'moved.png' should not exist after committing");
        assertTrue(Files.exists(newTarget), "'white.png' should exist after committing");
    }

    @TestTemplate
    public void copyFiles() throws IOException, URISyntaxException {
        Path whiteV1 = MCRVersionedPath.head(DERIVATE_1, "white.png");
        Path copiedV2 = MCRVersionedPath.head(DERIVATE_1, "copied.png");

        // copy non-existing file
        MCRTransactionManager.beginTransactions();
        assertThrows(NoSuchFileException.class, () -> Files.copy(copiedV2, whiteV1));
        MCRTransactionManager.commitTransactions();

        // copy white to copied
        MCRTransactionManager.beginTransactions();
        Files.copy(whiteV1, copiedV2);
        assertTrue(Files.exists(whiteV1), "'white.png' should exist");
        assertTrue(Files.exists(copiedV2), "'copied.png' should exist");
        MCRTransactionManager.commitTransactions();
        assertTrue(Files.exists(whiteV1), "'white.png' should exist after committing");
        assertTrue(Files.exists(copiedV2), "'copied.png' should exist after committing");

        // copy from v1 to head
        Path copiedV3 = MCRVersionedPath.getPath(DERIVATE_1, "copied2.png");
        MCRTransactionManager.beginTransactions();
        Files.copy(whiteV1, copiedV3);
        assertTrue(Files.exists(whiteV1), "'white.png' should exist");
        assertTrue(Files.exists(copiedV2), "'copied.png' should exist");
        assertTrue(Files.exists(copiedV3), "'copied2.png' should exist");
        MCRTransactionManager.commitTransactions();
        assertTrue(Files.exists(whiteV1), "'white.png' should exist after committing");
        assertTrue(Files.exists(copiedV2), "'copied.png' should exist");
        assertTrue(Files.exists(copiedV3), "'copied2.png' should exist after committing");

        // copy to v1
        MCRTransactionManager.beginTransactions();
        assertThrows("cannot copy to non head version", IOException.class, () -> {
            Files.copy(copiedV3, whiteV1);
        });
        MCRTransactionManager.commitTransactions();

        // copy foreign source file
        MCRTransactionManager.beginTransactions();
        URL whitePngFromDerivate2URL = getClass().getClassLoader().getResource(DERIVATE_2 + "/white.png");
        Assertions.assertNotNull(whitePngFromDerivate2URL);
        Path foreignSource = Path.of(whitePngFromDerivate2URL.toURI());
        MCRPath ocflTarget = MCRPath.getPath(DERIVATE_1, "copied3.png");
        Files.copy(foreignSource, ocflTarget, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        assertTrue(Files.exists(ocflTarget), "'copied3.png' should exist");
        MCRTransactionManager.commitTransactions();
        assertTrue(Files.exists(ocflTarget), "'copied3.png' should exist after committing");
    }

    @TestTemplate
    public void copyDirectories() throws IOException {
        Path emptyDirectory = MCRVersionedPath.head(DERIVATE_1, "empty");
        Path copiedEmptyDirectory = MCRVersionedPath.head(DERIVATE_1, "emptyCopy");

        // copy empty directory
        MCRTransactionManager.beginTransactions();
        Files.copy(emptyDirectory, copiedEmptyDirectory);
        assertTrue(Files.exists(emptyDirectory), "'empty' directory should exist");
        assertTrue(Files.exists(copiedEmptyDirectory), "'emptyCopy' directory should exist");
        MCRTransactionManager.commitTransactions();

        // copy to non-empty directory
        MCRTransactionManager.beginTransactions();
        Files.write(emptyDirectory.resolve("file"), new byte[] { 1, 3, 3, 7 });
        assertThrows(DirectoryNotEmptyException.class,
            () -> Files.copy(copiedEmptyDirectory, emptyDirectory, StandardCopyOption.REPLACE_EXISTING));
        MCRTransactionManager.commitTransactions();
    }

    @TestTemplate
    public void delete() throws IOException {
        Path whitePng = MCRPath.getPath(DERIVATE_1, "white.png");
        assertTrue(Files.exists(whitePng), "'white.png' should exist");
        MCRTransactionManager.beginTransactions();
        Files.delete(whitePng);
        Assertions.assertFalse(Files.exists(whitePng), "'white.png' should not exist");
        MCRTransactionManager.commitTransactions();
        Assertions.assertFalse(Files.exists(whitePng), "'white.png' should not exist");
    }

    @TestTemplate
    public void newDirectoryStream() throws IOException {
        // root directory
        Path rootPath = MCRPath.getPath(DERIVATE_1, "/");
        List<Path> rootContent = List.of(
            MCRPath.getPath(DERIVATE_1, "white.png"),
            MCRPath.getPath(DERIVATE_1, "black.png"),
            MCRPath.getPath(DERIVATE_1, "empty"));
        AtomicInteger numPaths = new AtomicInteger();
        int targetPaths = rootContent.size();

        MCRTransactionManager.beginTransactions();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(rootPath)) {
            paths.forEach(path -> {
                numPaths.getAndIncrement();
                assertTrue(rootContent.contains(path), "'" + path + "' should be in root '/'");
            });
        }
        Assertions.assertEquals(targetPaths, numPaths.get(),
            "There should be '" + targetPaths + "' paths in directory stream.");
        MCRTransactionManager.commitTransactions();

        // empty directory
        Path emptyDirectory = MCRPath.getPath(DERIVATE_1, "/empty");
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(emptyDirectory)) {
            assertThrows(
                "'empty' directory should be empty",
                NoSuchElementException.class,
                () -> paths.iterator().next());
        }
    }

    @TestTemplate
    public void keepTest() throws IOException {
        Path emptyDirectory = MCRPath.getPath(DERIVATE_1, "/empty");
        Path empty2Directory = MCRPath.getPath(DERIVATE_1, "/empty2");
        MCRTransactionManager.beginTransactions();
        assertTrue(Files.exists(emptyDirectory), "'empty' directory should exist");
        Assertions.assertFalse(Files.exists(empty2Directory), "'empty2' directory should exist");
        Files.move(emptyDirectory, empty2Directory);
        Assertions.assertFalse(Files.exists(emptyDirectory), "'empty' directory should not exist");
        assertTrue(Files.exists(empty2Directory), "'empty2' directory should exist");
        MCRTransactionManager.commitTransactions();
        Assertions.assertFalse(Files.exists(emptyDirectory), "'empty' directory should not exist after committing");
        assertTrue(Files.exists(empty2Directory), "'empty2' directory should exist after committing");
    }

    @TestTemplate
    public void fileAttributes() throws IOException, InterruptedException {
        MCRPath whitePng = MCRPath.getPath(DERIVATE_1, "white.png");
        BasicFileAttributes whitePngAttributes = Files.readAttributes(whitePng, BasicFileAttributes.class);
        Assertions.assertNotNull(whitePngAttributes, "attributes of 'white.png' shouldn't be null");
        assertTrue(whitePngAttributes.isRegularFile(), "'white.png' should be a regular file");
        Assertions.assertFalse(whitePngAttributes.isDirectory(), "'white.png' shouldn't be a directory");

        FileTime creationTime = whitePngAttributes.creationTime();
        FileTime modifyTime = whitePngAttributes.lastModifiedTime();
        FileTime accessTime = whitePngAttributes.lastAccessTime();
        Assertions.assertNotNull(creationTime, "creationTime() of 'white.png' should not be null");
        Assertions.assertNotNull(modifyTime, "lastModifiedTime() of 'white.png' should not be null");
        Assertions.assertNotNull(accessTime, "lastAccessTime() of 'white.png' should not be null");

        // WRITE
        MCRTransactionManager.beginTransactions();
        Thread.sleep(1);
        Files.write(whitePng, new byte[] { 1, 3, 3, 7 });
        MCRTransactionManager.commitTransactions();

        whitePngAttributes = Files.readAttributes(whitePng, BasicFileAttributes.class);
        Assertions.assertEquals(creationTime, whitePngAttributes.creationTime(),
            "creationTime() of 'white.png' should be same as before");
        assertTrue(whitePngAttributes.lastModifiedTime().toInstant().isAfter(modifyTime.toInstant()),
            "lastModifiedTime() of 'white.png' should be later");

        /*
         for some reason the access time is changed
         it looks like Files.write and Files.newByteChannel differ
         @see MCROCFLTransactionStore#newByteChannel -> return statement, at this point the update of the access time happens
         assertEquals("lastAccessTime() of 'white.png' should be same as before",
         accessTime, whitePngAttributes.lastAccessTime());
         */

        // READ
        Thread.sleep(1);
        Files.readAllBytes(whitePng);
        whitePngAttributes = Files.readAttributes(whitePng, BasicFileAttributes.class);
        assertTrue(whitePngAttributes.lastAccessTime().toInstant().isAfter(accessTime.toInstant()),
            "lastAccessTime() of 'white.png' should be later");

        // DIRECTORY
        BasicFileAttributes existingIdRootAttributes = Files.readAttributes(MCRPath.getPath(DERIVATE_1, "/"),
            BasicFileAttributes.class);
        assertTrue(existingIdRootAttributes.isDirectory(), "root directory should exist");
        assertThrows("root directory should not exist", NoSuchFileException.class, () -> {
            Files.readAttributes(MCRPath.getPath(DERIVATE_2, "/"), BasicFileAttributes.class);
        });
    }

    @TestTemplate
    public void readMultipleAttributes() throws IOException {
        MCRPath whitePng = MCRPath.getPath(DERIVATE_1, "white.png");
        Map<String, Object> attributeMap = Files.readAttributes(whitePng, "basic:size,lastModifiedTime,lastAccessTime");
        Assertions.assertEquals(3, attributeMap.size(), "There should be 3 attributes.");
        assertTrue(attributeMap.containsKey("size"));
        assertTrue(attributeMap.containsKey("lastModifiedTime"));
        assertTrue(attributeMap.containsKey("lastAccessTime"));
    }

}
