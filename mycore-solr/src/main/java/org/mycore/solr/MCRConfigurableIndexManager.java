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
            .filter(col -> col.getCoreTypes().contains(type))
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
