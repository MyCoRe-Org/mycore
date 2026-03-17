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

package org.mycore.solr.commands;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.solr.cloud.collection.MCRSolrCloudCollection;
import org.mycore.solr.MCRSolrIndex;
import org.mycore.solr.MCRSolrIndexManager;
import org.mycore.solr.cloud.collection.MCRSolrCollectionHelper;
import org.mycore.solr.cloud.configsets.MCRSolrConfigSetHelper;
import org.mycore.solr.cloud.configsets.MCRSolrConfigSetProvider;

/**
 * Class provides useful solr cloud related commands.
 *
 * @author Sebastian Hofmann
 */
@MCRCommandGroup(
    name = "SOLR-Cloud Commands")
public class MCRSolrCloudCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRCommand(
        syntax = "show remote config sets of {0}",
        help = "displays the names of the config sets on the remote Solr server were the collection"
            + " for the core {0} is located",
        order = 10)
    public static void listRemoteConfig(String collectionName) throws URISyntaxException,
        IOException, SolrServerException {

        MCRSolrCloudCollection collection = obtainCollection(collectionName);
        List<String> configSetNames = MCRSolrConfigSetHelper
            .getRemoteConfigSetNames(collection);

        for (String configSetName : configSetNames) {
            LOGGER.info("Remote ConfigSet: {}", configSetName);
        }

        if (configSetNames.isEmpty()) {
            LOGGER.info("No remote ConfigSets found.");
        }
    }

    @MCRCommand(
        syntax = "show local config sets",
        help = "displays MyCoRe properties for the current Solr configuration",
        order = 20)
    public static void listLocalConfig() {
        Map<String, MCRSolrConfigSetProvider> configSets = MCRSolrConfigSetHelper.getLocalConfigSets();
        configSets.forEach((name, configSet) -> LOGGER.info("Local ConfigSet: {}", name));

        if (configSets.isEmpty()) {
            LOGGER.info("No local ConfigSets found.");
        }
    }

    @MCRCommand(
        syntax = "delete remote config set for {0}",
        help = "looks up in the mycore.properties which config set is used for the core {0} and deletes it from the " +
            "remote Solr server",
        order = 30)
    public static void deleteRemoteConfig(String localCoreName) throws URISyntaxException {
        MCRSolrCloudCollection collection = obtainCollection(localCoreName);
        MCRSolrConfigSetHelper.deleteConfigSetFromRemoteSolrServer(collection);
    }

    @MCRCommand(
        syntax = "upload local config set for {0}",
        help = "looks up in the mycore.properties which config set is used for the core {0} and uploads it to the" +
            "remote Solr server",
        order = 40)
    public static void uploadLocalConfig(String collectionName)
        throws URISyntaxException, SolrServerException, IOException {
        MCRSolrCloudCollection collection = obtainCollection(collectionName);
        String localConfigSetName = collection.getConfigSetTemplate();

        MCRSolrConfigSetProvider configSet = MCRSolrConfigSetHelper.getLocalConfigSets().get(localConfigSetName);
        if (configSet == null) {
            LOGGER.error("ConfigSet {} not found.", localConfigSetName);
            return;
        }

        MCRSolrConfigSetHelper.transferConfigSetToRemoteSolrServer(collection);
    }

    @MCRCommand(
        syntax = "create collection for core {0}",
        help = "creates a new collection in the remote Solr server using the config set defined in the " +
            "mycore.properties",
        order = 60)
    public static void createCollection(String collectionName)
        throws SolrServerException, IOException {
        MCRSolrCloudCollection collection = obtainCollection(collectionName);
        MCRSolrCollectionHelper.createCollection(collection);
    }

    @MCRCommand(
        syntax = "remove collection for core {0}",
        help = "remove a collection in the remote Solr server using the config set defined in the " +
            "mycore.properties",
        order = 70)
    public static void removeCollection(String collectionName) throws SolrServerException,
        IOException {
        MCRSolrCloudCollection collection = obtainCollection(collectionName);
        MCRSolrCollectionHelper.removeCollection(collection);
    }

    /**
     * Helper method to obtain a {@link MCRSolrCloudCollection} for a given collection name.
     * The collection name is looked up in the {@link MCRSolrIndexManager} and checked if it is an
     * instance of {@link MCRSolrCloudCollection}.
     *
     * @param collectionName the name of the collection to obtain
     * @return the {@link MCRSolrCloudCollection} for the given collection name
     * @throws IllegalArgumentException if no index is found for the given collection name or
     * if the index is not a SolrCloud collection
     */
    private static MCRSolrCloudCollection obtainCollection(
        String collectionName) {
        Optional<MCRSolrIndex> optionalIndex = MCRSolrIndexManager.obtainInstance().getIndex(collectionName);

        if (optionalIndex.isEmpty()) {
            throw new IllegalArgumentException("No index found for collection name " + collectionName);
        }

        MCRSolrIndex index = optionalIndex.get();
        if (!(index instanceof MCRSolrCloudCollection collection)) {
            throw new IllegalArgumentException("Index " + collectionName + " is not a SolrCloud collection.");
        }
        return collection;
    }

}
