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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRLegacyXSLCheckTest {

    private static final String LAYOUT_PROPERTY = "MCR.LayoutService.XSLTProcessor";

    private static final String LEGACY_LAYOUT_PROPERTY = "MCR.LayoutService.TransformerFactoryClass";

    private static final String CONTENT_PROPERTY = "MCR.ContentTransformer.stylesheets-yed.XSLTProcessor";

    private static final String LEGACY_CONTENT_PROPERTY =
        "MCR.ContentTransformer.stylesheets-yed.TransformerFactoryClass";

    private static final String CUSTOM_PROPERTY = "MCR.Test.Custom.XSLTProcessor";

    private static final String LEGACY_CUSTOM_PROPERTY = "MCR.Test.Custom.TransformerFactoryClass";

    private static final String FLAVOR_PROPERTY = "MCR.Test.Flavor.XSLTProcessor";

    private static final String LEGACY_FLAVOR_PROPERTY = "MCR.Test.Flavor.TransformerFactory.Class";

    private static String getOverrides() {
        return MCRLegacyXSLCheck.getOverrides(MCRConfiguration2.getAllPropertiesMap());
    }

    @BeforeEach
    public void resetLegacyWarnings() {
        MCRXSLTProcessorSelector.resetLegacyWarnings();
    }

    @Test
    public void acceptsDefaultsAndIsRegisteredFirst() {
        assertEquals(MCRLegacyXSLCheck.class.getName(),
            MCRConfiguration2.getStringOrThrow("MCR.Startup.Class").split(",")[0]);
        assertDoesNotThrow(() -> new MCRLegacyXSLCheck().startUp(null));
        assertEquals("", getOverrides());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = LEGACY_LAYOUT_PROPERTY, string = "org.mycore.common.xsl.MCRXalanTransformerFactory")
    })
    public void legacyLayoutPropertyOverridesInheritedDefault() {
        assertDoesNotThrow(() -> new MCRLegacyXSLCheck().startUp(null));
        assertEquals("xalan", MCRXSLTProcessorSelector.getDefaultProcessorId());

        String overrides = getOverrides();
        assertTrue(overrides.contains(LEGACY_LAYOUT_PROPERTY + "=" + MCRXalanTransformerFactory.class.getName()));
        assertTrue(overrides.contains("overrides " + LAYOUT_PROPERTY + "=saxon"));
        assertTrue(overrides.contains("Set '" + LAYOUT_PROPERTY + "' to the intended XSLT processor ID"));
        assertTrue(overrides.contains("remove '" + LEGACY_LAYOUT_PROPERTY + "'"));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = CUSTOM_PROPERTY, string = "xalan"),
        @MCRTestProperty(key = LEGACY_CUSTOM_PROPERTY, string = "net.sf.saxon.TransformerFactoryImpl")
    })
    public void legacyPropertyOverridesModuleDefault() {
        assertEquals("saxon", MCRXSLTProcessorSelector.getProcessorId(CUSTOM_PROPERTY,
            LEGACY_CUSTOM_PROPERTY, "slowXalan"));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = LEGACY_CONTENT_PROPERTY, string = "org.mycore.common.xsl.MCRXalanTransformerFactory"),
        @MCRTestProperty(key = LEGACY_CUSTOM_PROPERTY, string = "application.CustomFactory"),
        @MCRTestProperty(key = CUSTOM_PROPERTY, string = "custom"),
        @MCRTestProperty(key = LEGACY_FLAVOR_PROPERTY, string = "org.mycore.common.xsl.MCRXalanTransformerFactory"),
        @MCRTestProperty(key = FLAVOR_PROPERTY, string = "saxon")
    })
    public void reportsAllOverridesIncludingModuleDefaultsCustomPrefixesAndFlavors() {
        String overrides = getOverrides();

        assertTrue(overrides.contains("overrides " + CONTENT_PROPERTY + "=saxon"));
        assertTrue(overrides.contains(LEGACY_CONTENT_PROPERTY + "=" + MCRXalanTransformerFactory.class.getName()));
        assertTrue(overrides.contains("overrides " + CUSTOM_PROPERTY + "=custom"));
        assertTrue(overrides.contains(LEGACY_CUSTOM_PROPERTY + "=application.CustomFactory"));
        assertTrue(overrides.contains("overrides " + FLAVOR_PROPERTY + "=saxon"));
        assertTrue(overrides.contains(LEGACY_FLAVOR_PROPERTY + "=" + MCRXalanTransformerFactory.class.getName()));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = LAYOUT_PROPERTY, empty = true),
        @MCRTestProperty(key = LEGACY_LAYOUT_PROPERTY, string = "org.mycore.common.xsl.MCRXalanTransformerFactory"),
        @MCRTestProperty(key = LEGACY_CUSTOM_PROPERTY, string = "org.apache.xalan.processor.TransformerFactoryImpl")
    })
    public void acceptsLegacyOnlyPropertiesAndPreservesProcessorSelection() {
        assertDoesNotThrow(() -> new MCRLegacyXSLCheck().startUp(null));
        assertEquals("", getOverrides());
        assertEquals("xalan", MCRXSLTProcessorSelector.getDefaultProcessorId());
        assertEquals("slowXalan", MCRXSLTProcessorSelector.getProcessorId(CUSTOM_PROPERTY,
            LEGACY_CUSTOM_PROPERTY, "saxon"));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = LEGACY_LAYOUT_PROPERTY, string = "  "),
        @MCRTestProperty(key = LEGACY_CUSTOM_PROPERTY, string = "org.mycore.common.xsl.MCRXalanTransformerFactory"),
        @MCRTestProperty(key = CUSTOM_PROPERTY, string = "  ")
    })
    public void treatsBlankValuesAsAbsent() {
        assertEquals("", getOverrides());
        assertEquals("saxon", MCRXSLTProcessorSelector.getDefaultProcessorId());
    }

}
