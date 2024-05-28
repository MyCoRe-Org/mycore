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

import static org.mycore.datamodel.niofs.MCRAbstractFileSystem.SEPARATOR_STRING;

import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.util.Objects;

/**
 * Represents a file system path that includes versioning information, extending the capabilities
 * of {@link MCRPath}.
 *
 * <p>Instances of this class are immutable and safe for use by multiple concurrent threads.</p>
 */
public abstract class MCRVersionedPath extends MCRPath {

    public final static String OWNER_VERSION_SEPARATOR = "@";

    private final OwnerVersion ownerVersion;

    MCRVersionedPath(final String root, final String path, MCRVersionedFileSystem fileSystem) {
        this(OwnerVersion.of(root), path, fileSystem);
    }

    MCRVersionedPath(final String owner, final String version, final String path, MCRVersionedFileSystem fileSystem) {
        this(new OwnerVersion(owner, version), path, fileSystem);
    }

    private MCRVersionedPath(final OwnerVersion ownerVersion, final String path, MCRVersionedFileSystem fileSystem) {
        super(ownerVersion.owner, path, fileSystem);
        this.ownerVersion = ownerVersion;
    }

    public static MCRVersionedPath getPath(String owner, String version, String path) {
        Path resolved = MCRPaths.getVersionedPath(owner, version, path);
        return toVersionedPath(resolved);
    }

    public static MCRVersionedPath head(String owner, String path) {
        return getPath(owner, null, path);
    }

    /**
     * Retrieves the owner part of the path.
     *
     * @return A string representing the owner of the path.
     */
    @Override
    public String getOwner() {
        return ownerVersion.owner();
    }

    /**
     * Retrieves the version part of the path, which may be {@code null} if not specified.
     *
     * @return A string representing the version of the path or {@code null} if no version is specified.
     */
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

    private static String toRoot(String owner, String version) {
        return owner + (version != null ? (OWNER_VERSION_SEPARATOR + version) : "");
    }

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

    /**
     * Checks if the version parts are equal.
     * 
     * @param other the other path
     * @return true if the version parts are equal, otherwise false
     */
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

    /**
     * Determines if the path is pointing to the head version, typically the latest or current version.
     *
     * @return {@code true} if the path is associated with the head version; {@code false} otherwise.
     */
    public boolean isHeadVersion() {
        if (this.ownerVersion.version == null) {
            return true;
        }
        return getHeadVersion().equals(this.ownerVersion.version);
    }

    /**
     * Retrieves the head version from the file system provider associated with this path.
     *
     * @return A string representing the head version.
     */
    protected String getHeadVersion() {
        return getFileSystem().provider().getHeadVersion(this.getOwner());
    }

    @Override
    public MCRVersionedFileSystem getFileSystem() {
        return (MCRVersionedFileSystem) this.fileSystem;
    }

    /**
     * Resolves the version of the given path if possible.
     *
     * @param path the path to resolve
     * @return new {@link MCRVersionedPath} instance with the resolved version
     */
    public static MCRVersionedPath resolveVersion(MCRVersionedPath path) {
        if (path.ownerVersion.version != null) {
            return path;
        }
        return path.getFileSystem().provider().getPath(path.getOwner(), path.getHeadVersion(), path.path);
    }

    /**
     * Cast the other path to a {@link MCRVersionedPath}, ensuring that the path is not null.
     *
     * @param other The path to convert.
     * @return The converted versioned path.
     * @throws ProviderMismatchException If the path is not an instance of {@link MCRVersionedPath}.
     */
    public static MCRVersionedPath toVersionedPath(final Path other) {
        Objects.requireNonNull(other);
        if (!(other instanceof MCRVersionedPath)) {
            throw new ProviderMismatchException("other is not an instance of MCRVersionedPath: " + other.getClass());
        }
        return (MCRVersionedPath) other;
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
