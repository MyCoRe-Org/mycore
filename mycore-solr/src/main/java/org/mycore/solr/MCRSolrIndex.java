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

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateBaseSolrClient;

/**
 * Describes a Collection or Core on a SolrServer
 */
public interface MCRSolrIndex {

  /**
   * The name of the collection/core on the servers
   * @return the name
   */
  String getName();

  /**
   * Returns a {@link SolrClient} which has this {@link MCRSolrIndex} set as default
   * collection/core.
   * @return the SolrClient
   */
  SolrClient getClient();

  /**
   * Returns a {@link SolrClient} without a default collection/core
   * @return the SolrClient
   */
  SolrClient getBaseClient();

  /**
   * Returns an optional {@link ConcurrentUpdateBaseSolrClient} if the implementation provides one.
   * This client can be used for concurrent updates to the index, which can improve performance in
   * certain scenarios. If the implementation does not provide a concurrent client, this method
   * will return an empty Optional.
   * @return an Optional containing the ConcurrentUpdateBaseSolrClient if available,
   * or an empty Optional if not a
   */
  Optional<ConcurrentUpdateBaseSolrClient> getConcurrentClient();

  /**
   * Returns a set of {@link MCRIndexType} which describes the Content stored in this
   * collection/core
   * @return a set
   */
  Set<MCRIndexType> getIndexTypes();

  /**
   * Closes the underlying SolrClient(s) and releases any resources.
   */
  void close() throws IOException;
}
