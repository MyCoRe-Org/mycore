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

package org.mycore.solr.cloud.collection;

import org.mycore.solr.MCRSolrIndex;

/**
 * Represents a Solr Cloud collection and extends {@link MCRSolrIndex} with
 * parameters required for creating the collection on a SolrCloud server.
 *
 * <p>The getter methods provide the configuration values that are passed to
 * the SolrCloud Collection Admin API when creating or managing the collection
 * (see {@link MCRSolrCollectionHelper#createCollection(MCRSolrCloudCollection)}).</p>
 *
 * @see MCRSolrIndex
 * @see MCRSolrCollectionHelper
 * @see MCRConfigurableSolrCloudCollection
 */
public interface MCRSolrCloudCollection extends MCRSolrIndex {

  /**
   * Returns the Configuration which is used to create the Collection on the Solr Server
   * @return the {@link MCRSolrCloudCollectionCreationConfiguration} instance containing the
   * collection creation parameters
   */
  MCRSolrCloudCollectionCreationConfiguration getCreationConfiguration();

}
