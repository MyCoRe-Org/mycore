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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.mycore.common.config.MCRConfigurationException;

/**
 * Central registry interface for accessing {@link MCRSolrIndex} instances.
 *
 * <p>Provides methods to look up Solr indexes by their identifier or by their
 * {@link MCRSolrIndexType}.
 */
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
  List<MCRSolrIndex> getIndexByType(MCRSolrIndexType type);

  /**
   * Returns a map of all configured {@link MCRSolrIndex} instances, keyed by their unique
   * identifier.
   * @return a readonly map of all configured indexes, where the key is the index identifier
   * and the value is the corresponding {@link MCRSolrIndex}
   */
  Map<String, MCRSolrIndex> getIndexes();

  /**
   * Closes all registered {@link MCRSolrIndex} instances, ensuring that any underlying
   * resources such as Solr clients are properly released. This method is typically called
   * during application shutdown to clean up resources. After calling this method, the registry
   * should not be used to access any indexes, as they will have been closed.
   */
  void closeIndexes();

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
