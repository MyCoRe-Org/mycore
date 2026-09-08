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

package org.mycore.common.xsl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRLegacyXSLCheckTest {

    private static final String LAYOUT_PROPERTY = "MCR.LayoutService.TransformerFactory";

    private static final String CONTENT_PROPERTY = "MCR.ContentTransformer.stylesheets-yed.TransformerFactory";

    private static final String CUSTOM_PROPERTY = "MCR.Test.Custom.TransformerFactory";

    @Test
    public void acceptsDefaultsAndIsRegisteredFirst() {
        assertEquals(MCRLegacyXSLCheck.class.getName(),
            MCRConfiguration2.getStringOrThrow("MCR.Startup.Class").split(",")[0]);
        assertDoesNotThrow(() -> new MCRLegacyXSLCheck().startUp(null));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = LAYOUT_PROPERTY + "Class", string = "%XALAN%")
    })
    public void rejectsLegacyLayoutOverrideWithInheritedDefault() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> new MCRLegacyXSLCheck().startUp(null));

        assertTrue(exception.getMessage().contains(LAYOUT_PROPERTY + "=Saxon"));
        assertTrue(
            exception.getMessage().contains(LAYOUT_PROPERTY + "Class=" + MCRXalanTransformerFactory.class.getName()));
        assertTrue(exception.getMessage().contains("Set '" + LAYOUT_PROPERTY + "' to the intended factory ID"));
        assertTrue(exception.getMessage().contains("remove '" + LAYOUT_PROPERTY + "Class'"));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = CONTENT_PROPERTY + "Class", string = "%XALAN%"),
        @MCRTestProperty(key = CUSTOM_PROPERTY + "Class", string = "application.CustomFactory"),
        @MCRTestProperty(key = CUSTOM_PROPERTY, string = "Custom")
    })
    public void reportsAllConflictsIncludingModuleDefaultsAndCustomPrefixes() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> new MCRLegacyXSLCheck().startUp(null));

        assertTrue(exception.getMessage().contains(CONTENT_PROPERTY + "=Saxon"));
        assertTrue(
            exception.getMessage().contains(CONTENT_PROPERTY + "Class=" + MCRXalanTransformerFactory.class.getName()));
        assertTrue(exception.getMessage().contains(CUSTOM_PROPERTY + "=Custom"));
        assertTrue(exception.getMessage().contains(CUSTOM_PROPERTY + "Class=application.CustomFactory"));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = LAYOUT_PROPERTY, empty = true),
        @MCRTestProperty(key = LAYOUT_PROPERTY + "Class", string = "%XALAN%"),
        @MCRTestProperty(key = CUSTOM_PROPERTY + "Class", string = "%SLOWXALAN%")
    })
    public void acceptsLegacyOnlyPropertiesAndPreservesFactorySelection() {
        assertDoesNotThrow(() -> new MCRLegacyXSLCheck().startUp(null));
        assertEquals("Xalan", MCRTransformerFactorySelector.getDefaultFactoryId());
        assertEquals("SlowXalan", MCRTransformerFactorySelector.getFactoryId(CUSTOM_PROPERTY,
            CUSTOM_PROPERTY + "Class", "Saxon"));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = LAYOUT_PROPERTY + "Class", string = "  "),
        @MCRTestProperty(key = CUSTOM_PROPERTY + "Class", string = "%XALAN%"),
        @MCRTestProperty(key = CUSTOM_PROPERTY, string = "  ")
    })
    public void treatsBlankValuesAsAbsent() {
        assertDoesNotThrow(() -> new MCRLegacyXSLCheck().startUp(null));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = LAYOUT_PROPERTY + "Class", string = "%SAXON%")
    })
    public void requiresMigrationEvenWhenBothPropertiesSelectTheSameFactory() {
        assertThrows(MCRConfigurationException.class, () -> new MCRLegacyXSLCheck().startUp(null));
    }

}
