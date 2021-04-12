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

package org.mycore.backend.jpa;

import org.mycore.common.MCRException;

/**
 * This AutoClosable can be used to begin and commit
 * a JPA transaction within the current thread.
 * 
 * Multiple MCRJPATransactionWrapper may be nested. 
 * Only the outermost wrapper will handle the transaction.
 * Inner wrappers will keep the transaction open and untouched.
 * 
 * @author Robert Stephan
 */
public class MCRJPATransactionWrapper implements AutoCloseable {

    //true, if this transaction wrapper is responsible for committing the transaction
    private boolean responsibleForTransaction = false;

    public MCRJPATransactionWrapper() {
        if (!MCRJPAUtil.isTransactionActive()) {
            responsibleForTransaction = true;
            MCRJPAUtil.beginTransaction();
        }
    }

    @Override
    public void close() throws MCRException {
        if (responsibleForTransaction) {
            MCRJPAUtil.commitTransaction();
            responsibleForTransaction = false;
        }
    }

}
