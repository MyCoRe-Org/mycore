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

package org.mycore.solr.cloud.configsets;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ConfigSetAdminRequest;
import org.apache.solr.client.solrj.response.ConfigSetAdminResponse;
import org.apache.solr.common.util.ContentStreamBase;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.solr.MCRSolrConstants;
import org.mycore.solr.MCRSolrCore;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;

/**
 * Provides helper methods for working with Solr configuration sets.
 */
public class MCRSolrConfigSetHelper {

    public static final Logger LOGGER = LogManager.getLogger();

    /**
     * The prefix for the configuration set properties.
     */
    public static final String CONFIG_SET_PROPERTY_PREFIX = MCRSolrConstants.SOLR_CONFIG_PREFIX + "ConfigSet.";

    /**
     * Fetches the list of config sets from the remote Solr server using the v2 API.
     * @param core The core for which the config sets should be fetched.
     * @throws URISyntaxException If the URL is invalid.
     * @throws IOException If an error occurs while fetching the config sets.
     */
    public static List<String> getRemoteConfigSetNames(MCRSolrCore core) throws URISyntaxException, IOException,
        SolrServerException {
        ConfigSetAdminRequest.List listRequest = new ConfigSetAdminRequest.List();
        MCRSolrAuthenticationManager.getInstance().applyAuthentication(listRequest,
            MCRSolrAuthenticationLevel.ADMIN);

        SolrClient solrClient = core.getBaseClient();
        ConfigSetAdminResponse.List listRequestResponse = listRequest.process(solrClient);
        return listRequestResponse.getConfigSets();
    }

    /**
     * Fetches the list of config sets from the mycore configuration.
     * @return A list of config sets.
     */
    public static Map<String, MCRSolrConfigSetProvider> getLocalConfigSets() {
        Map<String, Callable<MCRSolrConfigSetProvider>> instances = MCRConfiguration2.getInstances(
            MCRSolrConfigSetProvider.class, CONFIG_SET_PROPERTY_PREFIX);
        Map<String, MCRSolrConfigSetProvider> configSets = new HashMap<>(instances.size());

        instances.forEach((name, supplier) -> {
            try {
                configSets.put(name, supplier.call());
            } catch (Exception e) {
                throw new MCRConfigurationException("Error while initializing config set " + name, e);
            }
        });

        return configSets;
    }

    /**
     * Transfers a config set to the remote Solr server.
     * @param core The core for which the config set should be transferred.
     */
    public static void transferConfigSetToRemoteSolrServer(MCRSolrCore core) throws SolrServerException, IOException {
        String remoteName = core.buildRemoteConfigSetName();

        try {
            List<String> remoteConfigSetNames = getRemoteConfigSetNames(core);
            if (remoteConfigSetNames.contains(remoteName)) {
                throw new MCRConfigurationException("Config set " + remoteName + " already exists on the " +
                    "remote Solr server.");
            }
        } catch (IOException | URISyntaxException e) {
            throw new MCRConfigurationException("Error while checking for existing config sets on the remote Solr " +
                "server.", e);
        }

        ConfigSetAdminRequest.Upload request = new ConfigSetAdminRequest.Upload();
        MCRSolrAuthenticationManager.getInstance().applyAuthentication(request, MCRSolrAuthenticationLevel.ADMIN);
        request.setConfigSetName(remoteName);

        MCRSolrConfigSetProvider configSetProvider = getLocalConfigSets().get(core.getConfigSet());

        request.setUploadStream(new ContentStreamBase() {
            @Override
            public String getContentType() {
                return "application/octet-stream";
            }

            @Override
            public InputStream getStream() {
                return configSetProvider.getStreamSupplier().get();
            }
        });

        ConfigSetAdminResponse uploadResponse = request.process(core.getBaseClient());

        if (uploadResponse.getStatus() != 0) {
            throw new MCRConfigurationException("Error while transferring config set to remote Solr server. " +
                "Status code: " + uploadResponse.getStatus() + "\n  " + uploadResponse.getErrorMessages());

        }

        LOGGER.info("Config set {} transferred to remote Solr server.", remoteName);
    }

    /**
     * Deletes a config set from the remote Solr server.
     * @param core The core for which the config set should be deleted.
     */
    public static void deleteConfigSetFromRemoteSolrServer(MCRSolrCore core) {
        ConfigSetAdminRequest.Delete request = new ConfigSetAdminRequest.Delete();
        MCRSolrAuthenticationManager.getInstance().applyAuthentication(request,
            MCRSolrAuthenticationLevel.ADMIN);

        request.setConfigSetName(core.buildRemoteConfigSetName());

        try {
            ConfigSetAdminResponse deleteResponse = request.process(core.getBaseClient());
            if (deleteResponse.getStatus() != 0) {
                throw new MCRConfigurationException("Error while deleting config set from remote Solr server. " +
                    "Status code: " + deleteResponse.getStatus() + "\n  " + deleteResponse.getErrorMessages());
            }
        } catch (IOException | SolrServerException e) {
            throw new MCRConfigurationException("Error while deleting config set from remote Solr server.", e);
        }

        LOGGER.info("Config set {} deleted from remote Solr server.", core::getConfigSet);
    }

}
