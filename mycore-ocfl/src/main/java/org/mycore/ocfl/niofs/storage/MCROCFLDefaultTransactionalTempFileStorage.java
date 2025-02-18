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

package org.mycore.ocfl.niofs.storage;

import java.nio.file.Path;
import java.util.Objects;

import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.niofs.MCROCFLFileSystemTransaction;
import org.mycore.ocfl.niofs.MCROCFLInactiveTransactionException;

/**
 * Implementation of {@link MCROCFLTransactionalTempFileStorage} that provides default
 * transactional temporary file storage functionality.
 */
public class MCROCFLDefaultTransactionalTempFileStorage implements MCROCFLTransactionalTempFileStorage {

    private Path root;

    @MCRProperty(name = "Path")
    public String rootPathProperty;

    @SuppressWarnings("unused")
    public MCROCFLDefaultTransactionalTempFileStorage() {
        // default constructor for MCRConfiguration2 instantiation
    }

    public MCROCFLDefaultTransactionalTempFileStorage(Path root) {
        this.root = root;
    }

    @MCRPostConstruction
    public void init() {
        this.root = Path.of(rootPathProperty);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getRoot() {
        return root;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(MCRVersionedPath path) {
        try {
            return MCROCFLTransactionalTempFileStorage.super.exists(path);
        } catch (MCROCFLInactiveTransactionException inactiveTransactionException) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path toPhysicalPath(String owner, String version) {
        if (!MCROCFLFileSystemTransaction.isActive()) {
            throw new MCROCFLInactiveTransactionException("Transaction is not active!");
        }
        Long transactionId = MCROCFLFileSystemTransaction.getTransactionId();
        return toPhysicalPath(transactionId, owner, version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path toPhysicalPath(MCRVersionedPath path) throws MCROCFLInactiveTransactionException {
        if (!MCROCFLFileSystemTransaction.isActive()) {
            throw new MCROCFLInactiveTransactionException("Transaction is not active!");
        }
        Long transactionId = MCROCFLFileSystemTransaction.getTransactionId();
        return toPhysicalPath(transactionId, path);
    }

    /**
     * Converts the specified versioned path to a physical path within the context of the specified transaction.
     *
     * @param transactionId the transaction id.
     * @param path the versioned path to convert.
     * @return the physical path corresponding to the versioned path within the transaction.
     */
    private Path toPhysicalPath(Long transactionId, MCRVersionedPath path) {
        String owner = path.getOwner();
        String version = resolveVersion(path);
        String relativePath = path.getOwnerRelativePath().substring(1);
        return toPhysicalPath(transactionId, owner, version).resolve(relativePath);
    }

    /**
     * Converts the specified owner and version to a physical path within the context of the specified transaction.
     *
     * @param transactionId the transaction id.
     * @param owner the owner of the path.
     * @param version the version of the path.
     * @return the physical path corresponding to the owner and version within the transaction.
     */
    public Path toPhysicalPath(Long transactionId, String owner, String version) {
        Objects.requireNonNull(transactionId);
        Objects.requireNonNull(owner);
        Path transactionOwnerPath = toPhysicalPath(transactionId).resolve(owner);
        return transactionOwnerPath.resolve(version != null ? version : firstVersionFolder());
    }

}
