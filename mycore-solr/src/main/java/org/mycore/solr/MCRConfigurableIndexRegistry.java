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

package org.mycore.solr;

import static org.mycore.solr.MCRSolrConstants.SOLR_INDEX_REGISTRY_INDEX_PREFIX;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.solr.MCRConfigurableIndexRegistry.ConfigAdapter;

/**
 * Configurable implementation of {@link MCRSolrIndexRegistry} that manages a set of
 * {@link MCRSolrIndex} instances provided via the MyCoRe configuration system.
 *
 * <p>The indexes are injected as a name-to-index map through the {@link ConfigAdapter},
 * which reads the configuration properties prefixed with
 * {@link MCRSolrConstants#SOLR_INDEX_REGISTRY_INDEX_PREFIX}. Each entry in the map
 * represents a named Solr collection or core that can be looked up by its identifier
 * or filtered by its {@link MCRSolrIndexType}.</p>
 *
 * <p>This manager registers itself with the {@link MCRShutdownHandler} to ensure that
 * all underlying Solr clients are properly closed when the application shuts down.</p>
 *
 * @see MCRSolrIndexRegistryManager
 * @see MCRSolrIndex
 * @see ConfigAdapter
 */
@MCRConfigurationProxy(
    proxyClass = ConfigAdapter.class)
public class MCRConfigurableIndexRegistry implements MCRSolrIndexRegistry {

    private final Map<String, MCRSolrIndex> configuredIndexes;
    private static final Logger LOGGER = LogManager.getLogger();

    public MCRConfigurableIndexRegistry(Map<String, MCRSolrIndex> configuredIndexes) {
        this.configuredIndexes = configuredIndexes;
    }

    @Override
    public Optional<MCRSolrIndex> getIndex(String indexId) {
        return Optional.ofNullable(configuredIndexes.get(indexId));
    }

    @Override
    public List<MCRSolrIndex> getIndexByType(MCRSolrIndexType type) {
        return configuredIndexes.values()
            .stream()
            .filter(col -> col.getIndexTypes().contains(type))
            .toList();
    }

    @Override
    public Map<String, MCRSolrIndex> getIndexes() {
        return Collections.unmodifiableMap(configuredIndexes);
    }

    @Override
    public void closeIndexes() {
        configuredIndexes.values().forEach(col -> {
            try {
                col.close();
            } catch (IOException e) {
                LOGGER.error("Error closing Solr index {}", col::getName, () -> e);
            }
        });
    }

    public static class ConfigAdapter implements Supplier<MCRConfigurableIndexRegistry> {

        private Map<String, MCRSolrIndex> configuredIndexes;

        public Map<String, MCRSolrIndex> getConfiguredIndexes() {
            return configuredIndexes;
        }

        @MCRInstanceMap(name = SOLR_INDEX_REGISTRY_INDEX_PREFIX, valueClass = MCRSolrIndex.class)
        public void setConfiguredIndexes(Map<String, MCRSolrIndex> configuredIndexes) {
            this.configuredIndexes = configuredIndexes;
        }

        @Override
        public MCRConfigurableIndexRegistry get() {
            return new MCRConfigurableIndexRegistry(this.getConfiguredIndexes());
        }
    }
}
