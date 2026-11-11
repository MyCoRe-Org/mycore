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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRXSLTProcessorSelectorTest {

    private static final String PROPERTY = "MCR.Test.Selector.XSLTProcessor";

    private static final String LEGACY_PROPERTY = "MCR.Test.Selector.TransformerFactoryClass";

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = PROPERTY, string = "xalan")
    })
    public void acceptsRegisteredProcessorId() {
        assertEquals("xalan", MCRXSLTProcessorSelector.getProcessorId(PROPERTY, LEGACY_PROPERTY, "saxon"));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = PROPERTY, string = "saxn")
    })
    public void unknownProcessorIdNamesProperty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> MCRXSLTProcessorSelector.getProcessorId(PROPERTY, LEGACY_PROPERTY, "saxon"));

        assertTrue(exception.getMessage().contains("'" + PROPERTY + "'"), exception.getMessage());
        assertTrue(exception.getMessage().contains("'saxn'"), exception.getMessage());
        assertTrue(exception.getMessage().contains("saxon"), exception.getMessage());
        assertTrue(exception.getMessage().contains("MCR.XSLTProcessorRegistry.Entries.saxn.Factory.Class"),
            exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = PROPERTY, string = "org.example.MissingTransformerFactory")
    })
    public void unloadableClassNameNamesProperty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> MCRXSLTProcessorSelector.getProcessorId(PROPERTY, LEGACY_PROPERTY, "saxon"));

        assertTrue(exception.getMessage().contains("'" + PROPERTY + "'"), exception.getMessage());
        assertTrue(exception.getMessage().contains("org.example.MissingTransformerFactory"),
            exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = PROPERTY, classNameOf = String.class)
    })
    public void nonFactoryClassNameNamesProperty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> MCRXSLTProcessorSelector.getProcessorId(PROPERTY, LEGACY_PROPERTY, "saxon"));

        assertTrue(exception.getMessage().contains("'" + PROPERTY + "'"), exception.getMessage());
        assertTrue(exception.getMessage().contains("javax.xml.transform.TransformerFactory"),
            exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = PROPERTY, string = "org.mycore.common.xsl.MCRXalanTransformerFactory")
    })
    public void mapsDeprecatedFactoryClassValueToProcessorId() {
        assertEquals("xalan", MCRXSLTProcessorSelector.getProcessorId(PROPERTY, LEGACY_PROPERTY, "saxon"));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.LayoutService.XSLTProcessor", string = "unknown")
    })
    public void unknownDefaultProcessorIdNamesLayoutProperty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            MCRXSLTProcessorSelector::getDefaultProcessorId);

        assertTrue(exception.getMessage().contains("'MCR.LayoutService.XSLTProcessor'"), exception.getMessage());
    }

}
