package org.mycore.ocfl.niofs;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.MCRVersionedPath;

import io.ocfl.api.model.ObjectVersionId;

public class MCROCFLFileSystemProviderTest extends MCROCFLTestCase {

    @Test
    public void checkAccess() {
        // check files
        Path whitePngPath = MCRPath.getPath(DERIVATE_1, "white.png");
        Path blackPngPath = MCRPath.getPath(DERIVATE_1, "black.png");
        Path purplePngPath = MCRPath.getPath(DERIVATE_1, "purple.png");
        Path emptyDirectoryPath = MCRPath.getPath(DERIVATE_1, "empty");
        Path keepFile = MCRPath.getPath(DERIVATE_1, "empty/.keep");
        assertTrue("'white.png' should exist", Files.exists(whitePngPath));
        assertTrue("'black.png' should exist", Files.exists(blackPngPath));
        assertTrue("'empty' directory should exists", Files.exists(emptyDirectoryPath));
        assertFalse("'purple.png' should not exist", Files.exists(purplePngPath));
        assertFalse("'empty/.keep' file should not exists", Files.exists(keepFile));

        // check writable
        assertFalse("'" + DERIVATE_2 + "' should not exists", Files.exists(MCRPath.getPath(DERIVATE_2, "")));
        MCRTransactionHelper.beginTransaction();
        assertFalse("'" + DERIVATE_2 + "' should not exists", Files.exists(MCRPath.getPath(DERIVATE_2, "")));
        MCRTransactionHelper.commitTransaction();
    }

    @Test
    public void createDirectory() throws IOException {
        // create directory in existing ocfl object
        final MCRPath path1 = MCRPath.getPath(DERIVATE_1, "test");
        Assert.assertFalse("'test' directory should not exists", Files.exists(path1));
        MCRTransactionHelper.beginTransaction();
        Files.createDirectory(path1);
        assertTrue("'test' directory should exist before committing", Files.exists(path1));
        MCRTransactionHelper.commitTransaction();
        assertTrue("'test' directory should exist after committing", Files.exists(path1));

        // create already existing directory
        MCRTransactionHelper.beginTransaction();
        assertThrows(FileAlreadyExistsException.class, () -> Files.createDirectory(path1));
        MCRTransactionHelper.commitTransaction();

        // create directory in non-existing ocfl object
        final MCRPath path2 = MCRPath.getPath(DERIVATE_2, "test");
        Assert.assertFalse("'test' directory should not exists", Files.exists(path2));
        MCRTransactionHelper.beginTransaction();
        Files.createDirectory(path2);
        assertTrue("'test' directory should exist after create", Files.exists(path2));
        MCRTransactionHelper.commitTransaction();
        assertTrue("'test' directory should exist after commit", Files.exists(path2));
    }

    @Test
    public void newByteChannel() throws IOException, URISyntaxException {
        URL whitePngURL = getClass().getResource("/junit_derivate_00000001/white.png");
        assertNotNull(whitePngURL);
        // prepare
        byte[] expectedTestFileData = { 1, 3, 3, 7 };
        byte[] expectedWhitePngData = Files.readAllBytes(Path.of(whitePngURL.toURI()));
        // write
        MCRPath testFile = MCRPath.getPath(DERIVATE_1, "testFile");
        assertFalse("'testFile' should not exists", Files.exists(testFile));
        MCRTransactionHelper.beginTransaction();
        Files.write(testFile, expectedTestFileData);
        assertTrue("'testFile' should exists after writing", Files.exists(testFile));
        MCRTransactionHelper.commitTransaction();
        assertTrue("'testFile' should exists after committing", Files.exists(testFile));

        // read
        byte[] testFileData = Files.readAllBytes(testFile);
        assertArrayEquals("byte array should be equal", expectedTestFileData, testFileData);
        try (SeekableByteChannel byteChannel = Files.newByteChannel(testFile)) {
            byte[] seekBytes = new byte[4];
            ByteBuffer byteBuffer = ByteBuffer.wrap(seekBytes);
            byteChannel.read(byteBuffer);
            assertArrayEquals("byte array should be equal", expectedTestFileData, seekBytes);
        }

        // read v1
        ObjectVersionId versionId = ObjectVersionId.version(DERIVATE_1, 1);
        MCRVersionedPath whitePng = MCRVersionedPath.getPath(
            versionId.getObjectId(), versionId.getVersionNum().toString(), "white.png");
        byte[] whitePngData = Files.readAllBytes(whitePng);
        assertArrayEquals("byte array should be equal", expectedWhitePngData, whitePngData);
    }

