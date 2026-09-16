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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.function.Consumer;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRTransformerFactoryRegistryTest {

    private static final String TEST_PREFIX = "MCR.Test.TransformerFactory";

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = TEST_PREFIX + ".Configured.Class", classNameOf = ConfigurableFactory.class),
        @MCRTestProperty(
            key = TEST_PREFIX + ".Configured.Configuration.Class",
            classNameOf = TestConfiguration.class),
        @MCRTestProperty(key = TEST_PREFIX + ".Configured.Configuration.Enabled", string = "true"),
        @MCRTestProperty(key = TEST_PREFIX + ".Concurrent.Class", classNameOf = MCRXalanTransformerFactory.class),
        @MCRTestProperty(key = TEST_PREFIX + ".Concurrent.SerializeAccess", string = "false")
    })
    public void createsAndConfiguresFactoryMapFromProperties() throws TransformerConfigurationException {
        MCRTransformerFactoryRegistry registry = MCRConfiguration2.getInstanceOfOrThrow(
            MCRTransformerFactoryRegistry.class, TEST_PREFIX);

        assertNotNull(registry.get("Configured").newTransformer());
        assertTrue(registry.get("Configured").isAccessSerialized());
        assertFalse(registry.get("Concurrent").isAccessSerialized());
        assertThrows(MCRConfigurationException.class, () -> registry.get("Missing"));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = TEST_PREFIX + ".Failing.Class", classNameOf = MCRXalanTransformerFactory.class),
        @MCRTestProperty(
            key = TEST_PREFIX + ".Failing.Configuration.Class",
            classNameOf = FailingConfiguration.class)
    })
    public void preservesFailuresFromFactoryConfigurationConsumer() {
        MCRTransformerFactoryRegistry registry = new MCRTransformerFactoryRegistry();
        registry.setConfiguredFactories(Map.of("Failing", new MCRXalanTransformerFactory()));
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> registry.initialize(TEST_PREFIX));

        assertTrue(exception.getMessage().contains("Could not configure transformer factory"));
        assertInstanceOf(ClassCastException.class, exception.getCause());
    }

    @Test
    public void resolvesLegacyClassToUniqueConfiguredSubclass() {
        MCRTransformerFactoryRegistry registry = new MCRTransformerFactoryRegistry();
        registry.setConfiguredFactories(Map.of("Custom", new ConfigurableFactory()));
        registry.initialize(TEST_PREFIX);

        assertSame(registry.get("Custom"), registry.get(MCRXalanTransformerFactory.class));
    }

    @Test
    public void sharesLegacyFallbackForAmbiguousConfiguredClass() {
        MCRTransformerFactoryRegistry registry = new MCRTransformerFactoryRegistry();
        registry.setConfiguredFactories(Map.of(
            "First", new MCRXalanTransformerFactory(),
            "Second", new MCRXalanTransformerFactory()));
        registry.initialize(TEST_PREFIX);

        MCRSAXTransformerFactoryManager legacy = registry.get(MCRXalanTransformerFactory.class);

        assertNotSame(registry.get("First"), legacy);
        assertNotSame(registry.get("Second"), legacy);
        assertSame(legacy, registry.get(MCRXalanTransformerFactory.class));
        assertSame(legacy, registry.get(legacy.getId()));
        assertTrue(legacy.isAccessSerialized());
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

    public static final class TestConfiguration implements Consumer<TransformerFactory> {

        private boolean enabled;

        @MCRProperty(name = "Enabled")
        public void setEnabled(String enabled) {
            this.enabled = Boolean.parseBoolean(enabled);
        }

        @Override
        public void accept(TransformerFactory factory) {
            ((ConfigurableFactory) factory).configured = enabled;
        }

    }

    public static final class FailingConfiguration implements Consumer<TransformerFactory> {

        @Override
        public void accept(TransformerFactory factory) {
            throw new ClassCastException("Failure inside the configuration consumer");
        }

    }

}
