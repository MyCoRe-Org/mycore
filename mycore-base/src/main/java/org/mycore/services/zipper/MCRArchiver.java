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

package org.mycore.services.zipper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * Writes the content of derivates to an archive of a specific format.
 * This class is independent of the servlet API, so it can be used by servlets and REST resources.
 *
 * @param <T> the output stream type of the archive format
 * @see MCRZipArchiver
 * @see MCRTarArchiver
 */
public abstract class MCRArchiver<T extends ArchiveOutputStream<?>> {

    /**
     * Returns the mime type of the archive format.
     */
    public abstract String getMimeType();

    /**
     * Returns the file extension of the archive format (without a leading dot).
     */
    public abstract String getFileExtension();

    /**
     * Creates the archive stream that writes to the given stream.
     *
     * @param out the stream the archive is written to
     * @param comment a comment, if supported by the format
     */
    public abstract T createContainer(OutputStream out, String comment);

    /**
     * Adds a directory entry to the archive.
     */
    public abstract void sendDirectory(MCRPath dir, BasicFileAttributes attrs, T container) throws IOException;

    /**
     * Adds a file entry to the archive.
     */
    public abstract void sendFile(MCRPath file, BasicFileAttributes attrs, T container) throws IOException;

    /**
     * Adds an entry with the given content to the archive.
     */
    public abstract void sendMetadata(String fileName, byte[] content, long lastModified, T container)
        throws IOException;

    /**
     * Adds all directories and files below the given path (including the path itself) to the archive.
     */
    public void sendTree(MCRPath root, T container) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                sendDirectory(MCRPath.ofPath(dir), attrs, container);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                sendFile(MCRPath.ofPath(file), attrs, container);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Constructs an entry name in form of {ownerID}+'/'+{path} or {ownerID} if path is root component.
     * @param path absolute path
     */
    public static String getEntryName(MCRPath path) {
        return path.getNameCount() == 0 ? path.getOwner()
            : path.getOwner() + '/'
                + path.getRoot().relativize(path);
    }
}
