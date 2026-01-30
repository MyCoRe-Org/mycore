package org.mycore.solr;

import java.util.List;
import java.util.Optional;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

public interface MCRSolrIndexManager {

    Optional<MCRSolrIndex> getIndex(String indexId);

    List<MCRSolrIndex> getIndexWithType(MCRIndexType type);

    default Optional<MCRSolrIndex> getMainIndex() {
        return getIndex(MCRSolrConstants.MAIN_CORE_TYPE);
    }

    default MCRSolrIndex requireMainIndex() {
        return getMainIndex()
            .orElseThrow(() -> new MCRConfigurationException("No main index configured"));
    }

    default MCRSolrIndex requireIndex(String indexId) {
        return getIndex(indexId)
            .orElseThrow(() -> new MCRConfigurationException("No index with id " + indexId + " configured"));
    }

    static MCRSolrIndexManager obtainInstance() {
        MCRSolrIndexManager result = InstanceHolder.instance;
        if (result == null) {
            synchronized (InstanceHolder.class) {
                result = InstanceHolder.instance;
                if (result == null) {
                    InstanceHolder.instance = MCRConfiguration2.getInstanceOf(MCRSolrIndexManager.class,
                        MCRSolrConstants.SOLR_COLLECTION_MANAGER_PROPERTY).orElseThrow();
                    result = InstanceHolder.instance;
                }
            }
        }
        return result;
    }

    static void reloadInstance() {
        synchronized (InstanceHolder.class) {

            InstanceHolder.instance = MCRConfiguration2.getInstanceOf(MCRSolrIndexManager.class,
                MCRSolrConstants.SOLR_COLLECTION_MANAGER_PROPERTY).orElseThrow();
        }
    }

    final class InstanceHolder {
        private static volatile MCRSolrIndexManager instance;
    }
}
