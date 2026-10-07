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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import javax.xml.transform.TransformerFactory;
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
 * Registry of the application-scoped {@link MCRXSLTProcessor XSLT processors}, each registered under an ID.
 * <p>
 * Every processor wraps a JAXP {@link SAXTransformerFactory}. Entries are configured under
 * {@code MCR.XSLTProcessorRegistry.Entries.{id}}:
 * <ul>
 *   <li>{@code Factory.Class} – the {@link SAXTransformerFactory} implementation</li>
 *   <li>{@code SupportsConcurrency} – {@code true} if the factory may be used by several threads at the same
 *   time; otherwise access is serialized (default: {@code false})</li>
 *   <li>{@code Initializers.{n}.Class} – optional {@link FactoryInitializer} instances applied to the factory
 *   in order</li>
 * </ul>
 * An XSLT processor ID is one property-name segment and therefore must not contain a dot.
 */
@MCRConfigurationProxy(proxyClass = MCRXSLTProcessorRegistry.Factory.class)
public final class MCRXSLTProcessorRegistry {

    public static final String REGISTRY_PROPERTY = "MCR.XSLTProcessorRegistry";

    public static final String ENTRIES_KEY = "Entries";

    public static final String ENTRIES_PROPERTY_PREFIX = REGISTRY_PROPERTY + "." + ENTRIES_KEY + ".";

    public static final String FACTORY_KEY = "Factory";

    private static final String SUPPORTS_CONCURRENCY_KEY = "SupportsConcurrency";

    private static final String INITIALIZERS_KEY = "Initializers";

    private final Map<String, MCRXSLTProcessor> processors;

    private final MCRLegacyXSLTProcessorProvider legacyProcessorProvider =
        new MCRLegacyXSLTProcessorProvider();

    public MCRXSLTProcessorRegistry(Map<String, Entry> entries) {
        Map<String, MCRXSLTProcessor> configuredProcessors = new HashMap<>();
        entries.forEach((id, entry) -> configuredProcessors.put(id,
            new MCRXSLTProcessor(id, entry.factory(), entry.supportsConcurrency())));
        processors = Map.copyOf(configuredProcessors);
    }

    /**
     * Returns the application-wide registry configured under {@value #REGISTRY_PROPERTY}.
     *
     * @return shared registry
     */
    public static MCRXSLTProcessorRegistry obtainInstance() {
        return MCRConfiguration2.getSingleInstanceOfOrThrow(MCRXSLTProcessorRegistry.class, REGISTRY_PROPERTY);
    }

    /**
     * Creates a new, independent registry configured under {@value #REGISTRY_PROPERTY}.
     *
     * @return new registry
     */
    public static MCRXSLTProcessorRegistry createInstance() {
        return MCRConfiguration2.getInstanceOfOrThrow(MCRXSLTProcessorRegistry.class, REGISTRY_PROPERTY);
    }

    /**
     * Returns the shared XSLT processor for the default selected at call time.
     *
     * @return shared XSLT processor selected by {@link MCRXSLTProcessorSelector#getDefaultProcessorId()}
     */
    public MCRXSLTProcessor getDefaultProcessor() {
        return getProcessor(MCRXSLTProcessorSelector.getDefaultProcessorId());
    }

    /**
     * Returns the shared XSLT processor with the supplied ID.
     *
     * @param id configured XSLT processor ID
     * @return shared XSLT processor for this ID
     */
    public MCRXSLTProcessor getProcessor(String id) {
        MCRXSLTProcessor processor = processors.get(id);
        if (processor == null) {
            processor = legacyProcessorProvider.get(id);
        }
        if (processor == null) {
            throw new MCRConfigurationException("Unknown XSLT processor ID '" + id
                + "'. Configured IDs: " + processors.keySet());
        }
        return processor;
    }

    /**
     * Returns the shared XSLT processor for the factory uniquely configured with the supplied implementation
     * class, or a shared legacy XSLT processor created for that class.
     *
     * @deprecated use {@link #getProcessor(String)} with a configured XSLT processor ID
     */
    @Deprecated(forRemoval = true)
    public MCRXSLTProcessor getProcessor(Class<? extends TransformerFactory> factoryClass) {
        return getProcessor(MCRLegacyXSLTProcessorProvider.getProcessorId(factoryClass, processors));
    }

    public static class Factory implements Supplier<MCRXSLTProcessorRegistry> {

        @MCRSentinel
        @MCRInstanceMap(name = ENTRIES_KEY, valueClass = Entry.class, required = false)
        public Map<String, Entry> entries;

        @Override
        public MCRXSLTProcessorRegistry get() {
            return new MCRXSLTProcessorRegistry(entries);
        }

    }

    /**
     * Prepares a registered factory before it is shared, e.g. by setting provider-specific attributes.
     */
    @FunctionalInterface
    public interface FactoryInitializer {

        void initialize(SAXTransformerFactory factory);

    }

    /**
     * A registered factory together with the MyCoRe settings that apply to it.
     */
    @MCRConfigurationProxy(proxyClass = Entry.Factory.class)
    public static final class Entry {

        private final SAXTransformerFactory factory;

        private final boolean supportsConcurrency;

        public Entry(SAXTransformerFactory factory, boolean supportsConcurrency) {
            this.factory = Objects.requireNonNull(factory);
            this.supportsConcurrency = supportsConcurrency;
        }

        public SAXTransformerFactory factory() {
            return factory;
        }

        public boolean supportsConcurrency() {
            return supportsConcurrency;
        }

        public static class Factory implements Supplier<Entry> {

            @MCRInstance(name = FACTORY_KEY, valueClass = SAXTransformerFactory.class)
            public SAXTransformerFactory factory;

            @MCRProperty(name = SUPPORTS_CONCURRENCY_KEY, required = false)
            public String supportsConcurrency;

            @MCRSentinel
            @MCRInstanceList(name = INITIALIZERS_KEY, valueClass = FactoryInitializer.class, required = false)
            public List<FactoryInitializer> initializers;

            @Override
            public Entry get() {
                initializers.forEach(initializer -> initializer.initialize(factory));
                return new Entry(factory, parseBoolean(supportsConcurrency));
            }

            private boolean parseBoolean(String value) {
                if (value == null) {
                    return false;
                }
                return switch (value.trim().toLowerCase(Locale.ROOT)) {
                    case "true" -> true;
                    case "false" -> false;
                    default -> throw new MCRConfigurationException("Invalid value '" + value + "' for property '"
                        + SUPPORTS_CONCURRENCY_KEY + "' of transformer factory " + factory.getClass().getName()
                        + ". Use 'true' or 'false'.");
                };
            }

        }

    }

}
