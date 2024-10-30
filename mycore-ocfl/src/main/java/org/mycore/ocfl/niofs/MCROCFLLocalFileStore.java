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

import org.mycore.common.MCRException;
import org.mycore.datamodel.niofs.MCRAbstractFileStore;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.MCRVersionedPath;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

/**
 * A local file store implementation for the OCFL file system.
 * <p>
 * This class extends {@link MCRAbstractFileStore} to provide a custom file store for OCFL-managed files.
 * It integrates with the OCFL file system provider to offer file store operations such as retrieving
 * the total, usable, and unallocated space, and supports file attribute views.
 * </p>
 */
public class MCROCFLLocalFileStore extends MCRAbstractFileStore {

    private final MCROCFLFileSystemProvider fileSystemProvider;

    /**
     * Constructs a new {@code MCROCFLLocalFileStore} with the specified file system provider.
     *
     * @param fileSystemProvider the OCFL file system provider.
     */
    public MCROCFLLocalFileStore(MCROCFLFileSystemProvider fileSystemProvider) {
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
     */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalSpace() throws IOException {
        return Math.max(
            repositoryFileStore().getTotalSpace(),
            localStorageFileStore().getTotalSpace()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUsableSpace() throws IOException {
        return Math.max(
            repositoryFileStore().getUsableSpace(),
            localStorageFileStore().getUsableSpace()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUnallocatedSpace() throws IOException {
        return Math.max(
            repositoryFileStore().getUnallocatedSpace(),
            localStorageFileStore().getUnallocatedSpace()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        try {
            return localStorageFileStore().supportsFileAttributeView(type);
        } catch (IOException ioException) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsFileAttributeView(String name) {
        try {
            return localStorageFileStore().supportsFileAttributeView(name);
        } catch (IOException ioException) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
        try {
            return localStorageFileStore().getFileStoreAttributeView(type);
        } catch (IOException ioException) {
            throw new MCRException("Unable to get fileStoreAttributeView for class type '" + type + "'", ioException);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getAttribute(String attribute) throws IOException {
        return localStorageFileStore().getAttribute(attribute);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getBaseDirectory() {
        return this.fileSystemProvider.localStorage().getRoot();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getPhysicalPath(MCRPath path) {
        return this.fileSystemProvider.localStorage().toPhysicalPath(MCRVersionedPath.toVersionedPath(path));
    }

    /**
     * Returns the file store for the OCFL repository root.
     *
     * @return the file store for the repository root.
     * @throws IOException if an I/O error occurs.
     */
    private FileStore repositoryFileStore() throws IOException {
        return Files.getFileStore(this.fileSystemProvider.getRepositoryProvider().getRepositoryRoot());
    }

    /**
     * Returns the file store for the local storage root.
     *
     * @return the file store for the local storage root.
     * @throws IOException if an I/O error occurs.
     */
    private FileStore localStorageFileStore() throws IOException {
        return Files.getFileStore(this.fileSystemProvider.localStorage().getRoot());
    }

}
