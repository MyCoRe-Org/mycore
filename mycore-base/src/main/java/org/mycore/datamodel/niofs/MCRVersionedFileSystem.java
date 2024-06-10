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
