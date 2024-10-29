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

package org.mycore.datamodel.niofs;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

import org.mycore.common.digest.MCRDigest;

public class MCRDefaultFileAttributes<T> implements MCRFileAttributes<T> {

    private static final FileTime EPOCHE_TIME = FileTime.fromMillis(0);

    protected FileTime lastModified;

    protected FileTime lastAccessTime;

    protected FileTime creationTime;

    protected long size;

    protected T filekey;

    protected MCRDigest digest;

    protected FileType type;

    public MCRDefaultFileAttributes(final FileType type, final long size, final T filekey, final MCRDigest digest,
        final FileTime creationTime, final FileTime lastModified, final FileTime lastAccessTime) {
        this.type = type;
        if (size < 0) {
            throw new IllegalArgumentException("'size' cannot be negative");
        }
        this.size = size;
        this.filekey = filekey;
        setDigest(type, digest);
        this.creationTime = creationTime == null ? lastModified == null ? EPOCHE_TIME : lastModified : creationTime;
        this.lastModified = lastModified == null ? creationTime == null ? EPOCHE_TIME : creationTime : lastModified;
        this.lastAccessTime = lastAccessTime == null ? EPOCHE_TIME : lastAccessTime;
    }

    public static <T> MCRDefaultFileAttributes<T> directory(final T filekey, final long size,
        final FileTime lastModified) {
        return new MCRDefaultFileAttributes<>(FileType.directory, size, filekey, null, null, lastModified, null);
    }

    public static <T> MCRDefaultFileAttributes<T> file(final T filekey, final long size, final MCRDigest digest,
        final FileTime lastModified) {
        return file(filekey, size, digest, null, lastModified, null);
    }

    public static <T> MCRDefaultFileAttributes<T> file(final T filekey, final long size, final MCRDigest digest,
        final FileTime creationTime, final FileTime lastModified, final FileTime lastAccessTime) {
        return new MCRDefaultFileAttributes<>(FileType.file, size, filekey, digest, creationTime, lastModified,
            lastAccessTime);
    }

    public static MCRFileAttributes fromAttributes(BasicFileAttributes attrs, MCRDigest digest) {
        return new MCRDefaultFileAttributes<>(FileType.fromAttribute(attrs), attrs.size(), attrs.fileKey(), digest,
            attrs.creationTime(), attrs.lastModifiedTime(), attrs.lastAccessTime());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileTime creationTime() {
        return creationTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T fileKey() {
        return filekey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() {
        return type == FileType.directory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOther() {
        return type == FileType.other;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegularFile() {
        return type == FileType.file;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSymbolicLink() {
        return type == FileType.link;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileTime lastAccessTime() {
        return lastAccessTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileTime lastModifiedTime() {
        return lastModified;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long size() {
        return size;
    }

    @Override
    public MCRDigest digest() {
        return digest;
    }

    private void setDigest(final FileType type, final MCRDigest digest) {
        if (type != FileType.file && digest == null) {
            return;
        }
        Objects.requireNonNull(digest, "'digest' is required for files");
        this.digest = digest;
    }

}
