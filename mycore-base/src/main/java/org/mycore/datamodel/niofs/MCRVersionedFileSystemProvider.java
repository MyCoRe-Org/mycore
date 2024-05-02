package org.mycore.datamodel.niofs;

import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;

public abstract class MCRVersionedFileSystemProvider extends MCRAbstractFileSystemProvider {

    @Override
    public Path getPath(final URI uri) {
        PathInformation pathInfo = getPathInformation(uri);
        return getPath(pathInfo.owner(), pathInfo.path(), getFileSystem());
    }

    @Override
    public MCRVersionedPath getPath(String owner, String path, MCRAbstractFileSystem fs) {
        Objects.requireNonNull(fs, MCRAbstractFileSystem.class.getSimpleName() + " instance may not be null.");
        return new MCRVersionedPath(owner, path) {
            @Override
            public MCRVersionedFileSystem getFileSystem() {
                return (MCRVersionedFileSystem) fs;
            }
        };
    }

    public abstract String getHeadVersion(String owner);

    public boolean isHeadVersion(String owner, String version) {
        return version == null || version.equals(getHeadVersion(owner));
    }

}
