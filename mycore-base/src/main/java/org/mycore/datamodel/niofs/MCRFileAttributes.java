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

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRFileAttributes<T> implements BasicFileAttributes {
    enum FileType {
        file, directory, link, other;

        public static FileType fromAttribute(BasicFileAttributes attrs) {
            if (attrs.isRegularFile()) {
                return file;
            }
            if (attrs.isDirectory()) {
                return directory;
            }
            if (attrs.isSymbolicLink()) {
                return link;
            }
            return other;
        }
    }

    private static final FileTime EPOCHE_TIME = FileTime.fromMillis(0);

    private static final Pattern MD5_HEX_PATTERN = Pattern.compile("[a-fA-F0-9]{32}");

    FileTime lastModified, lastAccessTime, creationTime;

    long size;

    T filekey;

    String md5sum;

    FileType type;

    private MCRFileAttributes(final FileType type, final long size, final T filekey, final String md5sum,
        final FileTime creationTime, final FileTime lastModified, final FileTime lastAccessTime) {
        super();
        this.type = type;
        if (size < 0) {
            throw new IllegalArgumentException("'size' cannot be negative");
        }
        this.size = size;
        this.filekey = filekey;
        setMd5sum(type, md5sum);
        this.creationTime = creationTime == null ? lastModified == null ? EPOCHE_TIME : lastModified : creationTime;
        this.lastModified = lastModified == null ? creationTime == null ? EPOCHE_TIME : creationTime : lastModified;
        this.lastAccessTime = lastAccessTime == null ? EPOCHE_TIME : lastAccessTime;
    }

    /**
     *
     */
    public static <T> MCRFileAttributes<T> directory(final T filekey, final long size, final FileTime lastModified) {
        return new MCRFileAttributes<>(FileType.directory, size, filekey, null, null, lastModified, null);
    }

    public static <T> MCRFileAttributes<T> file(final T filekey, final long size, final String md5sum,
        final FileTime lastModified) {
        return file(filekey, size, md5sum, null, lastModified, null);
    }

    public static <T> MCRFileAttributes<T> file(final T filekey, final long size, final String md5sum,
        final FileTime creationTime, final FileTime lastModified, final FileTime lastAccessTime) {
        return new MCRFileAttributes<>(FileType.file, size, filekey, md5sum, creationTime, lastModified,
            lastAccessTime);
    }

    public static <T> MCRFileAttributes<T> fromAttributes(BasicFileAttributes attrs, String md5) {
        return new MCRFileAttributes<>(FileType.fromAttribute(attrs), attrs.size(), (T) attrs.fileKey(), md5,
            attrs.creationTime(), attrs.lastModifiedTime(), attrs.lastAccessTime());
    }

    /* (non-Javadoc)
     * @see java.nio.file.attribute.BasicFileAttributes#creationTime()
     */
    @Override
    public FileTime creationTime() {
        return creationTime;
    }

    /* (non-Javadoc)
     * @see java.nio.file.attribute.BasicFileAttributes#fileKey()
     */
    @Override
    public T fileKey() {
        return filekey;
    }

    /* (non-Javadoc)
     * @see java.nio.file.attribute.BasicFileAttributes#isDirectory()
     */
    @Override
    public boolean isDirectory() {
        return type == FileType.directory;
    }

    /* (non-Javadoc)
     * @see java.nio.file.attribute.BasicFileAttributes#isOther()
     */
    @Override
    public boolean isOther() {
        return type == FileType.other;
    }

    /* (non-Javadoc)
     * @see java.nio.file.attribute.BasicFileAttributes#isRegularFile()
     */
    @Override
    public boolean isRegularFile() {
        return type == FileType.file;
    }

    /* (non-Javadoc)
     * @see java.nio.file.attribute.BasicFileAttributes#isSymbolicLink()
     */
    @Override
    public boolean isSymbolicLink() {
        return type == FileType.link;
    }

    /* (non-Javadoc)
     * @see java.nio.file.attribute.BasicFileAttributes#lastAccessTime()
     */
    @Override
    public FileTime lastAccessTime() {
        return lastAccessTime;
    }

    /* (non-Javadoc)
     * @see java.nio.file.attribute.BasicFileAttributes#lastModifiedTime()
     */
    @Override
    public FileTime lastModifiedTime() {
        return lastModified;
    }

    public String md5sum() {
        return md5sum;
    }

    /* (non-Javadoc)
     * @see java.nio.file.attribute.BasicFileAttributes#size()
     */
    @Override
    public long size() {
        return size;
    }

    private void setMd5sum(final FileType type, final String md5sum) {
        if (type != FileType.file && md5sum == null) {
            return;
        }
        Objects.requireNonNull(md5sum, "'md5sum' is required for files");
        if (md5sum.length() != 32 || !MD5_HEX_PATTERN.matcher(md5sum).matches()) {
            throw new IllegalArgumentException("Not a valid md5sum: " + md5sum);
        }
        this.md5sum = md5sum;
    }

}
