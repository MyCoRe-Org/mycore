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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.config.annotation.MCRSentinel;

/**
 * Registry of named, application-scoped JAXP transformer factories.
 * <p>
 * Entries are injected from {@code MCR.TransformerRegistry.{id}.Class}. An optional consumer configured below
 * {@code MCR.TransformerRegistry.{id}.Configuration.Class} is applied before the common MyCoRe configuration.
 * Access to a factory is serialized by default and can be disabled with
 * {@code MCR.TransformerRegistry.{id}.SupportsConcurrency=false} for thread-safe providers.
 * A factory ID is one property-name segment and therefore must not contain a dot.
 */
@MCRConfigurationProxy(proxyClass = MCRTransformerFactoryRegistry.Factory.class)
public final class MCRTransformerFactoryRegistry {

    public static final String REGISTRY_PROPERTY = "MCR.TransformerRegistry";

    public static final String DEFAULT_PROPERTY_PREFIX = "MCR.Default.TransformerRegistry.";

    private static final String FACTORY_KEY = "Factory";

    private static final String SUPPORTS_CONCURRENCY_KEY = "SupportsConcurrency";

    private static final String INITIALIZERS_KEY = "Initializers";

    private final Map<String, MCRTransformerFactory> managers;

    private final MCRLegacyTransformerFactoryManagerProvider legacyFactoryProvider =
        new MCRLegacyTransformerFactoryManagerProvider();

    public MCRTransformerFactoryRegistry(Map<String, Entry> entries) {
        managers = new HashMap<>();
        entries.forEach((id, entry) -> {
            managers.put(id, new MCRTransformerFactory(id, entry.factory(), entry.supportsConcurrency()));
        });
    }

    public static MCRTransformerFactoryRegistry obtainInstance() {
        return MCRConfiguration2.getSingleInstanceOfOrThrow(MCRTransformerFactoryRegistry.class, REGISTRY_PROPERTY);
    }

    public static MCRTransformerFactoryRegistry createInstance() {
        return MCRConfiguration2.getInstanceOfOrThrow(MCRTransformerFactoryRegistry.class, REGISTRY_PROPERTY);
    }

    /**
     * Returns the shared factory for the default selected at call time.
     *
     * @return shared factory selected by {@link MCRTransformerFactorySelector#getDefaultFactoryId()}
     */
    public MCRTransformerFactory getSharedFactory() {
        return getSharedFactory(MCRTransformerFactorySelector.getDefaultFactoryId());
    }

    /**
     * Returns the shared factory with the supplied ID.
     *
     * @param id configured factory ID
     * @return shared factory for this ID
     */
    public MCRTransformerFactory getSharedFactory(String id) {
        MCRTransformerFactory factory = managers.get(id);
        if (factory == null) {
            factory = legacyFactoryProvider.get(id);
        }
        if (factory == null) {
            throw new MCRConfigurationException("Unknown transformer factory ID '" + id
                + "'. Configured IDs: " + managers.keySet());
        }
        return factory;
    }

    /**
     * Returns the uniquely configured factory with the supplied implementation class.
     *
     * @deprecated use {@link #getSharedFactory(String)} with a configured factory ID
     */
    @Deprecated(forRemoval = true)
    public MCRTransformerFactory getFactory(Class<? extends TransformerFactory> factoryClass) {
        return getSharedFactory(MCRLegacyTransformerFactoryManagerProvider.getFactoryId(factoryClass, managers));
    }

    public static class Factory implements Supplier<MCRTransformerFactoryRegistry> {

        @MCRSentinel
        @MCRInstanceMap(valueClass = Entry.class, required = false)
        public Map<String, Entry> entries;

        @Override
        public MCRTransformerFactoryRegistry get() {
            return new MCRTransformerFactoryRegistry(entries);
        }

    }

    public interface Initializer {

        void initialize(SAXTransformerFactory factory);

    }

    @MCRConfigurationProxy(proxyClass = Entry.Factory.class)
    public static final class Entry {

        private final SAXTransformerFactory factory;

        private final boolean supportsConcurrency;

        public Entry(SAXTransformerFactory factory, boolean supportsConcurrency) {
            this.factory = Objects.requireNonNull(factory);
            this.supportsConcurrency = supportsConcurrency;
        }

        public SAXTransformerFactory factory() throws TransformerFactoryConfigurationError {
            return factory;
        }

        public boolean supportsConcurrency() {
            return supportsConcurrency;
        }

        public static class Factory implements Supplier<Entry> {

            @MCRInstance(name = FACTORY_KEY, valueClass = SAXTransformerFactory.class)
            public SAXTransformerFactory factory;

            @MCRProperty(name = SUPPORTS_CONCURRENCY_KEY, defaultName = DEFAULT_PROPERTY_PREFIX + "SupportsConcurrency")
            public String supportsConcurrency;

            @MCRSentinel
            @MCRInstanceList(name = INITIALIZERS_KEY, valueClass = Initializer.class, required = false)
            public List<Initializer> initializers;

            @Override
            public Entry get() {
                initializers.forEach(configuration -> configuration.initialize(factory));
                return new Entry(factory, Boolean.parseBoolean(supportsConcurrency));
            }

        }

    }

}
