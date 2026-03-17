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
