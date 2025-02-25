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

package org.mycore.ocfl.niofs.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.mycore.datamodel.niofs.MCRVersionedPath;

/**
 * Interface for temporary file storage with support for versioned paths.
 * Provides default methods for common file operations such as checking existence,
 * creating byte channels, copying, moving, deleting files, and more.
 */
public interface MCROCFLTempFileStorage {

    /**
     * Gets the root path of the storage.
     *
     * @return the root path.
     */
    Path getRoot();

    /**
     * Checks if the specified path exists.
     *
     * @param path the versioned path to check.
     * @return {@code true} if the path exists, {@code false} otherwise.
     */
    default boolean exists(MCRVersionedPath path) {
        return Files.exists(toPhysicalPath(path));
    }

    /**
     * Creates a new byte channel for the specified path with the given options and attributes.
     *
     * @param path the versioned path for which to create the byte channel.
     * @param options the options specifying how the file is opened.
     * @param fileAttributes an optional list of file attributes to set atomically when creating the file.
     * @return a new seekable byte channel.
     * @throws IOException if an I/O error occurs.
     */
    default SeekableByteChannel newByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options,
        FileAttribute<?>... fileAttributes) throws IOException {
        Path fileSystemPath = toPhysicalPath(path);
        boolean read = options.isEmpty() || options.contains(StandardOpenOption.READ);
        if (!read) {
            Files.createDirectories(fileSystemPath.getParent());
        }
        return Files.newByteChannel(fileSystemPath, options, fileAttributes);
    }

    /**
     * Copies the content from an input stream to the specified target path.
     *
     * @param is the input stream to read from.
     * @param target the target versioned path.
     * @param options options specifying how the copy should be done.
     * @throws IOException if an I/O error occurs.
     */
    default void copy(InputStream is, MCRVersionedPath target, CopyOption... options) throws IOException {
        Path targetSystemFile = toPhysicalPath(target);
        Files.createDirectories(targetSystemFile.getParent());
        Files.copy(is, targetSystemFile, options);
    }

    /**
     * Copies a file from a source path to a target path.
     *
     * @param source the source versioned path.
     * @param target the target versioned path.
     * @param options options specifying how the copy should be done.
     * @throws IOException if an I/O error occurs.
     */
    default void copy(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        Path sourceSystemFile = toPhysicalPath(source);
        Path targetSystemFile = toPhysicalPath(target);
        Files.createDirectories(targetSystemFile.getParent());
        Files.copy(sourceSystemFile, targetSystemFile, options);
    }

    /**
     * Moves a file from a source path to a target path.
     *
     * @param source the source versioned path.
     * @param target the target versioned path.
     * @param options options specifying how the move should be done.
     * @throws IOException if an I/O error occurs.
     */
    default void move(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        Path targetSystemFile = toPhysicalPath(target);
        Files.createDirectories(targetSystemFile.getParent());
        Files.move(toPhysicalPath(source), toPhysicalPath(target), options);
    }

    /**
     * Deletes the specified path if it exists.
     *
     * @param path the versioned path to delete.
     * @throws IOException if an I/O error occurs.
     */
    default void deleteIfExists(MCRVersionedPath path) throws IOException {
        Files.deleteIfExists(toPhysicalPath(path));
    }

    /**
     * Creates directories for the specified directory path.
     *
     * @param directoryPath the versioned path for which to create directories.
     * @param attrs an optional list of file attributes to set atomically when creating the directories.
     * @throws IOException if an I/O error occurs.
     */
    default void createDirectories(MCRVersionedPath directoryPath, FileAttribute<?>... attrs) throws IOException {
        Files.createDirectories(toPhysicalPath(directoryPath), attrs);
    }

    /**
     * Returns the size of a file (in bytes).
     * 
     * @param path the path to the file
     * @return the size of a file in bytes
     * @throws IOException if an I/O error occurs.
     */
    default long size(MCRVersionedPath path) throws IOException {
        // TODO implement
        return Files.size(toPhysicalPath(path));
    }

    /**
     * Reads a file's attributes as a bulk operation.
     *
     * @param path the path to the file
     * @param type the Class of the file attributes required to read
     * @param options options indicating how symbolic links are handled
     * @return the file attributes
     * @throws IOException if an I/O error occurs.
     */
    default <A extends BasicFileAttributes> A readAttributes(MCRVersionedPath path, Class<A> type,
        LinkOption... options) throws IOException {
        return Files.readAttributes(toPhysicalPath(path), type, options);
    }

    /**
     * Clears the storage by deleting all files and directories under the root path.
     *
     * @throws IOException if an I/O error occurs.
     */
    default void clear() throws IOException {
        FileUtils.deleteDirectory(getRoot().toFile());
    }

    /**
     * Creates a directory for the specified owner and version.
     *
     * @param owner the owner of the directory.
     * @param version the version of the directory.
     * @throws IOException if an I/O error occurs.
     */
    default void create(String owner, String version) throws IOException {
        Files.createDirectories(toPhysicalPath(owner, version));
    }

    /**
     * Converts the specified owner and version to a physical path.
     * <p>
     * If called externally, use the returned path only for READ operations!
     *
     * @param owner the owner.
     * @param version the version.
     * @return the physical path.
     */
    Path toPhysicalPath(String owner, String version);

    /**
     * Converts the specified versioned path to a physical path.
     * <p>
     * If called externally, use the returned path only for READ operations!
     *
     * @param path the versioned path.
     * @return the physical path.
     */
    default Path toPhysicalPath(MCRVersionedPath path) {
        MCRVersionedPath versionedPath = MCRVersionedPath.resolveVersion(path);
        String owner = versionedPath.getOwner();
        String version = resolveVersion(path);
        String relativePath = versionedPath.toRelativePath();
        return toPhysicalPath(owner, version).resolve(relativePath);
    }

    default String resolveVersion(MCRVersionedPath path) {
        MCRVersionedPath resolvedVersion = MCRVersionedPath.resolveVersion(path);
        return resolvedVersion.getVersion() != null ? resolvedVersion.getVersion() : firstVersionFolder();
    }

    default String firstVersionFolder() {
        return "v0";
    }

}
