/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.datamodel.niofs.ifs2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.common.events.MCREventManager;
import org.mycore.common.events.MCRPathEventHelper;
import org.mycore.datamodel.ifs2.MCRStoreManager;
import org.mycore.datamodel.niofs.MCRAbstractFileSystem;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.test.MyCoReTest;

@MCRTestConfiguration(
    properties = {
        @MCRTestProperty(key = "MCR.Metadata.Type.derivate", string = "true")
    })
@MyCoReTest
public class MCRFileSystemEventTest {

    private EventRegister register;

    @TempDir
    public Path storeFolder;

    @TempDir
    public Path exportFolder;

    private Path derivateRoot;

    @BeforeEach
    public void setUp() throws Exception {
        register = new EventRegister();
        MCREventManager.getInstance().addEventHandler(MCREvent.ObjectType.PATH, register);
        MCRConfiguration2.set("MCR.IFS2.Store.IFS2_junit_derivate.BaseDir", storeFolder.toAbsolutePath().toString());
        derivateRoot = Paths.get(URI.create("ifs2:/junit_derivate_00000001:/"));
    }

    @AfterEach
    public void tearDown() throws Exception {
        MCREventManager.getInstance().removeEventHandler(MCREvent.ObjectType.PATH, register);
        register.clear();
        MCRStoreManager.removeStore("IFS2_junit_derivate");
    }

    private long countEvents(MCREvent.EventType type) {
        return register.getEntries().stream()
            .map(EventRegisterEntry::getEventType)
            .filter(type::equals)
            .count();
    }

    @Test
    public void testRegister() throws IOException {
        final Path tmpFile = File.createTempFile(this.getClass().getName(), ".test").toPath();
        tmpFile.toFile().deleteOnExit();
        final BasicFileAttributes attributes = Files.readAttributes(tmpFile, BasicFileAttributes.class);
        assertTrue(register.getEntries().isEmpty());
        MCRPathEventHelper.fireFileCreateEvent(tmpFile, attributes);
        assertEquals(1, register.getEntries().size());
        register.getEntries().forEach(System.out::println);
        register.clear();
        Files.delete(tmpFile);
    }

