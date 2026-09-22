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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRException;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.common.MCRDefaultXMLMetadataManager;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.test.MCRAlwaysFailingXMLMetadataManager;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MCRTestHelper;
import org.mycore.test.MyCoReTest;

/**
 * Verifies that the metadata store is unusable unless {@link MCRMetadataExtension} has set it up.
 * <p>
 * Without that extension, a test would write into a directory shared by every test of the same fork and would
 * leave a store behind in the JVM-wide {@link org.mycore.datamodel.ifs2.MCRStoreCenter}, which makes an unrelated
 * test class fail for some test execution orders. The guard turns that into an error in the test that causes it.
 */
@MyCoReTest
@MCRTestConfiguration(
    properties = {
        @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true")
    })
public class MCRMetadataStoreGuardTest {

    private static MCRObjectID testObjectId() {
        // not a constant: MCRObjectID validates against MCR.Metadata.Type.test, which is only set once the
        // test configuration has been applied
        return MCRObjectID.getInstance("MyCoRe_test_00004711");
    }

    @Test
    public void testMetadataStoreIsDisabledWithoutExtension() {
        assertInstanceOf(MCRAlwaysFailingXMLMetadataManager.class, MCRXMLMetadataManager.obtainInstance(),
            "Without " + MCRMetadataExtension.class.getSimpleName() + " the metadata store must be disabled");
    }

    @Test
    public void testUsingTheMetadataStoreWithoutExtensionFails() {
        MCRException exception = assertThrows(MCRException.class,
            () -> MCRXMLMetadataManager.obtainInstance().exists(testObjectId()));
        assertTrue(exception.getMessage().contains(MCRMetadataExtension.class.getSimpleName()),
            "The error should name the extension to add, but was: " + exception.getMessage());
    }

    /**
     * A test class may opt out deliberately, the way {@code mycore-ocfl} does in its test properties.
     */
    @Nested
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = MCRTestHelper.METADATA_MANAGER_CLASS_PROPERTY,
            classNameOf = MarkerMetadataManager.class)
    })
    class WithAnnotatedManagerAndNoExtension {
        @Test
        public void testAnnotatedManagerWins() {
            assertInstanceOf(MarkerMetadataManager.class, MCRXMLMetadataManager.obtainInstance(),
                "An annotated metadata manager must take precedence over the guard");
        }
    }

    @Nested
    @ExtendWith(MCRJPAExtension.class)
    @ExtendWith(MCRMetadataExtension.class)
    class WithMetadataExtension {
        @Test
        public void testMetadataStoreIsEnabled() {
            assertInstanceOf(MCRDefaultXMLMetadataManager.class, MCRXMLMetadataManager.obtainInstance(),
                MCRMetadataExtension.class.getSimpleName() + " must enable the metadata manager of mycore.properties");
            MCRXMLMetadataManager.obtainInstance().exists(testObjectId());
        }

        @Test
        public void testExtensionRestoresTheConfiguredManager() {
            assertEquals(MCRConfiguration2.getStringOrThrow(MCRTestHelper.METADATA_MANAGER_CLASS_PROPERTY),
                MCRXMLMetadataManager.obtainInstance().getClass().getName(),
                "The extension must restore the configured manager rather than one of its own choosing");
        }
    }

    /**
     * The annotation wins over the extension too, although class properties are applied after annotated ones.
     */
    @Nested
    @ExtendWith(MCRJPAExtension.class)
    @ExtendWith(MCRMetadataExtension.class)
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = MCRTestHelper.METADATA_MANAGER_CLASS_PROPERTY,
            classNameOf = MarkerMetadataManager.class)
    })
    class WithAnnotatedManagerAndExtension {
        @Test
        public void testAnnotatedManagerWinsOverExtension() {
            assertInstanceOf(MarkerMetadataManager.class, MCRXMLMetadataManager.obtainInstance(),
                "An annotated metadata manager must take precedence over " + MCRMetadataExtension.class
                    .getSimpleName());
        }

        @Test
        @MCRTestConfiguration(properties = {
            @MCRTestProperty(key = MCRTestHelper.METADATA_MANAGER_CLASS_PROPERTY,
                classNameOf = MCRDefaultXMLMetadataManager.class)
        })
        public void testAnnotationOnTheMethodWinsOverTheOneOnTheClass() {
            assertInstanceOf(MCRDefaultXMLMetadataManager.class, MCRXMLMetadataManager.obtainInstance(),
                "A metadata manager annotated on the test method must take precedence over the one on the class");
        }
    }

    /** Behaves like the configured manager, but is identifiable by its class. */
    public static class MarkerMetadataManager extends MCRDefaultXMLMetadataManager {
    }

}
