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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mycore.ocfl.MCROCFLTestCaseHelper.DERIVATE_1;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRPermutationExtension.class, MCROCFLSetupExtension.class })
public class MCROCFLRemoteVirtualObjectTest {

    protected MCROCFLRepository repository;

    private final boolean remote = true;

    @PermutedParam
    private boolean purge;

    private MCROCFLVirtualObject derivate1;

    private MCRVersionedPath path1;

    @BeforeEach
    public void setUp() throws Exception {
        MCROCFLVirtualObjectProvider virtualObjectProvider = MCROCFLFileSystemProvider.get().virtualObjectProvider();
        derivate1 = virtualObjectProvider.get(DERIVATE_1);
        path1 = MCRVersionedPath.head(DERIVATE_1, "file1");
    }

    @TestTemplate
    public void toPhysicalPath() throws IOException {
        String tempStorage = "/temp-storage/";
        String transactionalDirectory = "/transactional-storage/";

        assertTrue(derivate1.toPhysicalPath(path1).toString().contains(tempStorage),
            "should access temp store");
        MCRTransactionManager.beginTransactions();
        Assertions.assertTrue(derivate1.toPhysicalPath(path1).toString().contains(tempStorage),
            "should access temp store because the transaction store is empty");
        Files.write(path1, new byte[] { 1 });
        Assertions.assertTrue(derivate1.toPhysicalPath(path1).toString().contains(transactionalDirectory),
            "should access transactional store because a file was written there");
        MCRTransactionManager.commitTransactions();
        Assertions.assertTrue(derivate1.toPhysicalPath(path1).toString().contains(tempStorage),
            "should access temp store because there is no active transaction");
        MCRTransactionManager.beginTransactions();
        Assertions.assertTrue(derivate1.toPhysicalPath(path1).toString().contains(tempStorage),
            "should access temp store because the transaction store is empty");
        MCRTransactionManager.commitTransactions();
    }

}
