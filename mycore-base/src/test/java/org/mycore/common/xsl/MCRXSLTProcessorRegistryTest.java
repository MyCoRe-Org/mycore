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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.xsl.MCRXSLTProcessorRegistry.Entry;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRXSLTProcessorRegistryTest {

    private static final String TEST_PREFIX = "MCR.Test.XSLTProcessor";

    private static final String ENTRIES_PREFIX = TEST_PREFIX + ".Entries.";

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = ENTRIES_PREFIX + "configured.Factory.Class", classNameOf = ConfigurableFactory.class),
        @MCRTestProperty(key = ENTRIES_PREFIX + "configured.Initializers.10.Class",
            classNameOf = TestInitializer.class),
        @MCRTestProperty(key = ENTRIES_PREFIX + "configured.Initializers.10.Enabled", string = "true"),
        @MCRTestProperty(key = ENTRIES_PREFIX + "concurrent.Factory.Class",
            classNameOf = MCRXalanTransformerFactory.class),
        @MCRTestProperty(key = ENTRIES_PREFIX + "concurrent.SupportsConcurrency", string = "true")
    })
    public void createsAndInitializesProcessorsFromProperties() throws TransformerConfigurationException {
        MCRXSLTProcessorRegistry registry = MCRConfiguration2.getInstanceOfOrThrow(
            MCRXSLTProcessorRegistry.class, TEST_PREFIX);

        assertNotNull(registry.getProcessor("configured").newTransformer());
        assertFalse(registry.getProcessor("configured").supportsConcurrency());
        assertTrue(registry.getProcessor("concurrent").supportsConcurrency());
        assertThrows(MCRConfigurationException.class, () -> registry.getProcessor("missing"));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = ENTRIES_PREFIX + "invalid.Factory.Class",
            classNameOf = MCRXalanTransformerFactory.class),
        @MCRTestProperty(key = ENTRIES_PREFIX + "invalid.SupportsConcurrency", string = "yes")
    })
    public void rejectsInvalidSupportsConcurrencyValue() {
        assertThrows(MCRConfigurationException.class, () -> MCRConfiguration2.getInstanceOfOrThrow(
            MCRXSLTProcessorRegistry.class, TEST_PREFIX));
    }

    @Test
    @SuppressWarnings("removal")
    public void resolvesLegacyClassToUniqueConfiguredSubclass() {
        MCRXSLTProcessorRegistry registry = new MCRXSLTProcessorRegistry(Map.of(
            "Custom", new Entry(new ConfigurableFactory(), false)));

        assertSame(registry.getProcessor("Custom"), registry.getProcessor(MCRXalanTransformerFactory.class));
    }

    @Test
    @SuppressWarnings("removal")
    public void sharesLegacyFallbackForAmbiguousConfiguredClass() {

        MCRXSLTProcessorRegistry registry = new MCRXSLTProcessorRegistry(Map.of(
            "First", new Entry(new MCRXalanTransformerFactory(), false),
            "Second", new Entry(new MCRXalanTransformerFactory(), false)));

        MCRXSLTProcessor legacy = registry.getProcessor(MCRXalanTransformerFactory.class);

        assertNotSame(registry.getProcessor("First"), legacy);
        assertNotSame(registry.getProcessor("Second"), legacy);
        assertSame(legacy, registry.getProcessor(MCRXalanTransformerFactory.class));
        assertSame(legacy, registry.getProcessor(legacy.getId()));
        assertFalse(legacy.supportsConcurrency());
    }

    public static final class ConfigurableFactory extends MCRXalanTransformerFactory {

        private boolean configured;

        @Override
        public Transformer newTransformer() throws TransformerConfigurationException {
            if (!configured) {
                throw new TransformerConfigurationException("Factory was not configured");
            }
            return super.newTransformer();
        }

    }

    public static final class TestInitializer implements MCRXSLTProcessorRegistry.FactoryInitializer {

        private boolean enabled;

        @MCRProperty(name = "Enabled")
        public void setEnabled(String enabled) {
            this.enabled = Boolean.parseBoolean(enabled);
        }

        @Override
        public void initialize(SAXTransformerFactory factory) {
            ((ConfigurableFactory) factory).configured = enabled;
        }

    }

    public static final class FailingConfiguration implements MCRXSLTProcessorRegistry.FactoryInitializer {

        @Override
        public void initialize(SAXTransformerFactory factory) {
            throw new ClassCastException("Failure inside the configuration consumer");
        }

    }

}
