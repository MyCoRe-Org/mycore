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

package org.mycore.datamodel.niofs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public abstract class MCRPathUtils {

    private MCRPathUtils() {

    }

    /**
     * Returns requested {@link BasicFileAttributes} or null if file does not exist.
     * 
     * Same as {@link Files#readAttributes(Path, Class, LinkOption...)} without throwing {@link IOException}.
     * 
     * @param   path
     *          the path to the file
     * @param   type
     *          the {@code Class} of the file attributes required
     *          to read
     * @param   options
     *          options indicating how symbolic links are handled
     *
     * @return  the file attributes
     */
    public static <A extends BasicFileAttributes> A getAttributes(Path path, Class<A> type, LinkOption... options) {
        try {
            return Files.readAttributes(path, type, options);
        } catch (NoSuchFileException | FileNotFoundException e) {
            //we expect that file may not exist
        } catch (IOException e) {
            //any other IOException is catched
            LogManager.getLogger(MCRPathUtils.class).info("Error while retrieving attributes of file: {}", path, e);
        }
        return null;
    }

    public static Path getPath(FileSystem targetFS, String fileName) {
        String[] nameComps = fileName.replace('\\', '/').split("/");
        if (nameComps.length == 1) {
            return targetFS.getPath(nameComps[0]);
        } else {
            return targetFS.getPath(nameComps[0], Arrays.copyOfRange(nameComps, 1, nameComps.length));
        }

    }

    /**
     * Returns the size of the path.
     *
     * If the path is a directory the size returned is the sum of all files found recursivly
     * in this directory.
     * @param p path of a file or directory
     * @return the size of p in bytes
     * @throws IOException underlaying IOException
     */
    public static long getSize(Path p) throws IOException {
        AtomicLong size = new AtomicLong();
        Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                size.addAndGet(attrs.size());
                return super.visitFile(file, attrs);
            }
        });
        return size.get();
    }

}
