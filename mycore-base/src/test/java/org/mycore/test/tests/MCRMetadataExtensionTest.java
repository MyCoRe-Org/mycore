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

package org.mycore.test.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@ExtendWith(MCRMetadataExtension.class)
@MCRTestConfiguration(
    properties = {
        @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true")
    })
public class MCRMetadataExtensionTest {

    @Test
    public void testXMLMetadataManagerAvailable() {
        Path storeBaseDir = MCRConfiguration2.getOrThrow("MCR.Metadata.Store.BaseDir", Paths::get);
        System.out.println("Store BaseDir=" + storeBaseDir.toAbsolutePath());
        assertTrue(Files.isDirectory(storeBaseDir), "Store base dir should be a directory");
        assertTrue(storeBaseDir.getFileName().toString().startsWith("mcr-store"),
            "Store base dir should start with 'mcr-store'");
        assertFalse(MCRXMLMetadataManager.obtainInstance().exists(MCRObjectID.getInstance("MyCoRe_test_00004711")),
            "MCRXMLMetadataManager should be available but the object should not exist");
    }

    @Nested
    class MCRMetadataExtensionTest1 {
        @Test
        public void testXMLMetadataManagerAvailableInNested() {
            testXMLMetadataManagerAvailable();
        }
    }

}
