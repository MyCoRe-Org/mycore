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

package org.mycore.ocfl.niofs;

import java.io.IOException;
import java.nio.file.attribute.FileTime;

import org.mycore.common.digest.MCRDigest;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRVersionedPath;

/**
 * Implementation of {@link MCRFileAttributes} that provides file attributes for a file stored in an OCFL repository.
 * <p>
 * This class wraps the file metadata and attributes such as creation time, last modified time, access time,
 * file size, and digest, which are retrieved from the {@link MCROCFLVirtualObject}.
 * </p>
 * <p>
 * This class is typically used in conjunction with {@link MCRVersionedPath} to retrieve the attributes of
 * a specific versioned file or directory within the OCFL storage system.
 * </p>
 *
 * @see MCRFileAttributes
 * @see MCROCFLVirtualObject
 * @see MCRVersionedPath
 */
public class MCROCFLFileAttributes implements MCRFileAttributes<Object> {

    private final FileTime creationTime;

    private final FileTime modifiedTime;

    private final FileTime accessTime;

    private final boolean file;

    private final boolean directory;

    private final long size;

    private final MCRDigest digest;

    private final Object fileKey;

    /**
     * Constructs an instance of {@code MCROCFLFileAttributes} by extracting the necessary attributes
     * from the given {@link MCROCFLVirtualObject} and the specified {@link MCRVersionedPath}.
     *
     * @param virtualObject the OCFL virtual object from which the file attributes are retrieved.
     * @param path the versioned path representing the file or directory.
     * @throws IOException if there is an issue retrieving the file attributes from the virtual object.
     */
    public MCROCFLFileAttributes(MCROCFLVirtualObject virtualObject, MCRVersionedPath path) throws IOException {
        this.creationTime = virtualObject.getCreationTime(path);
        this.modifiedTime = virtualObject.getModifiedTime(path);
        this.accessTime = virtualObject.getAccessTime(path);
        this.file = virtualObject.isFile(path);
        this.directory = virtualObject.isDirectory(path);
        this.size = virtualObject.getSize(path);
        this.digest = virtualObject.getDigest(path);
        this.fileKey = virtualObject.getFileKey(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileTime creationTime() {
        return this.creationTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileTime lastModifiedTime() {
        return this.modifiedTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileTime lastAccessTime() {
        return this.accessTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegularFile() {
        return this.file;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() {
        return this.directory;
    }

    /**
     * {@inheritDoc}
     * <p>Returns {@code false} as this implementation does not support symbolic links.</p>
     */
    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>Returns {@code false} as this implementation does not support other file types.</p>
     */
    @Override
    public boolean isOther() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long size() {
        return this.size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object fileKey() {
        return this.fileKey;
    }

    /**
     * {@inheritDoc}
     * <p>Returns the digest (checksum) of the file content as a {@link MCRDigest} object.</p>
     */
    @Override
    public MCRDigest digest() {
        return this.digest;
    }

}
