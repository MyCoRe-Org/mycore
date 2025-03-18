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

import org.mycore.datamodel.niofs.MCRAbstractFileStore;

/**
 * A remote file store implementation for the OCFL file system.
 * <p>
 * This class extends {@link MCRAbstractFileStore} to provide a custom file store for OCFL-managed files.
 * It integrates with the OCFL file system provider to offer file store operations such as retrieving
 * the total, usable, and unallocated space, and supports file attribute views.
 * <p>
 * Be aware that for remote repositories {@link #getTotalSpace()}, {@link #getUsableSpace()} and
 * {@link #getUnallocatedSpace()} only returns the space of the local temporary folder. The underlying remote
 * repository is NOT queried!
 */
public class MCROCFLRemoteFileStore extends MCROCFLFileStore {

    /**
     * Constructs a new {@code MCROCFLRemoteFileStore} with the specified file system provider.
     *
     * @param fileSystemProvider the OCFL file system provider.
     */
    public MCROCFLRemoteFileStore(MCROCFLFileSystemProvider fileSystemProvider) {
        super(fileSystemProvider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalSpace() throws IOException {
        return localStorageFileStore().getTotalSpace();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUsableSpace() throws IOException {
        return localStorageFileStore().getUsableSpace();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUnallocatedSpace() throws IOException {
        return localStorageFileStore().getUnallocatedSpace();
    }

}
