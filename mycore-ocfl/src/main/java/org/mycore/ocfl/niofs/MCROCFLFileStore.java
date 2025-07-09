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

package org.mycore.ocfl.niofs;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Objects;

import org.mycore.datamodel.niofs.MCRAbstractFileStore;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * The base FileStore implementation for the OCFL file system.
 * <p>
 * This class extends {@link MCRAbstractFileStore} to provide a custom file store for OCFL-managed files.
 * <p>
 * The retrieval of the total, usable, and unallocated space is not supported!
 */
public class MCROCFLFileStore extends MCRAbstractFileStore {

    protected final MCROCFLFileSystemProvider fileSystemProvider;

    /**
     * Constructs a new {@code MCROCFLFileStore} with the specified file system provider.
     *
     * @param fileSystemProvider the OCFL file system provider.
     */
    public MCROCFLFileStore(MCROCFLFileSystemProvider fileSystemProvider) {
        this.fileSystemProvider = fileSystemProvider;
    }

    /**
     * Returns the name of this file store, which is the repository ID of the file system provider.
     *
     * @return the name of this file store.
     */
    @Override
    public String name() {
        return this.fileSystemProvider.getRepositoryId();
    }

    /**
     * Returns the type of this file store, which is the canonical name of the repository class.
     *
     * @return the type of this file store.
     */
    @Override
    public String type() {
        return this.fileSystemProvider.getRepository().getClass().getCanonicalName();
    }

    /**
     * {@inheritDoc}
     * <p>
     * An OCFL repository is never read-only.
     */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is not supported in an OCFL repository.
     */
    @Override
    public long getUsableSpace() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is not supported in an OCFL repository.
     */
    @Override
    public long getUnallocatedSpace() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is not supported in an OCFL repository.
     */
    @Override
    public long getTotalSpace() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        return BasicFileAttributeView.class.isAssignableFrom(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsFileAttributeView(String name) {
        return name.equals(MCROCFLFileSystemProvider.BASIC_ATTRIBUTE_VIEW)
            || name.equals(MCROCFLFileSystemProvider.OCFL_ATTRIBUTE_VIEW);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
        Objects.requireNonNull(type, "type");
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * OCFL repository does not support any attributes.
     */
    @Override
    public Object getAttribute(String attribute) {
        throw new UnsupportedOperationException("'" + attribute + "' not supported!");
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is not supported in an OCFL repository.
     */
    @Override
    public Path getBaseDirectory() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is not supported in an OCFL repository.
     */
    @Override
    public Path getPhysicalPath(MCRPath path) {
        throw new UnsupportedOperationException();
    }

}
