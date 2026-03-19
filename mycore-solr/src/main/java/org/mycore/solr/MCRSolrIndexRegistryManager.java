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

import java.util.Optional;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCRShutdownHandler;


/**
 * Central manager interface for accessing the singleton {@link MCRSolrIndexRegistry} instance.
 * The registry is lazily created based on the MyCoRe configuration property
 * {@link MCRSolrConstants#SOLR_INDEX_REGISTRY_PROPERTY} and can be reloaded at runtime using the
 * {@link #reloadRegistry()} method, which discards the current instance and creates a new one.
 *
 * @see MCRSolrIndexRegistry
 */
public class MCRSolrIndexRegistryManager {

    private static volatile MCRSolrIndexRegistry instance;

    static {
        MCRShutdownHandler shutdownHandler = MCRShutdownHandler.getInstance();
        if (shutdownHandler != null) {
            shutdownHandler.addCloseable(MCRSolrIndexRegistryManager::closeIndexes);
        }
    }

    /**
     * Returns the singleton {@link MCRSolrIndexRegistry} instance. The instance is
     * lazily created using double-checked locking from the MyCoRe configuration
     * property {@link MCRSolrConstants#SOLR_INDEX_REGISTRY_PROPERTY}.
     *
     * @return the singleton {@link MCRSolrIndexRegistry} instance
     */
    public static MCRSolrIndexRegistry obtainRegistry() {
        if (instance == null) {
            synchronized (MCRSolrIndexRegistryManager.class) {
                if (instance == null) {
                    instance = MCRConfiguration2.getInstanceOf(
                        MCRSolrIndexRegistry.class,
                        MCRSolrConstants.SOLR_INDEX_REGISTRY_PROPERTY).orElseThrow();
                }
            }
        }
        return instance;
    }

    /**
     * Reloads the singleton {@link MCRSolrIndexRegistry} instance from the MyCoRe
     * configuration. This discards the current instance and creates a new one based
     * on the current value of {@link MCRSolrConstants#SOLR_INDEX_REGISTRY_PROPERTY}.
     */
    public static void reloadRegistry() {
        synchronized (MCRSolrIndexRegistryManager.class) {
            if (instance != null) {
                instance.closeIndexes();
            }
            instance = null;
            obtainRegistry();
        }
    }

    /**
     * Returns the main index, identified by {@link MCRSolrConstants#MAIN_INDEX_ID}.
     *
     * @return an {@link Optional} containing the main index, or an empty
     *         {@link Optional} if no main index is configured
     */
    public static Optional<MCRSolrIndex> getMainIndex() {
        return obtainRegistry().getMainIndex();
    }

    /**
     * Returns the main index or throws an exception if it is not configured.
     *
     * @return the main {@link MCRSolrIndex}
     * @throws org.mycore.common.config.MCRConfigurationException if no main index is configured
     */
    public static MCRSolrIndex requireMainIndex() {
        return obtainRegistry().requireMainIndex();
    }

    /**
     * Returns the classification index, identified by {@link MCRSolrConstants#CLASSIFICATION_INDEX_ID}.
     *
     * @return an {@link Optional} containing the classification index, or an empty
     *         {@link Optional} if no classification index is configured
     */
    public static Optional<MCRSolrIndex> getClassificationIndex() {
        return obtainRegistry().getClassificationIndex();
    }

    /**
     * Returns the classification index or throws an exception if it is not configured.
     *
     * @return the classification {@link MCRSolrIndex}
     * @throws org.mycore.common.config.MCRConfigurationException if no classification index is configured
     */
    public static MCRSolrIndex requireClassificationIndex() {
        return obtainRegistry().requireClassificationIndex();
    }

    private static void closeIndexes() {
        if( instance != null ) {
            instance.closeIndexes();
        }
    }
}