    @Test
    public void move() throws IOException {
        MCRTransactionHelper.beginTransaction();
        Path source = MCRPath.getPath(DERIVATE_1, "white.png");
        Path target = MCRPath.getPath(DERIVATE_1, "moved.png");
        Files.move(source, target);
        assertFalse("'white.png' should not exist", Files.exists(source));
        assertTrue("'moved.png' should exist", Files.exists(target));
        MCRTransactionHelper.commitTransaction();
        assertFalse("'white.png' should not exist after committing", Files.exists(source));
        assertTrue("'moved.png' should exist after committing", Files.exists(target));

        // move from different derivate
        Path newSource = MCRPath.getPath(DERIVATE_1, "moved.png");
        Path newTarget = MCRPath.getPath(DERIVATE_2, "white.png");
        MCRTransactionHelper.beginTransaction();
        Files.move(newSource, newTarget);
        MCRTransactionHelper.commitTransaction();
        assertFalse("'moved.png' should not exist after committing", Files.exists(newSource));
        assertTrue("'white.png' should exist after committing", Files.exists(newTarget));
    }

    @Test
    public void copy() throws IOException, URISyntaxException {
        MCRTransactionHelper.beginTransaction();
        Path whiteV1 = MCRVersionedPath.getPath(DERIVATE_1, "v1", "white.png");
        Path copiedV2 = MCRPath.getPath(DERIVATE_1, "copied.png");
        Files.copy(whiteV1, copiedV2);
        assertTrue("'white.png' should exist", Files.exists(whiteV1));
        assertTrue("'copied.png' should exist", Files.exists(copiedV2));
        MCRTransactionHelper.commitTransaction();
        assertTrue("'white.png' should exist after committing", Files.exists(whiteV1));
        assertTrue("'copied.png' should exist after committing", Files.exists(copiedV2));

        // copy from v1 to head
        Path copiedV3 = MCRVersionedPath.getPath(DERIVATE_1, "copied2.png");
        MCRTransactionHelper.beginTransaction();
        Files.copy(whiteV1, copiedV3);
        assertTrue("'white.png' should exist", Files.exists(whiteV1));
        assertTrue("'copied.png' should exist", Files.exists(copiedV2));
        assertTrue("'copied2.png' should exist", Files.exists(copiedV3));
        MCRTransactionHelper.commitTransaction();
        assertTrue("'white.png' should exist after committing", Files.exists(whiteV1));
        assertTrue("'copied.png' should exist", Files.exists(copiedV2));
        assertTrue("'copied2.png' should exist after committing", Files.exists(copiedV3));

        // copy to v1
        MCRTransactionHelper.beginTransaction();
        assertThrows("cannot copy to non head version", IOException.class, () -> {
            Files.copy(copiedV3, whiteV1);
        });
        MCRTransactionHelper.commitTransaction();

        // copy foreign source file
        MCRTransactionHelper.beginTransaction();
        URL whitePngFromDerivate2URL = getClass().getClassLoader().getResource(DERIVATE_2 + "/white.png");
        assertNotNull(whitePngFromDerivate2URL);
        Path foreignSource = Path.of(whitePngFromDerivate2URL.toURI());
        MCRPath ocflTarget = MCRPath.getPath(DERIVATE_1, "copied3.png");
        Files.copy(foreignSource, ocflTarget, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        assertTrue("'copied3.png' should exist", Files.exists(ocflTarget));
        MCRTransactionHelper.commitTransaction();
        assertTrue("'copied3.png' should exist after committing", Files.exists(ocflTarget));
    }

    @Test
    public void delete() throws IOException {
        Path whitePng = MCRPath.getPath(DERIVATE_1, "white.png");
        assertTrue("'white.png' should exist", Files.exists(whitePng));
        MCRTransactionHelper.beginTransaction();
        Files.delete(whitePng);
        assertFalse("'white.png' should not exist", Files.exists(whitePng));
        MCRTransactionHelper.commitTransaction();
        assertFalse("'white.png' should not exist", Files.exists(whitePng));
    }

    @Test
    public void newDirectoryStream() throws IOException {
        // root directory
        Path rootPath = MCRPath.getPath(DERIVATE_1, "/");
        List<Path> rootContent = List.of(
            MCRPath.getPath(DERIVATE_1, "white.png"),
            MCRPath.getPath(DERIVATE_1, "black.png"),
            MCRPath.getPath(DERIVATE_1, "empty"));
        AtomicInteger numPaths = new AtomicInteger();
        int targetPaths = rootContent.size();

        MCRTransactionHelper.beginTransaction();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(rootPath)) {
            paths.forEach(path -> {
                numPaths.getAndIncrement();
                assertTrue("'" + path + "' should be in root '/'", rootContent.contains(path));
            });
        }
        assertEquals("There should be '" + targetPaths + "' paths in directory stream.", targetPaths, numPaths.get());
        MCRTransactionHelper.commitTransaction();

        // empty directory
        Path emptyDirectory = MCRPath.getPath(DERIVATE_1, "/empty");
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(emptyDirectory)) {
            assertThrows(
                "'empty' directory should be empty",
                NoSuchElementException.class,
                () -> paths.iterator().next());
        }
    }

    @Test
    public void keepTest() throws IOException {
        Path emptyDirectory = MCRPath.getPath(DERIVATE_1, "/empty");
        Path empty2Directory = MCRPath.getPath(DERIVATE_1, "/empty2");
        MCRTransactionHelper.beginTransaction();
        assertTrue("'empty' directory should exist", Files.exists(emptyDirectory));
        assertFalse("'empty2' directory should exist", Files.exists(empty2Directory));
        Files.move(emptyDirectory, empty2Directory);
        assertFalse("'empty' directory should not exist", Files.exists(emptyDirectory));
        assertTrue("'empty2' directory should exist", Files.exists(empty2Directory));
        MCRTransactionHelper.commitTransaction();
        assertFalse("'empty' directory should not exist after committing", Files.exists(emptyDirectory));
        assertTrue("'empty2' directory should exist after committing", Files.exists(empty2Directory));
    }

    @Test
    public void fileAttributes() throws IOException, InterruptedException {
        MCRPath whitePng = MCRPath.getPath(DERIVATE_1, "white.png");
        BasicFileAttributes whitePngAttributes = Files.readAttributes(whitePng, BasicFileAttributes.class);
        assertNotNull("attributes of 'white.png' shouldn't be null", whitePngAttributes);
        assertTrue("'white.png' should be a regular file", whitePngAttributes.isRegularFile());
        assertFalse("'white.png' shouldn't be a directory", whitePngAttributes.isDirectory());

        FileTime creationTime = whitePngAttributes.creationTime();
        FileTime modifyTime = whitePngAttributes.lastModifiedTime();
        FileTime accessTime = whitePngAttributes.lastAccessTime();
        assertNotNull("creationTime() of 'white.png' should not be null", creationTime);
        assertNotNull("lastModifiedTime() of 'white.png' should not be null", modifyTime);
        assertNotNull("lastAccessTime() of 'white.png' should not be null", accessTime);

        // WRITE
        MCRTransactionHelper.beginTransaction();
        Thread.sleep(1);
        Files.write(whitePng, new byte[] { 1, 3, 3, 7 });
        MCRTransactionHelper.commitTransaction();

        whitePngAttributes = Files.readAttributes(whitePng, BasicFileAttributes.class);
        assertEquals("creationTime() of 'white.png' should be same as before",
            creationTime, whitePngAttributes.creationTime());
        assertTrue("lastModifiedTime() of 'white.png' should be later",
            whitePngAttributes.lastModifiedTime().toInstant().isAfter(modifyTime.toInstant()));

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
        assertTrue("lastAccessTime() of 'white.png' should be later",
            whitePngAttributes.lastAccessTime().toInstant().isAfter(accessTime.toInstant()));

        // DIRECTORY
        BasicFileAttributes existingIdRootAttributes = Files.readAttributes(MCRPath.getPath(DERIVATE_1, "/"),
            BasicFileAttributes.class);
        assertTrue("root directory should exist", existingIdRootAttributes.isDirectory());
        assertThrows("root directory should not exist", NoSuchFileException.class, () -> {
            Files.readAttributes(MCRPath.getPath(DERIVATE_2, "/"), BasicFileAttributes.class);
        });
    }

    @Test
    public void readMultipleAttributes() throws IOException {
        MCRPath whitePng = MCRPath.getPath(DERIVATE_1, "white.png");
        Map<String, Object> attributeMap = Files.readAttributes(whitePng, "basic:size,lastModifiedTime,lastAccessTime");
        assertEquals("There should be 3 attributes.", 3, attributeMap.size());
        assertTrue(attributeMap.containsKey("size"));
        assertTrue(attributeMap.containsKey("lastModifiedTime"));
        assertTrue(attributeMap.containsKey("lastAccessTime"));
    }

}
