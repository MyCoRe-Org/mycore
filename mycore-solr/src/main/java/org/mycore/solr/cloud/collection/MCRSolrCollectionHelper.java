/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.mycore.common.MCRException;
import org.mycore.solr.MCRSolrAuthenticationHelper;
import org.mycore.solr.MCRSolrHttpHelper;

public class MCRSolrCollectionHelper {


    public static final Logger LOGGER = LogManager.getLogger();

    public static void createCollection(URI solrServerURL,
        String collectionName,
        String configSetName,
        int numShards) throws SolrServerException, IOException {

        CollectionAdminRequest.Create collectionCreateRequest = CollectionAdminRequest
                .createCollection(collectionName, configSetName, numShards == -1 ? 1 : numShards, null, null, null);
        MCRSolrAuthenticationHelper.addAuthentication(collectionCreateRequest,
                MCRSolrAuthenticationHelper.AuthenticationLevel.ADMIN);
        CollectionAdminResponse collectionAdminResponse = collectionCreateRequest
                .process(MCRSolrHttpHelper.getSolrClient(solrServerURL));

        if(!collectionAdminResponse.isSuccess()){
            throw new MCRException("Error creating collection " + collectionName + ": " +
                    collectionAdminResponse.getErrorMessages());
        }

        LOGGER.info("Collection {} created.", collectionName);
    }

    public static void removeCollection(URI solrServerURL, String collectionName) throws SolrServerException,
            IOException {
        CollectionAdminRequest.Delete collectionDeleteReq = CollectionAdminRequest.deleteCollection(collectionName);
        MCRSolrAuthenticationHelper.addAuthentication(collectionDeleteReq,
                MCRSolrAuthenticationHelper.AuthenticationLevel.ADMIN);
        CollectionAdminResponse collectionAdminResponse = collectionDeleteReq
                .process(MCRSolrHttpHelper.getSolrClient(solrServerURL));


        if(!collectionAdminResponse.isSuccess()){
            throw new MCRException("Error creating collection " + collectionName + ": " +
                    collectionAdminResponse.getErrorMessages());
        }

        LOGGER.info("Collection {} deleted.", collectionName);
    }

}
