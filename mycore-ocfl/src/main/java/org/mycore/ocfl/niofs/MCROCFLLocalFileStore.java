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

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;

import org.mycore.datamodel.niofs.MCRAbstractFileStore;
import org.mycore.ocfl.MCROCFLException;
import org.mycore.ocfl.repository.MCROCFLLocalRepositoryProvider;

/**
 * A local file store implementation for the OCFL file system.
 * <p>
 * This class extends {@link MCRAbstractFileStore} to provide a custom file store for OCFL-managed files.
 * It integrates with the OCFL file system provider to offer file store operations such as retrieving
 * the total, usable, and unallocated space, and supports file attribute views.
 */
public class MCROCFLLocalFileStore extends MCROCFLFileStore {

    /**
     * Constructs a new {@code MCROCFLLocalFileStore} with the specified file system provider.
     *
     * @param fileSystemProvider the OCFL file system provider.
     */
    public MCROCFLLocalFileStore(MCROCFLFileSystemProvider fileSystemProvider) {
        super(fileSystemProvider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalSpace() throws IOException {
        return Math.min(
            repositoryFileStore().getTotalSpace(),
            localStorageFileStore().getTotalSpace());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUsableSpace() throws IOException {
        return Math.min(
            repositoryFileStore().getUsableSpace(),
            localStorageFileStore().getUsableSpace());
    }

    /**
     * Returns the file store for the OCFL repository root.
     *
     * @return the file store for the repository root.
     * @throws IOException if an I/O error occurs.
     */
    private FileStore repositoryFileStore() throws IOException {
        if (this.fileSystemProvider
            .getRepositoryProvider() instanceof MCROCFLLocalRepositoryProvider localRepositoryProvider) {
            return Files.getFileStore(localRepositoryProvider.getRepositoryRoot());
        }
        throw new MCROCFLException("Excepted instance of MCROCFLLocalRepositoryProvider, but got "
            + this.fileSystemProvider.getRepositoryProvider().getClass().getSimpleName());
    }

}
