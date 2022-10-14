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

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;

class MCRPathEventHelper {

    private static void fireFileEvent(MCREvent.EventType event, Path file, BasicFileAttributes attrs) {
        MCREvent fileEvent = new MCREvent(MCREvent.ObjectType.PATH, event);
        fileEvent.put(MCREvent.PATH_KEY, file);
        if (attrs != null) {
            fileEvent.put(MCREvent.FILEATTR_KEY, attrs);
        }
        MCREventManager.instance().handleEvent(fileEvent);
    }

    static void fireFileCreateEvent(Path file, BasicFileAttributes attrs) {
        fireFileEvent(MCREvent.EventType.CREATE, file, Objects.requireNonNull(attrs));
    }

    static void fireFileUpdateEvent(Path file, BasicFileAttributes attrs) {
        fireFileEvent(MCREvent.EventType.UPDATE, file, Objects.requireNonNull(attrs));
    }

    static void fireFileDeleteEvent(Path file) {
        fireFileEvent(MCREvent.EventType.DELETE, file, null);
    }

}
