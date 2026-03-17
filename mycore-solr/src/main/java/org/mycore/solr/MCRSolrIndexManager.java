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
import java.util.Optional;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;


/**
 * Central manager interface for accessing and managing {@link MCRSolrIndex} instances.
 *
 * <p>Provides methods to look up Solr indexes by their identifier or by their
 * {@link MCRIndexType}. A singleton instance is lazily created from the MyCoRe
 * configuration property defined by {@link MCRSolrConstants#SOLR_INDEX_MANAGER_PROPERTY}
 * and can be obtained via {@link #obtainInstance()}.</p>
 *
 * @see MCRSolrIndex
 * @see MCRIndexType
 */
public interface MCRSolrIndexManager {

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
     * given {@link MCRIndexType}.
     *
     * @param type the index type to filter by
     * @return a list of matching indexes; may be empty if no index matches the type
     */
    List<MCRSolrIndex> getIndexWithType(MCRIndexType type);

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

    /**
     * Returns the singleton {@link MCRSolrIndexManager} instance. The instance is
     * lazily created using double-checked locking from the MyCoRe configuration
     * property {@link MCRSolrConstants#SOLR_INDEX_MANAGER_PROPERTY}.
     *
     * @return the singleton {@link MCRSolrIndexManager} instance
     */
    static MCRSolrIndexManager obtainInstance() {
        MCRSolrIndexManager result = InstanceHolder.instance;
        if (result == null) {
            synchronized (InstanceHolder.class) {
                result = InstanceHolder.instance;
                if (result == null) {
                    InstanceHolder.instance = MCRConfiguration2.getInstanceOf(MCRSolrIndexManager.class,
                        MCRSolrConstants.SOLR_INDEX_MANAGER_PROPERTY).orElseThrow();
                    result = InstanceHolder.instance;
                }
            }
        }
        return result;
    }

    /**
     * Reloads the singleton {@link MCRSolrIndexManager} instance from the MyCoRe
     * configuration. This discards the current instance and creates a new one based
     * on the current value of {@link MCRSolrConstants#SOLR_INDEX_MANAGER_PROPERTY}.
     */
    static void reloadInstance() {
        synchronized (InstanceHolder.class) {

            InstanceHolder.instance = MCRConfiguration2.getInstanceOf(MCRSolrIndexManager.class,
                MCRSolrConstants.SOLR_INDEX_MANAGER_PROPERTY).orElseThrow();
        }
    }

    /**
     * Internal holder class for the lazy singleton instance. Uses the
     * {@code volatile} keyword to ensure thread-safe publication.
     */
    final class InstanceHolder {
        private static volatile MCRSolrIndexManager instance;
    }
}
