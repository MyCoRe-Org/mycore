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

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Objects;
import java.util.Set;

/**
 * An abstract file system provider implementation that supports versioned file paths.
 * This class provides a framework for implementing file system operations with
 * additional support for versioning, allowing file operations to interact with different
 * versions of files transparently.
 *
 * <p>All file operations such as creating, copying, moving, deleting, and checking access are
 * overridden to handle {@link MCRVersionedPath} objects which incorporate version information.</p>
 */
public abstract class MCRVersionedFileSystemProvider extends MCRAbstractFileSystemProvider {

    @Override
    public MCRVersionedPath getPath(final URI uri) {
        PathInformation pathInfo = getPathInformation(uri);
        return getPath(pathInfo.owner(), pathInfo.path());
    }

    @Override
    public MCRVersionedPath getPath(String owner, String path) {
        return new MCRVersionedPath(owner, path, getFileSystem()) {
        };
    }

    @Override
    public abstract MCRVersionedFileSystem getFileSystem();

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
        throws IOException {
        MCRVersionedPath versionedPath = MCRVersionedPath.toVersionedPath(path);
        return newByteChannel(versionedPath, options, attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path path, DirectoryStream.Filter<? super Path> filter)
        throws IOException {
        MCRVersionedPath versionedPath = MCRVersionedPath.toVersionedPath(path);
        return newDirectoryStream(versionedPath, filter);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        MCRVersionedPath versionedPath = MCRVersionedPath.toVersionedPath(dir);
        createDirectory(versionedPath, attrs);
    }

    @Override
    public void delete(Path path) throws IOException {
        MCRVersionedPath versionedPath = MCRVersionedPath.toVersionedPath(path);
        delete(versionedPath);
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        MCRVersionedPath versionedSource = MCRVersionedPath.toVersionedPath(source);
        MCRVersionedPath versionedTarget = MCRVersionedPath.toVersionedPath(target);
        copy(versionedSource, versionedTarget, options);
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        MCRVersionedPath versionedSource = MCRVersionedPath.toVersionedPath(source);
        MCRVersionedPath versionedTarget = MCRVersionedPath.toVersionedPath(target);
        move(versionedSource, versionedTarget, options);
    }

    @Override
    public boolean isSameFile(Path path1, Path path2) {
        if (path1 == null || path2 == null) {
            return false;
        }
        MCRVersionedPath versionedPath1 = MCRVersionedPath.toVersionedPath(path1);
        MCRVersionedPath versionedPath2 = MCRVersionedPath.toVersionedPath(path2);
        return Objects.equals(versionedPath1, versionedPath2);
    }

    @Override
    public boolean isHidden(Path path) {
        return false;
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        MCRVersionedPath versionedPath = MCRVersionedPath.toVersionedPath(path);
        return getFileStore(versionedPath);
    }

    @Override
    public void checkAccess(Path path, AccessMode... accessModes) throws IOException {
        MCRVersionedPath versionedPath = MCRVersionedPath.toVersionedPath(path);
        checkAccess(versionedPath, accessModes);
    }

    /**
     * Opens or creates a file, returning a seekable byte channel to access the file.
     * This method works in exactly the manner specified by the
     * {@link java.nio.file.Files#newByteChannel(Path, Set, FileAttribute[])} method.
     *
     * @param path the path of the file to open or create
     * @param options options specifying how the file is opened
     * @param attrs an optional list of file attributes to set atomically when creating the file
     * @return a new seekable byte channel
     * @throws IOException If an I/O error occurs
     * @throws MCRReadOnlyIOException If the path is read-only
     */
    public abstract SeekableByteChannel newByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options,
        FileAttribute<?>... attrs)
        throws IOException, MCRReadOnlyIOException;

