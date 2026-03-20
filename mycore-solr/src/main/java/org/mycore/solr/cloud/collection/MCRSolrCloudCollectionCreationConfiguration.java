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


/**
 * Holds the configuration parameters required to create a SolrCloud collection.
 *
 * <p>These values are passed to the SolrCloud Collection Admin API
 * (via {@link org.apache.solr.client.solrj.request.CollectionAdminRequest.Create})
 * when a new collection is created in
 * {@link MCRSolrCollectionHelper#createCollection(MCRSolrCloudCollection)}.</p>
 *
 * @see MCRSolrCloudCollection#getCreationConfiguration()
 * @see MCRSolrCollectionHelper#createCollection(MCRSolrCloudCollection)
 */
public interface MCRSolrCloudCollectionCreationConfiguration {


  /**
   * Returns the number of shards to split the collection into.
   *
   * @return the number of shards, or {@code null} to use the Solr default
   */
  Integer numShards();


  /**
   * Returns the number of NRT (Near-Real-Time) replicas for each shard.
   * NRT replicas maintain a full copy of the index in memory and on disk and
   * support both indexing and searching.
   *
   * @return the number of NRT replicas, or {@code null} to use the Solr default
   */
  Integer numNrtReplicas();


  /**
   * Returns the number of TLOG replicas for each shard.
   * TLOG replicas maintain a transaction log and replicate index segments from
   * the shard leader, supporting searching but not direct indexing.
   *
   * @return the number of TLOG replicas, or {@code null} to use the Solr default
   */
  Integer numTlogReplicas();


  /**
   * Returns the number of PULL replicas for each shard.
   * PULL replicas replicate index segments from the shard leader and support
   * searching only; they do not maintain a transaction log.
   *
   * @return the number of PULL replicas, or {@code null} to use the Solr default
   */
  Integer numPullReplicas();


  /**
   * Returns the name of the config set template used to create the remote config set
   * for this collection. The remote config set name is derived by appending this
   * template name to the collection name (separated by an underscore).
   *
   * @return the config set template name
   * @see MCRSolrCollectionHelper#buildRemoteConfigSetName(MCRSolrCloudCollection)
   */
  String configSetTemplate();

}
