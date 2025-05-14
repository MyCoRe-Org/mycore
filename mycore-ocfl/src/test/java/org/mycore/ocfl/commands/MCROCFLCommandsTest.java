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

package org.mycore.ocfl.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.MCROCFLTestCaseHelper;
import org.mycore.ocfl.niofs.MCROCFLFileSystemTransaction;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.ocfl.util.MCROCFLObjectIDPrefixHelper;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRJPAExtension.class, MCRPermutationExtension.class, MCROCFLSetupExtension.class })
@MCROCFLSetupExtension.LoadDefaultDerivate(false)
@MCRTestConfiguration(
    properties = {
        // set jpa mappings, only use mycore-base
        @MCRTestProperty(key = "MCR.JPA.MappingFileNames", string = "META-INF/mycore-base-mappings.xml"),
        // use ocfl metadata
        @MCRTestProperty(key = "MCR.Metadata.Type.object", string = "true"),
        @MCRTestProperty(key = "MCR.Metadata.Type.derivate", string = "true"),
        // set free access
        @MCRTestProperty(key = "MCR.Access.Class", string = "org.mycore.access.MCRAccessBaseImpl"),
        @MCRTestProperty(key = "MCR.Access.Strategy.Class", string = "org.mycore.ocfl.test.AlwaysTrueStrategy")
    })
public class MCROCFLCommandsTest {

    public static final String JUNIT_DERIVATE_00000001 = "junit_derivate_00000001";

    protected MCROCFLRepository repository;

    @PermutedParam
    private boolean remote;

    private final boolean purge = false;

    @TestTemplate
    public void restoreDerivate() throws Exception {
        // prepare
        MCRDerivate derivate = createDerivate();
        deleteDerivate(derivate);
        MCRObjectID derivateId = MCRObjectID.getInstance(JUNIT_DERIVATE_00000001);
        assertFalse(MCRMetadataManager.exists(derivateId), "derivate should not exist");

        // restore
        MCROCFLCommands.restoreDerivateFromOCFL(JUNIT_DERIVATE_00000001, "v2");

        // test
        assertTrue(MCRMetadataManager.exists(derivateId), "derivate should exist");
        try(Stream<Path> directoryStream = Files.list(MCRVersionedPath.head(JUNIT_DERIVATE_00000001, "/"))) {
            assertEquals(3, directoryStream.toList().size());
        }
        MCRObject mcrObject = MCRMetadataManager.retrieveMCRObject(derivate.getOwnerID());
        assertTrue(mcrObject.getStructure().containsDerivate(derivateId));
    }

    @TestTemplate
    public void describeObject() throws MCRAccessException, URISyntaxException, IOException {
        MCRDerivate derivate = createDerivate();
        MCROCFLCommands.describeObject(MCROCFLObjectIDPrefixHelper.toDerivateObjectId(derivate.getId().toString()),
            repository.getId());
    }

    private static MCRDerivate createDerivate() throws MCRAccessException, URISyntaxException, IOException {
        MCRObject object = MCROCFLTestCaseHelper.createObject("junit_object_00000001");
        MCRDerivate derivate =
            MCROCFLTestCaseHelper.createDerivate(object.getId().toString(), JUNIT_DERIVATE_00000001);
        MCRMetadataManager.create(object);

        MCRTransactionManager.requireTransactions(MCROCFLFileSystemTransaction.class);
        MCRMetadataManager.create(derivate);
        MCROCFLTestCaseHelper.loadDerivate(derivate.getId().toString());
        MCRTransactionManager.commitTransactions(MCROCFLFileSystemTransaction.class);

        return derivate;
    }

    private static void deleteDerivate(MCRDerivate derivate) throws MCRAccessException {
        MCRTransactionManager.requireTransactions(MCROCFLFileSystemTransaction.class);
        MCRMetadataManager.delete(derivate);
        MCRTransactionManager.commitTransactions(MCROCFLFileSystemTransaction.class);
    }

}