    /**
     * Opens a directory, returning a DirectoryStream to iterate over the entries in the directory. This method works
     * in exactly the manner specified by the
     * {@link java.nio.file.Files#newDirectoryStream(Path, DirectoryStream.Filter)} method.
     * 
     * @param path the path to the directory
     * @param filter the directory stream filter
     * @return a new and open DirectoryStream object
     * @throws IOException  if an I/O error occurs
     */
    public abstract DirectoryStream<Path> newDirectoryStream(MCRVersionedPath path,
        DirectoryStream.Filter<? super Path> filter)
        throws IOException;

    /**
     * Creates a new directory. This method works in exactly the manner specified by the
     * {@link java.nio.file.Files#createDirectory(Path, FileAttribute[])} method.
     *
     * @param dir the directory to create
     * @param attrs an optional list of file attributes to set atomically when creating the directory
     * @throws IOException If an I/O error occurs or the directory cannot be created
     * @throws MCRReadOnlyIOException If the directory path is read-only
     */
    public abstract void createDirectory(MCRVersionedPath dir, FileAttribute<?>... attrs)
        throws IOException, MCRReadOnlyIOException;

    /**
     * Deletes a file or directory. This method works in exactly the manner specified by the
     * {@link java.nio.file.Files#delete(Path)} method.
     *
     * @param path the path to the file or directory to delete
     * @throws IOException If an I/O error occurs
     * @throws MCRReadOnlyIOException If the path is read-only
     */
    public abstract void delete(MCRVersionedPath path) throws IOException, MCRReadOnlyIOException;

    /**
     * Copy a file to a target file. This method works in exactly the manner specified by the
     * {@link java.nio.file.Files#copy(Path, Path, CopyOption...)} method except that both the source and target paths
     * must be associated with this provider.
     *
     * @param source the path to the source file
     * @param target the path to the target file
     * @param options options specifying how the copy is performed
     * @throws IOException if an I/O error occurs
     * @throws MCRReadOnlyIOException if the target path is read-only
     */
    public abstract void copy(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options)
        throws IOException, MCRReadOnlyIOException;

    /**
     * Move or rename a file to a target file. This method works in exactly the manner specified by the
     * {@link java.nio.file.Files#move(Path, Path, CopyOption...)} method except that both the source and target paths
     * must be associated with this provider.
     *
     * @param source the path to the source file
     * @param target the path to the target file
     * @param options options specifying how the copy is performed
     * @throws IOException if an I/O error occurs
     * @throws MCRReadOnlyIOException if the target path is read-only
     */
    public abstract void move(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options)
        throws IOException, MCRReadOnlyIOException;

    /**
     * Returns the FileStore representing the file store where a file is located. This method works in exactly the
     * manner specified by the {@link java.nio.file.Files#getFileStore(Path)} method.
     *
     * @param path the path to the file
     * @return the file store where the file is stored
     * @throws IOException if an I/O error occurs
     */
    public abstract FileStore getFileStore(MCRVersionedPath path) throws IOException;

    /**
     * Checks access against a path. This method works in exactly the manner specified by
     * {@link java.nio.file.spi.FileSystemProvider#checkAccess(Path, AccessMode...)}.
     *
     * @param path the path to the file to check
     * @param accessModes The access modes to check; may have zero elements
     * @throws IOException if an I/O error occurs
     */
    public abstract void checkAccess(MCRVersionedPath path, AccessMode... accessModes) throws IOException;

    /**
     * Retrieves the latest version identifier for a specified owner. This can return null if
     * the head has no version yet.
     *
     * @param owner The owner whose head version is being queried.
     * @return The latest version for the given owner.
     */
    public abstract String getHeadVersion(String owner);

    /**
     * Determines if the specified version is the head (latest) version for the given owner.
     *
     * @param owner The owner whose version is to be checked.
     * @param version The version to check against the head version.
     * @return true if the specified version is the latest; false otherwise.
     */
    public boolean isHeadVersion(String owner, String version) {
        return version == null || version.equals(getHeadVersion(owner));
    }

}
