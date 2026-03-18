package org.mycore.solr;

import java.util.List;
import java.util.Optional;
import org.mycore.common.config.MCRConfigurationException;

public interface MCRSolrIndexRegistry {

  /**
   * Returns the {@link MCRSolrIndex} with the given identifier.
   *
   * @param indexId the unique identifier of the index
   * @return an {@link Optional} containing the index if it exists, or an empty
   *         {@link Optional} if no index with the given identifier is configured
   */
  Optional<MCRSolrIndex> getIndex(String indexId);

  /**
   * Returns all {@link MCRSolrIndex} instances that are associated with the
   * given {@link MCRSolrIndexType}.
   *
   * @param type the index type to filter by
   * @return a list of matching indexes; may be empty if no index matches the type
   */
  List<MCRSolrIndex> getIndexWithType(MCRSolrIndexType type);

  /**
   * Returns the main index, identified by {@link MCRSolrConstants#MAIN_INDEX_ID}.
   *
   * @return an {@link Optional} containing the main index, or an empty
   *         {@link Optional} if no main index is configured
   */
  default Optional<MCRSolrIndex> getMainIndex() {
    return getIndex(MCRSolrConstants.MAIN_INDEX_ID);
  }

  /**
   * Returns the main index or throws an exception if it is not configured.
   *
   * @return the main {@link MCRSolrIndex}
   * @throws MCRConfigurationException if no main index is configured
   */
  default MCRSolrIndex requireMainIndex() {
    return getMainIndex()
        .orElseThrow(() -> new MCRConfigurationException("No main index configured"));
  }

  /**
   * Returns the index with the given identifier or throws an exception if it
   * is not configured.
   *
   * @param indexId the unique identifier of the index
   * @return the {@link MCRSolrIndex} with the given identifier
   * @throws MCRConfigurationException if no index with the given identifier is configured
   */
  default MCRSolrIndex requireIndex(String indexId) {
    return getIndex(indexId)
        .orElseThrow(() -> new MCRConfigurationException("No index with id " + indexId + " configured"));
  }

}
