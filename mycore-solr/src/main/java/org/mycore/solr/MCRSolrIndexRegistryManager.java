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

import org.mycore.common.config.MCRConfiguration2;


/**
 * Central manager interface for accessing and managing {@link MCRSolrIndex} instances.
 *
 * <p>Provides methods to look up Solr indexes by their identifier or by their
 * {@link MCRSolrIndexType}. A singleton instance is lazily created from the MyCoRe
 * configuration property defined by {@link MCRSolrConstants#SOLR_INDEX_REGISTRY_PROPERTY}
 * and can be obtained via {@link #obtainRegistry()}.</p>
 *
 * @see MCRSolrIndex
 * @see MCRSolrIndexType
 */
public class MCRSolrIndexRegistryManager {

    private static volatile MCRSolrIndexRegistry instance;

    /**
     * Returns the singleton {@link MCRSolrIndexRegistryManager} instance. The instance is
     * lazily created using double-checked locking from the MyCoRe configuration
     * property {@link MCRSolrConstants#SOLR_INDEX_REGISTRY_PROPERTY}.
     *
     * @return the singleton {@link MCRSolrIndexRegistryManager} instance
     */
    public static MCRSolrIndexRegistry obtainRegistry() {
        if (instance == null) {
            synchronized (MCRSolrIndexRegistryManager.class) {
                if (instance == null) {
                    instance = MCRConfiguration2.getInstanceOf(
                        MCRConfigurableIndexRegistry.class,
                        MCRSolrConstants.SOLR_INDEX_REGISTRY_PROPERTY).orElseThrow();
                }
            }
        }
        return instance;
    }

    /**
     * Reloads the singleton {@link MCRSolrIndexRegistryManager} instance from the MyCoRe
     * configuration. This discards the current instance and creates a new one based
     * on the current value of {@link MCRSolrConstants#SOLR_INDEX_REGISTRY_PROPERTY}.
     */
    public static void reloadRegistry() {
        synchronized (MCRSolrIndexRegistryManager.class) {
            instance = MCRConfiguration2.getInstanceOf(MCRSolrIndexRegistry.class,
                MCRSolrConstants.SOLR_INDEX_REGISTRY_PROPERTY).orElseThrow();
        }
    }

}
