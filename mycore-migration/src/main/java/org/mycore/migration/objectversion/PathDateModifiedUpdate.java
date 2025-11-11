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

package org.mycore.migration.objectversion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Date;

/**
 * A record that holds a Path and a Date to update the last modified time of the file at the given path.
 *
 * @param path         the path to the file
 * @param dateModified the new last modified date
 */
record PathDateModifiedUpdate(Path path, Date dateModified) {

    /**
     * Update the last modified time of the file at the given path to the specified dateModified.
     *
     * @throws IOException if an I/O error occurs
     */
    void update() throws IOException {
        Files.setLastModifiedTime(path, FileTime.fromMillis(dateModified.getTime()));
    }
}
