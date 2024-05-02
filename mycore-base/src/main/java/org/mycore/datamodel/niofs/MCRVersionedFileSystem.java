package org.mycore.datamodel.niofs;

public abstract class MCRVersionedFileSystem extends MCRAbstractFileSystem {

    public MCRVersionedFileSystem(MCRVersionedFileSystemProvider provider) {
        super(provider);
    }

    @Override
    public MCRVersionedFileSystemProvider provider() {
        return (MCRVersionedFileSystemProvider) super.provider();
    }

}
