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
import org.mycore.solr.MCRSolrCore;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;

/**
 * Provides helper methods for working with Solr collections (cores).
 */
public class MCRSolrCollectionHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void createCollection(MCRSolrCore core) throws SolrServerException, IOException {
        CollectionAdminRequest.Create collectionCreateRequest = CollectionAdminRequest
            .createCollection(core.getName(), core.buildRemoteConfigSetName(), core.getShardCount(), null, null, null);
        MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(collectionCreateRequest,
            MCRSolrAuthenticationLevel.ADMIN);
        CollectionAdminResponse collectionAdminResponse = collectionCreateRequest
            .process(core.getBaseClient());

        if (!collectionAdminResponse.isSuccess()) {
            throw new MCRException("Error creating collection " + core.getName() + ": " +
                collectionAdminResponse.getErrorMessages());
        }

        LOGGER.info("Collection {} created.", core::getName);
    }

    public static void removeCollection(MCRSolrCore core) throws SolrServerException,
        IOException {
        CollectionAdminRequest.Delete collectionDeleteReq = CollectionAdminRequest.deleteCollection(core.getName());
        MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(collectionDeleteReq,
            MCRSolrAuthenticationLevel.ADMIN);
        CollectionAdminResponse collectionAdminResponse = collectionDeleteReq
            .process(core.getBaseClient());

        if (!collectionAdminResponse.isSuccess()) {
            throw new MCRException("Error creating collection " + core.getName() + ": " +
                collectionAdminResponse.getErrorMessages());
        }

        LOGGER.info("Collection {} deleted.", core::getName);
    }

}
