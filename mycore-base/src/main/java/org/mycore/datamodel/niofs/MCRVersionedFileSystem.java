package org.mycore.datamodel.niofs;

/**
 * Represents a file system with versioning capabilities. This class extends {@link MCRAbstractFileSystem}
 * to work with {@link MCRVersionedFileSystemProvider}, thereby enabling handling of paths that
 * include version information.
 */
public abstract class MCRVersionedFileSystem extends MCRAbstractFileSystem {

    /**
     * Constructs a new {@code MCRVersionedFileSystem} with the specified versioned file system provider.
     *
     * @param provider The versioned file system provider to be associated with this file system.
     *                 Must not be null.
     */
    public MCRVersionedFileSystem(MCRVersionedFileSystemProvider provider) {
        super(provider);
    }

    /**
     * Returns the file system provider that created this file system and supports versioned operations.
     *
     * @return The file system provider as an instance of {@link MCRVersionedFileSystemProvider}.
     */
    @Override
    public MCRVersionedFileSystemProvider provider() {
        return (MCRVersionedFileSystemProvider) super.provider();
    }

}
