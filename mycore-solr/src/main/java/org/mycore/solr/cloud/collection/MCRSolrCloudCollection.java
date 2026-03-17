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
   * Returns the number of shards to split the collection into when it is created
   * on the SolrCloud server.
   *
   * @return the number of shards, or {@code null} to use the server default
   */
  Integer getNumShards();

  /**
   * Returns the number of NRT (Near-Real-Time) replicas for the collection.
   * NRT replicas maintain a transaction log and update their index locally.
   *
   * @return the number of NRT replicas, or {@code null} to use the server default
   */
  Integer getNumNrtReplicas();

  /**
   * Returns the number of TLOG replicas for the collection.
   * TLOG replicas maintain a transaction log but re-replicate from the leader
   * instead of indexing documents locally.
   *
   * @return the number of TLOG replicas, or {@code null} to use the server default
   */
  Integer getNumTlogReplicas();

  /**
   * Returns the number of PULL replicas for the collection.
   * PULL replicas replicate the index from the leader and do not maintain a
   * transaction log; they only serve queries.
   *
   * @return the number of PULL replicas, or {@code null} to use the server default
   */
  Integer getNumPullReplicas();

  /**
   * Returns the name of the config set template used when creating the collection
   * on the SolrCloud server. The actual remote config set name is derived from the
   * collection name and this template name
   * (see {@link MCRSolrCollectionHelper#buildRemoteConfigSetName(MCRSolrCloudCollection)}).
   *
   * @return the config set template name
   */
  String getConfigSetTemplate();
}
