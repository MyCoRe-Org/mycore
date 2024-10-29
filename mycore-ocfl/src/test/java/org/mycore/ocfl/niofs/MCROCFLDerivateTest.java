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

import org.junit.Test;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.ocfl.MCROCFLMetadataTestCase;
import org.mycore.ocfl.MCROCFLTestCaseHelper;

public class MCROCFLDerivateTest extends MCROCFLMetadataTestCase {

    @Override
    public void tearDown() throws Exception {
        MCROCFLFileSystemProvider.get().clearCache();
        if (MCRTransactionManager.isActive(MCROCFLFileSystemTransaction.class)) {
            MCRTransactionManager.rollbackTransactions(MCROCFLFileSystemTransaction.class);
        }
        MCROCFLFileSystemTransaction.resetTransactionCounter();
        super.tearDown();
    }

    @Test
    public void create() throws Exception {
        MCRObject object = createObject("junit_object_00000001");
        MCRDerivate derivate = createDerivate(object.getId().toString(), "junit_derivate_00000001");
        MCRMetadataManager.create(object);

        MCRTransactionManager.requireTransactions(MCROCFLFileSystemTransaction.class);
        MCRMetadataManager.create(derivate);
        MCROCFLTestCaseHelper.loadDerivate(derivate.getId().toString());
        MCRTransactionManager.commitTransactions(MCROCFLFileSystemTransaction.class);
    }

}