    @Test
    public void testFiles() throws IOException {
        Path file = derivateRoot.resolve("File.txt");
        assertTrue(register.getEntries().isEmpty());
        Files.createFile(file);
        assertEquals(1, register.getEntries().size());
        assertEquals(1, countEvents(MCREvent.EventType.CREATE));
        register.clear();
        Files.writeString(file, "Hello World!", StandardCharsets.UTF_8);
        assertEquals(1, register.getEntries().size());
        assertEquals(1, countEvents(MCREvent.EventType.UPDATE));
        register.clear();
        Files.delete(file);
        assertEquals(1, register.getEntries().size());
        assertEquals(1, countEvents(MCREvent.EventType.DELETE));
        register.clear();
        Files.writeString(file, "Hello World!", StandardCharsets.UTF_8);
        assertEquals(1, register.getEntries().size());
        assertEquals(1, countEvents(MCREvent.EventType.CREATE));
        register.clear();
        final Path newFile = file.getParent().resolve("File.old");
        Files.move(file, newFile);
        //register.getEntries().forEach(System.out::println);
        assertEquals(2, register.getEntries().size());
        assertEquals(1, countEvents(MCREvent.EventType.CREATE));
        assertEquals(1, countEvents(MCREvent.EventType.DELETE));
        register.clear();
        final byte[] bytes = Files.readAllBytes(newFile);
        assertTrue(register.getEntries().isEmpty());
        assertEquals("Hello World!", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testDirectoryStream() throws IOException {
        Path dir1 = derivateRoot.resolve("dir1");
        Path dir2 = derivateRoot.resolve("dir2");
        Path file = dir1.resolve("File.txt");
        final MCRAbstractFileSystem fileSystem = (MCRAbstractFileSystem) derivateRoot.getFileSystem();
        fileSystem.createRoot(MCRPath.ofPath(derivateRoot).getOwner());
        Files.createDirectory(dir1);
        Files.createDirectory(dir2);
        assertTrue(register.getEntries().isEmpty());
        Files.writeString(file, "Hello World!", StandardCharsets.UTF_8);
        assertEquals(1, register.getEntries().size());
        assertEquals(1, countEvents(MCREvent.EventType.CREATE));
        register.clear();
        try (DirectoryStream<Path> dir1Stream = Files.newDirectoryStream(dir1);
            DirectoryStream<Path> dir2Stream = Files.newDirectoryStream(dir2)) {
            if (!(dir1Stream instanceof SecureDirectoryStream<Path> sDir1Stream)) {
                LogManager.getLogger().warn("Current OS ({}) does not provide SecureDirectoryStream.",
                    System.getProperty("os.name"));
                return;
            }
            //further testing
            SecureDirectoryStream<Path> sDir2Stream = (SecureDirectoryStream<Path>) dir2Stream;
            //relative -> relative
            sDir1Stream.move(file.getFileName(), sDir2Stream, file.getFileName());
            assertEquals(2, register.getEntries().size());
            assertEquals(1, countEvents(MCREvent.EventType.CREATE));
            assertEquals(1, countEvents(MCREvent.EventType.DELETE));
            sDir2Stream.move(file.getFileName(), sDir1Stream, file.getFileName());
            register.clear();
            //absolute -> relative
            sDir1Stream.move(file, sDir2Stream, file.getFileName());
            assertEquals(2, register.getEntries().size());
            assertEquals(1, countEvents(MCREvent.EventType.CREATE));
            assertEquals(1, countEvents(MCREvent.EventType.DELETE));
            sDir2Stream.move(file.getFileName(), sDir1Stream, file.getFileName());
            register.clear();
            //relative -> absolute
            sDir1Stream.move(file.getFileName(), sDir2Stream, dir2.resolve(file.getFileName()));
            assertEquals(2, register.getEntries().size());
            assertEquals(1, countEvents(MCREvent.EventType.CREATE));
            assertEquals(1, countEvents(MCREvent.EventType.DELETE));
            sDir2Stream.move(file.getFileName(), sDir1Stream, file.getFileName());
            register.clear();
            //absolute -> absolute
            sDir1Stream.move(file, sDir2Stream, dir2.resolve(file.getFileName()));
            assertEquals(2, register.getEntries().size());
            assertEquals(1, countEvents(MCREvent.EventType.CREATE));
            assertEquals(1, countEvents(MCREvent.EventType.DELETE));
            sDir2Stream.move(file.getFileName(), sDir1Stream, file.getFileName());
            register.clear();
            //rename
            sDir1Stream.move(file.getFileName(), sDir1Stream, dir1.resolve("Junit.txt").getFileName());
            assertEquals(2, register.getEntries().size());
            assertEquals(1, countEvents(MCREvent.EventType.CREATE));
            assertEquals(1, countEvents(MCREvent.EventType.DELETE));
            sDir1Stream.move(dir1.resolve("Junit.txt").getFileName(), sDir1Stream, file.getFileName());
            register.clear();
            //move to local dir
            final Path exportDir = exportFolder;
            try (DirectoryStream<Path> exportDirStream = Files.newDirectoryStream(exportDir)) {
                if (exportDirStream instanceof SecureDirectoryStream<Path> sExportDirStream) {
                    final Path localFilePath = MCRFileSystemUtils.toNativePath(exportDir.getFileSystem(),
                        file.getFileName());
                    sDir1Stream.move(file.getFileName(), sExportDirStream, localFilePath);
                    assertEquals(1, register.getEntries().size());
                    assertEquals(1, countEvents(MCREvent.EventType.DELETE));
                    register.clear();
                    try (
                        SeekableByteChannel targetChannel = sDir1Stream.newByteChannel(file.getFileName(), Set.of(
                            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE));
                        FileInputStream fis = new FileInputStream(exportDir.resolve(localFilePath).toFile());
                        FileChannel inChannel = fis.getChannel()) {
                        long bytesTransferred = 0;
                        while (bytesTransferred < inChannel.size()) {
                            bytesTransferred += inChannel.transferTo(bytesTransferred, inChannel.size(), targetChannel);
                        }
                    }
                    assertEquals(1, register.getEntries().size());
                    assertEquals(1, countEvents(MCREvent.EventType.CREATE));
                    register.clear();
                }
            }
        }
    }

    private static class EventRegister extends MCREventHandlerBase {
        private List<EventRegisterEntry> entries;

        EventRegister() {
            entries = new ArrayList<>();
        }

        @Override
        protected void handlePathUpdated(MCREvent evt, Path path, BasicFileAttributes attrs) {
            addEntry(evt, path, attrs);
        }

        @Override
        protected void handlePathDeleted(MCREvent evt, Path path, BasicFileAttributes attrs) {
            addEntry(evt, path, attrs);
        }

        @Override
        protected void handlePathCreated(MCREvent evt, Path path, BasicFileAttributes attrs) {
            addEntry(evt, path, attrs);
        }

        private void addEntry(MCREvent evt, Path path, BasicFileAttributes attrs) {
            entries.add(new MCRFileSystemEventTest.EventRegisterEntry(evt.getEventType(), path, attrs));
        }

        void clear() {
            entries.clear();
        }

        List<EventRegisterEntry> getEntries() {
            return entries;
        }
    }

    private static class EventRegisterEntry {

        private Instant time;

        private MCREvent.EventType eventType;

        private Path path;

        private BasicFileAttributes attrs;

        private final StackTraceElement[] stackTrace;

        public EventRegisterEntry(MCREvent.EventType eventType, Path path, BasicFileAttributes attrs) {
            this.time = Instant.now();
            this.eventType = eventType;
            this.path = path;
            this.attrs = attrs;
            this.stackTrace = Thread.currentThread().getStackTrace();
        }

        public Instant getTime() {
            return time;
        }

        public MCREvent.EventType getEventType() {
            return eventType;
        }

        public Path getPath() {
            return path;
        }

        public BasicFileAttributes getAttrs() {
            return attrs;
        }

        @Override
        public String toString() {
            return "EventRegisterEntry{" +
                "time=" + time +
                ", eventType='" + eventType + '\'' +
                ", path=" + path +
                ", attrs=" + attrs +
                '}';

        }

        public String getStackTraceAsString() {
            return Stream.of(stackTrace)
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n", "\n", ""));
        }
    }
}
