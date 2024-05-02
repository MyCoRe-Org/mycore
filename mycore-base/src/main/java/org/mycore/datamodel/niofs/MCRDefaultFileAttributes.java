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

    /* (non-Javadoc)
     * @see java.nio.file.attribute.BasicFileAttributes#size()
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
