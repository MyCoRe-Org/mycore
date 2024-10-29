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

package org.mycore.ocfl.niofs.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;

/**
 * Interface for temporary file storage with transaction support, extending the
 * {@link MCROCFLTempFileStorage} interface. Provides additional methods for handling
 * transactional file operations.
 */
public interface MCROCFLTransactionalTempFileStorage extends MCROCFLTempFileStorage {

    /**
     * Purges all files associated with the specified transaction.
     *
     * @param transactionId the transaction id
     * @throws IOException if an I/O error occurs during the purge.
     */
    default void purge(Long transactionId) throws IOException {
        FileUtils.deleteDirectory(toPhysicalPath(transactionId).toFile());
    }

    /**
     * Converts the specified transaction to a physical path.
     *
     * @param transactionId the transaction id
     * @return the physical path corresponding to the transaction.
     */
    default Path toPhysicalPath(Long transactionId) {
        return this.getRoot().resolve(transactionId.toString());
    }

    /**
     * Clears the transactional directory.
     *
     * @throws IOException if an I/O error occurs during the purge.
     */
    default void clearTransactional() throws IOException {
        Path root = getRoot();
        if (Files.isDirectory(root)) {
            FileUtils.cleanDirectory(root.toFile());
        }
    }

}
