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

package org.mycore.common.events;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

public class MCRPathEventHelper {

    private static void fireFileEvent(MCREvent.EventType event, Path file) throws IOException {
        BasicFileAttributes attrs = Files.getFileAttributeView(file, BasicFileAttributeView.class).readAttributes();
        fireFileEvent(event, file, attrs);
    }

    private static void fireFileEvent(MCREvent.EventType event, Path file, BasicFileAttributes attrs) {
        MCREvent fileEvent = new MCREvent(MCREvent.ObjectType.PATH, event);
        fileEvent.put(MCREvent.PATH_KEY, file);
        if (attrs != null) {
            fileEvent.put(MCREvent.FILEATTR_KEY, attrs);
        }
        MCREventManager.getInstance().handleEvent(fileEvent);
    }

    public static void fireFileCreateEvent(Path file) throws IOException {
        BasicFileAttributes attrs = Files.getFileAttributeView(file, BasicFileAttributeView.class).readAttributes();
        fireFileCreateEvent(file, attrs);
    }

    public static void fireFileCreateEvent(Path file, BasicFileAttributes attrs) {
        fireFileEvent(MCREvent.EventType.CREATE, file, Objects.requireNonNull(attrs));
    }

    public static void fireFileUpdateEvent(Path file) throws IOException {
        BasicFileAttributes attrs = Files.getFileAttributeView(file, BasicFileAttributeView.class).readAttributes();
        fireFileUpdateEvent(file, attrs);
    }

    public static void fireFileUpdateEvent(Path file, BasicFileAttributes attrs) {
        fireFileEvent(MCREvent.EventType.UPDATE, file, Objects.requireNonNull(attrs));
    }

    public static void fireFileDeleteEvent(Path file) {
        fireFileEvent(MCREvent.EventType.DELETE, file, null);
    }

    public static void fireFileMoveEvent(Path source, Path target, boolean create) throws IOException {
        fireFileEvent(create ? MCREvent.EventType.CREATE : MCREvent.EventType.UPDATE, target);
        fireFileDeleteEvent(source);
    }

}
