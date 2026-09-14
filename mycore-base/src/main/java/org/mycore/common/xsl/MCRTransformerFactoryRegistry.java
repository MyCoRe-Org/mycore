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
import java.util.Map;
import java.util.function.Consumer;

import javax.xml.transform.TransformerFactory;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRPostConstruction;

/**
 * Registry of named, application-scoped JAXP transformer factories.
 * <p>
 * Entries are injected from {@code MCR.TransformerFactory.{id}.Class}. An optional consumer configured below
 * {@code MCR.TransformerFactory.{id}.Configuration.Class} is applied before the common MyCoRe configuration.
 * Access to a factory is serialized by default and can be disabled with
 * {@code MCR.TransformerFactory.{id}.SerializeAccess=false} for thread-safe providers.
 * A factory ID is one property-name segment and therefore must not contain a dot.
 */
public final class MCRTransformerFactoryRegistry {

    public static final String CONFIGURATION_PREFIX = "MCR.TransformerFactory";

    private static final String SERIALIZE_ACCESS_PROPERTY = "SerializeAccess";

    private Map<String, TransformerFactory> configuredFactories = Map.of();

    private Map<String, MCRSAXTransformerFactoryManager> factories = Map.of();

    private final MCRLegacyTransformerFactorySupport legacyFactories = new MCRLegacyTransformerFactorySupport();

    /**
     * Receives the transformer factories configured directly below {@link #CONFIGURATION_PREFIX}.
     *
     * @param configuredFactories transformer factories by configured ID
     */
    @MCRInstanceMap(valueClass = TransformerFactory.class)
    public void setConfiguredFactories(Map<String, TransformerFactory> configuredFactories) {
        this.configuredFactories = new HashMap<>(configuredFactories);
    }

    /**
     * Applies the optional per-factory configuration and creates the shared holders.
     *
     * @param configurationPrefix canonical property prefix supplied by the MyCoRe instantiator
     */
    @MCRPostConstruction
    public void initialize(String configurationPrefix) {
        Map<String, MCRSAXTransformerFactoryManager> initializedFactories = new HashMap<>();
        configuredFactories.forEach((id, factory) -> {
            applyConfiguration(configurationPrefix, id, factory);
            boolean serializeAccess = MCRConfiguration2
                .getBoolean(configurationPrefix + "." + id + "." + SERIALIZE_ACCESS_PROPERTY)
                .orElse(true);
            initializedFactories.put(id, new MCRSAXTransformerFactoryManager(id, factory, serializeAccess));
        });
        factories = Map.copyOf(initializedFactories);
        configuredFactories = Map.of();
    }

    /**
     * Returns the shared factory with the supplied ID.
     *
     * @param id configured factory ID
     * @return shared factory
     */
    public MCRSAXTransformerFactoryManager get(String id) {
        MCRSAXTransformerFactoryManager factory = factories.get(id);
        if (factory == null) {
            factory = legacyFactories.get(id);
        }
        if (factory == null) {
            throw new MCRConfigurationException("Unknown transformer factory ID '" + id
                + "'. Configured IDs: " + factories.keySet());
        }
        return factory;
    }

    /**
     * Resolves a legacy class to a configured or internal legacy factory.
     */
    MCRSAXTransformerFactoryManager get(Class<? extends TransformerFactory> factoryClass) {
        return get(MCRLegacyTransformerFactorySupport.getFactoryId(factoryClass, factories));
    }

    @SuppressWarnings("unchecked")
    private void applyConfiguration(String configurationPrefix, String id, TransformerFactory factory) {
        String property = configurationPrefix + "." + id + ".Configuration";
        MCRConfiguration2.getInstanceOf(Consumer.class, property).ifPresent(configuration -> {
            try {
                ((Consumer<TransformerFactory>) configuration).accept(factory);
            } catch (MCRConfigurationException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new MCRConfigurationException("Could not configure transformer factory "
                    + factory.getClass().getName() + " using " + property + ".Class", e);
            }
        });
    }

}
