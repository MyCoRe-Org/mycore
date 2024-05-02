package org.mycore.datamodel.niofs;

import static org.mycore.datamodel.niofs.MCRAbstractFileSystem.SEPARATOR_STRING;

import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.util.Objects;

public abstract class MCRVersionedPath extends MCRPath {

    public final static String OWNER_VERSION_SEPARATOR = "@";

    private final OwnerVersion ownerVersion;

    MCRVersionedPath(final String root, final String path) {
        this(OwnerVersion.of(root), path);
    }

    MCRVersionedPath(final OwnerVersion ownerVersion, final String path) {
        super(ownerVersion.owner, path);
        this.ownerVersion = ownerVersion;
    }

    @Override
    public String getOwner() {
        return ownerVersion.owner();
    }

    public String getVersion() {
        return ownerVersion.version();
    }

    @Override
    public MCRVersionedPath getParent() {
        MCRPath parent = super.getParent();
        return parent != null ? toVersionedPath(parent) : null;
    }

    @Override
    public MCRVersionedPath subpath(int beginIndex, int endIndex) {
        return toVersionedPath(super.subpath(beginIndex, endIndex));
    }

    @Override
    public MCRVersionedPath subpathComplete() {
        return toVersionedPath(super.subpathComplete());
    }

    @Override
    public MCRVersionedPath getName(int index) {
        return toVersionedPath(super.getName(index));
    }

    @Override
    protected MCRVersionedPath getPath(String owner, String path, MCRAbstractFileSystem fs) {
        return toVersionedPath(super.getPath(owner, path, fs));
    }

    public String toRelativePath() {
        return this.getOwnerRelativePath().substring(1);
    }

    public static MCRVersionedPath getPath(String owner, String path, String version) {
        String root = toRoot(owner, version);
        return toVersionedPath(MCRPaths.getPath(root, path));
    }

    private static String toRoot(String owner, String version) {
        return owner + (version != null ? (OWNER_VERSION_SEPARATOR + version) : "");
    }

    @Override
    public abstract MCRVersionedFileSystem getFileSystem();

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MCRVersionedPath that)) {
            return false;
        }
        if (!getFileSystem().equals(that.getFileSystem())) {
            return false;
        }
        if (!this.path.equals(that.path)) {
            return false;
        }
        if (!this.getOwner().equals(that.getOwner())) {
            return false;
        }
        return this.hasSameVersion(that);
    }

    public boolean hasSameVersion(MCRVersionedPath other) {
        String v1 = this.getVersion();
        String v2 = other.getVersion();
        if (v1 == null && v2 == null) {
            return true;
        }
        v1 = v1 != null ? v1 : this.getHeadVersion();
        v2 = v2 != null ? v2 : other.getHeadVersion();
        return Objects.equals(v1, v2);
    }

    @Override
    public String toString() {
        return this.ownerVersion.toString() + (this.path.isEmpty() ? SEPARATOR_STRING : this.path);
    }

    public boolean isHeadVersion() {
        if (this.ownerVersion.version == null) {
            return true;
        }
        return getHeadVersion().equals(this.ownerVersion.version);
    }

    protected String getHeadVersion() {
        return getFileSystem().provider().getHeadVersion(this.getOwner());
    }

    public static MCRVersionedPath resolveVersion(MCRVersionedPath path) {
        if (path.ownerVersion.version != null) {
            return path;
        }
        return MCRVersionedPath.getPath(path.getOwner(), path.path, path.getHeadVersion());
    }

    public static MCRVersionedPath toVersionedPath(final Path other) {
        Objects.requireNonNull(other);
        if (!(other instanceof MCRVersionedPath)) {
            throw new ProviderMismatchException("other is not an instance of MCRPath: " + other.getClass());
        }
        return (MCRVersionedPath) other;
    }

    public static MCRVersionedPath head(String owner, String path) {
        return MCRVersionedPath.getPath(owner, path, (String) null);
    }

    private record OwnerVersion(String owner, String version) {

        public static OwnerVersion of(String root) {
            if (root == null) {
                return new OwnerVersion("", null);
            }
            int separatorIndex = root.indexOf(OWNER_VERSION_SEPARATOR);
            if (separatorIndex == -1) {
                return new OwnerVersion(root, null);
            }
            String owner = root.substring(0, separatorIndex);
            String version = root.substring(separatorIndex + 1);
            return new OwnerVersion(owner, version);
        }

        @Override
        public String toString() {
            if (this.owner.isEmpty()) {
                return "";
            }
            return this.version != null ? this.owner + OWNER_VERSION_SEPARATOR + this.version + ":" : this.owner + ":";
        }

    }

}
