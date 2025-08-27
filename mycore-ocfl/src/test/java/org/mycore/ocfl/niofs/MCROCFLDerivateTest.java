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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.MCROCFLTestCaseHelper;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.PermutedParam;
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
public class MCROCFLDerivateTest {

    protected MCROCFLRepository repository;

    @PermutedParam
    private boolean remote;

    @PermutedParam
    private boolean purge;

    @TestTemplate
    public void create() throws Exception {
        MCRDerivate derivate =
            MCROCFLTestCaseHelper.loadObjectAndDerivate("junit_object_00000001", "junit_derivate_00000001");
        assertTrue(MCRMetadataManager.exists(derivate.getOwnerID()), "object should exist");
        assertTrue(MCRMetadataManager.exists(derivate.getId()), "derivate should exist");
        try (Stream<Path> list = Files.list(MCRVersionedPath.head(derivate.getId().toString(), "/"))) {
            assertEquals(3, list.count(), "there should be 3 directory entries");
        }
    }

}
