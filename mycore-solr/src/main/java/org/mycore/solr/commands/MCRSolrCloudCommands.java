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

package org.mycore.solr.commands;

import static org.mycore.solr.MCRSolrConstants.DEFAULT_SOLR_SERVER_URL;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrCore;
import org.mycore.solr.MCRSolrUtils;
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
        syntax = "show remote config sets",
        help = "displays the names of the config sets on the remote Solr server",
        order = 10)
    public static void listRemoteConfig() throws URISyntaxException, IOException, SolrServerException {
        List<String> configSetNames = MCRSolrConfigSetHelper.getRemoteConfigSetNames(getSolrServerURL());

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
        MCRSolrCore core = MCRSolrClientFactory.get(localCoreName)
                .orElseThrow(() -> MCRSolrUtils.getCoreConfigMissingException(localCoreName));
        String localConfigSetName = core.getConfigSet();
        String coreName = core.getName();
        String remoteConfigSetName = MCRSolrConfigSetHelper.buildRemoteConfigSetName(coreName, localConfigSetName);
        MCRSolrConfigSetHelper.deleteConfigSetFromRemoteSolrServer(
                getSolrServerURL(), remoteConfigSetName);
    }

    @MCRCommand(
        syntax = "upload local config set for {0}",
        help = "looks up in the mycore.properties which config set is used for the core {0} and uploads it to the" +
                "remote Solr server",
        order = 40)
    public static void uploadLocalConfig(String coreName) throws URISyntaxException, SolrServerException, IOException {
        MCRSolrCore core = MCRSolrClientFactory.get(coreName)
                .orElseThrow(() -> MCRSolrUtils.getCoreConfigMissingException(coreName));
        String localConfigSetName = core.getConfigSet();

        MCRSolrConfigSetProvider configSet = MCRSolrConfigSetHelper.getLocalConfigSets().get(localConfigSetName);
        if (configSet == null) {
            LOGGER.error("ConfigSet {} not found.", localConfigSetName);
            return;
        }

        String remoteCoreName = core.getName();
        String remoteConfigSetName = MCRSolrConfigSetHelper.buildRemoteConfigSetName(remoteCoreName,
                localConfigSetName);

        MCRSolrConfigSetHelper.transferConfigSetToRemoteSolrServer(getSolrServerURL(),
                remoteConfigSetName, configSet);
    }


    @MCRCommand(
        syntax = "create collection for core {0}",
        help = "creates a new collection in the remote Solr server using the config set defined in the " +
                "mycore.properties",
        order = 60)
    public static void createCollection(String localCoreName)
            throws URISyntaxException, SolrServerException, IOException {
        MCRSolrCore core = MCRSolrClientFactory.get(localCoreName)
                .orElseThrow(() -> MCRSolrUtils.getCoreConfigMissingException(localCoreName));
        String localConfigSetName = core.getConfigSet();
        String remoteCoreName = core.getName();

        String remoteConfigSetName = MCRSolrConfigSetHelper.buildRemoteConfigSetName(remoteCoreName,
                localConfigSetName);

        MCRSolrCollectionHelper.createCollection(getSolrServerURL(), remoteCoreName, remoteConfigSetName,
            core.getShardCount());
    }

    private static URI getSolrServerURL() throws URISyntaxException {
        return new URI(DEFAULT_SOLR_SERVER_URL + "/solr");
    }

    @MCRCommand(
        syntax = "remove collection for core {0}",
            help = "remove a collection in the remote Solr server using the config set defined in the " +
                    "mycore.properties",
        order = 70)
    public static void removeCollection(String collectionName) throws URISyntaxException, SolrServerException,
            IOException {
        MCRSolrCollectionHelper.removeCollection(getSolrServerURL(), collectionName);
    }

}
