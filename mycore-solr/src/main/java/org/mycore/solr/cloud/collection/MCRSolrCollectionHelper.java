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

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.mycore.common.MCRException;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;

/**
 * Provides helper methods for working with Solr collections.
 */
public class MCRSolrCollectionHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Creates a new collection on the Solr server based on the given collection configuration.
     * The collection will be created with the name and shard count specified in the collection
     * configuration, and it will use the remote config set specified in the collection
     * configuration.
     *
     * @param collection The collection configuration based on which the collection should be
     *                   created.
     * @throws SolrServerException If an error occurs while communicating with the Solr server.
     * @throws IOException If an error occurs while communicating with the Solr server.
     */
    public static void createCollection(MCRSolrCloudCollection collection) throws SolrServerException, IOException {
        MCRSolrCloudCollectionCreationConfiguration creationConfiguration = collection.getCreationConfiguration();
        CollectionAdminRequest.Create collectionCreateRequest = CollectionAdminRequest
            .createCollection(collection.getName(), buildRemoteConfigSetName(collection),
                creationConfiguration.numShards(),
                creationConfiguration.numNrtReplicas(), creationConfiguration.numTlogReplicas(),
                creationConfiguration.numPullReplicas());

        MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(collectionCreateRequest,
            MCRSolrAuthenticationLevel.ADMIN);
        CollectionAdminResponse collectionAdminResponse = collectionCreateRequest
            .process(collection.getBaseClient());

        if (!collectionAdminResponse.isSuccess()) {
            throw new MCRException("Error creating collection " + collection.getName() + ": " +
                collectionAdminResponse.getErrorMessages());
        }

        LOGGER.info("Collection {} created.", collection::getName);
    }

    /**
     * Removes the given collection from the Solr server. This will delete all data in the
     * collection, so use with caution.
     * @param collection The collection to be removed.
     * @throws SolrServerException If an error occurs while communicating with the Solr server.
     * @throws IOException If an error occurs while communicating with the Solr server.
     */
    public static void removeCollection(MCRSolrCloudCollection collection) throws SolrServerException,
        IOException {
        CollectionAdminRequest.Delete collectionDeleteReq =
            CollectionAdminRequest.deleteCollection(collection.getName());
        MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(collectionDeleteReq,
            MCRSolrAuthenticationLevel.ADMIN);
        CollectionAdminResponse collectionAdminResponse = collectionDeleteReq
            .process(collection.getBaseClient());

        if (!collectionAdminResponse.isSuccess()) {
            throw new MCRException("Error creating collection " + collection.getName() + ": " +
                collectionAdminResponse.getErrorMessages());
        }

        LOGGER.info("Collection {} deleted.", collection::getName);
    }

    /**
     * Builds the name of the remote config set for the given collection. It is assumed that the
     * remote config set has the same name as the collection, followed by an underscore and the
     * config set template name.
     *
     * @param collection The collection for which the config set name should be built.
     * @return The name of the remote config set.
     */
    public static String buildRemoteConfigSetName(MCRSolrCloudCollection collection) {
        return collection.getName() + "_" + collection.getCreationConfiguration()
            .configSetTemplate();
    }
}
