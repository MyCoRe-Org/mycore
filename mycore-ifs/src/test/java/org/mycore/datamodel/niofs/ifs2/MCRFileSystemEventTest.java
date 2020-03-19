/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mycore.common.MCRTestCase;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.ifs2.MCRStoreManager;
import org.mycore.datamodel.niofs.MCRAbstractFileSystem;
import org.mycore.datamodel.niofs.MCRPath;

public class MCRFileSystemEventTest extends MCRTestCase {

    private EventRegister register;

    @Rule
    public TemporaryFolder storeFolder = new TemporaryFolder();

    private Path derivateRoot;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        register = new EventRegister();
        MCREventManager.instance().addEventHandler(MCREvent.PATH_TYPE, register);
        derivateRoot = Paths.get(URI.create("ifs2:/junit_derivate_00000001:/"));
    }

    @Override
    public void tearDown() throws Exception {
        MCREventManager.instance().removeEventHandler(MCREvent.PATH_TYPE, register);
        register.clear();
        MCRStoreManager.removeStore("IFS2_junit_derivate");
        super.tearDown();
    }

    @Override
    protected Map<String, String> getTestProperties() {
        final Map<String, String> map = super.getTestProperties();
        map.put("MCR.Metadata.Type.derivate", "true");
        map.put("MCR.IFS2.Store.IFS2_junit_derivate.BaseDir", storeFolder.getRoot().getAbsolutePath());
        return map;
    }

    @Override
    protected boolean isDebugEnabled() {
        return true;
    }

    private long countEvents(String type) {
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
        Assert.assertTrue(register.getEntries().isEmpty());
        MCRPathEventHelper.fireFileCreateEvent(tmpFile, attributes);
        Assert.assertEquals(1, register.getEntries().size());
        register.getEntries().forEach(System.out::println);
        register.clear();
        Files.delete(tmpFile);
    }

    @Test
    public void testFiles() throws IOException {
        Path file = derivateRoot.resolve("File.txt");
        Assert.assertTrue(register.getEntries().isEmpty());
        Files.createFile(file);
        Assert.assertEquals(1, register.getEntries().size());
        Assert.assertEquals(1, countEvents(MCREvent.CREATE_EVENT));
        register.clear();
        Files.writeString(file, "Hello World!", StandardCharsets.UTF_8);
        Assert.assertEquals(1, register.getEntries().size());
        Assert.assertEquals(1, countEvents(MCREvent.UPDATE_EVENT));
        register.clear();
        Files.delete(file);
        Assert.assertEquals(1, register.getEntries().size());
        Assert.assertEquals(1, countEvents(MCREvent.DELETE_EVENT));
        register.clear();
        Files.writeString(file, "Hello World!", StandardCharsets.UTF_8);
        Assert.assertEquals(1, register.getEntries().size());
        Assert.assertEquals(1, countEvents(MCREvent.CREATE_EVENT));
        register.clear();
        final Path newFile = file.getParent().resolve("File.old");
        Files.move(file, newFile);
        //register.getEntries().forEach(System.out::println);
        Assert.assertEquals(2, register.getEntries().size());
        Assert.assertEquals(1, countEvents(MCREvent.CREATE_EVENT));
        Assert.assertEquals(1, countEvents(MCREvent.DELETE_EVENT));
        register.clear();
        final byte[] bytes = Files.readAllBytes(newFile);
        Assert.assertTrue(register.getEntries().isEmpty());
        Assert.assertEquals("Hello World!", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testDirectoryStream() throws IOException {
        Path dir1 = derivateRoot.resolve("dir1");
        Path dir2 = derivateRoot.resolve("dir2");
        Path file = dir1.resolve("File.txt");
        final MCRAbstractFileSystem fileSystem = (MCRAbstractFileSystem) derivateRoot.getFileSystem();
        fileSystem.createRoot(MCRPath.toMCRPath(derivateRoot).getOwner());
        Files.createDirectory(dir1);
        Files.createDirectory(dir2);
        Assert.assertTrue(register.getEntries().isEmpty());
        Files.writeString(file, "Hello World!", StandardCharsets.UTF_8);
        Assert.assertEquals(1, register.getEntries().size());
        Assert.assertEquals(1, countEvents(MCREvent.CREATE_EVENT));
        register.clear();
    }

    private static class EventRegister extends MCREventHandlerBase {
        private List<EventRegisterEntry> entries;

        EventRegister() {
            entries = new LinkedList<>();
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

        private String eventType;

        private Path path;

        private BasicFileAttributes attrs;

        private final StackTraceElement[] stackTrace;

        public EventRegisterEntry(String eventType, Path path, BasicFileAttributes attrs) {
            this.time = Instant.now();
            this.eventType = eventType;
            this.path = path;
            this.attrs = attrs;
            this.stackTrace = Thread.currentThread().getStackTrace();
        }

        public Instant getTime() {
            return time;
        }

        public String getEventType() {
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
