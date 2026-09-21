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
import org.mycore.common.xsl.MCRTransformerFactoryManagerRegistry.Entry;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRTransformerFactoryManagerRegistryTest {

    private static final String TEST_PREFIX = "MCR.Test.TransformerFactory";

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = TEST_PREFIX + ".Configured.Factory.Class", classNameOf = ConfigurableFactory.class),
        @MCRTestProperty(key = TEST_PREFIX + ".Configured.Initializers.10.Class", classNameOf = TestInitalizer.class),
        @MCRTestProperty(key = TEST_PREFIX + ".Configured.Initializers.10.Enabled", string = "true"),
        @MCRTestProperty(key = TEST_PREFIX + ".Concurrent.Factory.Class",
            classNameOf = MCRXalanTransformerFactory.class),
        @MCRTestProperty(key = TEST_PREFIX + ".Concurrent.SupportsConcurrency", string = "true")
    })
    public void createsAndInitializesFactoryMapFromProperties() throws TransformerConfigurationException {
        MCRTransformerFactoryManagerRegistry registry = MCRConfiguration2.getInstanceOfOrThrow(
            MCRTransformerFactoryManagerRegistry.class, TEST_PREFIX);

        assertNotNull(registry.get("Configured").newTransformer());
        assertFalse(registry.get("Configured").supportsConcurrency());
        assertTrue(registry.get("Concurrent").supportsConcurrency());
        assertThrows(MCRConfigurationException.class, () -> registry.get("Missing"));
    }

    @Test
    public void resolvesLegacyClassToUniqueConfiguredSubclass() {
        MCRTransformerFactoryManagerRegistry registry = new MCRTransformerFactoryManagerRegistry(Map.of(
            "Custom", new Entry(new ConfigurableFactory(), false)));

        assertSame(registry.get("Custom"), registry.get(MCRXalanTransformerFactory.class));
    }

    @Test
    public void sharesLegacyFallbackForAmbiguousConfiguredClass() {

        MCRTransformerFactoryManagerRegistry registry = new MCRTransformerFactoryManagerRegistry(Map.of(
            "First", new Entry(new MCRXalanTransformerFactory(), false),
            "Second", new Entry(new MCRXalanTransformerFactory(), false)));

        MCRTransformerFactoryManager legacy = registry.get(MCRXalanTransformerFactory.class);

        assertNotSame(registry.get("First"), legacy);
        assertNotSame(registry.get("Second"), legacy);
        assertSame(legacy, registry.get(MCRXalanTransformerFactory.class));
        assertSame(legacy, registry.get(legacy.getId()));
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

    public static final class TestInitalizer implements MCRTransformerFactoryManagerRegistry.Initializer {

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

    public static final class FailingConfiguration implements MCRTransformerFactoryManagerRegistry.Initializer {

        @Override
        public void initialize(SAXTransformerFactory factory) {
            throw new ClassCastException("Failure inside the configuration consumer");
        }

    }

}
