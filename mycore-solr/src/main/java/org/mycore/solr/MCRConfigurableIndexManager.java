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

import static org.mycore.solr.MCRSolrConstants.SOLR_COLLECTION_MANAGER_INDEX_PREFIX;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.solr.MCRConfigurableIndexManager.ConfigAdapter;

/**
 * Configurable implementation of {@link MCRSolrIndexManager} that manages a set of
 * {@link MCRSolrIndex} instances provided via the MyCoRe configuration system.
 *
 * <p>The indexes are injected as a name-to-index map through the {@link ConfigAdapter},
 * which reads the configuration properties prefixed with
 * {@link MCRSolrConstants#SOLR_COLLECTION_MANAGER_INDEX_PREFIX}. Each entry in the map
 * represents a named Solr collection or core that can be looked up by its identifier
 * or filtered by its {@link MCRIndexType}.</p>
 *
 * <p>This manager registers itself with the {@link MCRShutdownHandler} to ensure that
 * all underlying Solr clients are properly closed when the application shuts down.</p>
 *
 * @see MCRSolrIndexManager
 * @see MCRSolrIndex
 * @see ConfigAdapter
 */
@MCRConfigurationProxy(
    proxyClass = ConfigAdapter.class)
public class MCRConfigurableIndexManager implements MCRSolrIndexManager {

    private final Map<String, MCRSolrIndex> configuredCollections;
    private static final Logger LOGGER = LogManager.getLogger();

    public MCRConfigurableIndexManager(Map<String, MCRSolrIndex> configuredCollections) {
        this.configuredCollections = configuredCollections;

        MCRShutdownHandler shutdownHandler = MCRShutdownHandler.getInstance();
        if (shutdownHandler != null) {
            shutdownHandler.addCloseable(this::closeIndexes);
        }
    }

    @Override
    public Optional<MCRSolrIndex> getIndex(String indexId) {
        return Optional.ofNullable(configuredCollections.get(indexId));
    }

    @Override
    public List<MCRSolrIndex> getIndexWithType(MCRIndexType type) {
        return configuredCollections.values()
            .stream()
            .filter(col -> col.getIndexTypes().contains(type))
            .toList();
    }

    public void closeIndexes() {
        configuredCollections.values().forEach(col -> {
            try {
                col.close();
            } catch (IOException e) {
                LOGGER.error("Error closing Solr index {}", col::getName, () -> e);
            }
        });
    }

    public static class ConfigAdapter implements Supplier<MCRConfigurableIndexManager> {

        private Map<String, MCRSolrIndex> collections;

        public Map<String, MCRSolrIndex> getCollections() {
            return collections;
        }

        @MCRInstanceMap(name = SOLR_COLLECTION_MANAGER_INDEX_PREFIX, valueClass = MCRSolrIndex.class)
        public void setCollections(Map<String, MCRSolrIndex> collections) {
            this.collections = collections;
        }

        @Override
        public MCRConfigurableIndexManager get() {
            return new MCRConfigurableIndexManager(this.getCollections());
        }
    }
}
