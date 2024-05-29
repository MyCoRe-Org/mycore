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

package org.mycore.solr.cloud.configsets;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
import org.mycore.solr.MCRSolrAuthenticationHelper;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrConstants;
import org.mycore.solr.MCRSolrCore;
import org.mycore.solr.MCRSolrHttpHelper;
import org.mycore.solr.MCRSolrUtils;

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
     * @param solrServerURL The URL of the Solr server.
     * @return A list of config set names.
     * @throws URISyntaxException If the URL is invalid.
     * @throws IOException If an error occurs while fetching the config sets.
     */
    public static List<String> getRemoteConfigSetNames(URI solrServerURL) throws URISyntaxException, IOException,
            SolrServerException {
        ConfigSetAdminRequest.List listRequest = new ConfigSetAdminRequest.List();
        MCRSolrAuthenticationHelper.addAuthentication(listRequest,
            MCRSolrAuthenticationHelper.AuthenticationLevel.ADMIN);

        SolrClient solrClient = MCRSolrHttpHelper.getSolrClient(solrServerURL);
        ConfigSetAdminResponse.List listRequestResponse = listRequest.process(solrClient);
        return listRequestResponse.getConfigSets();
    }

    /**
     * Fetches the list of config sets from the mycore configuration.
     * @return A list of config sets.
     */
    public static Map<String, MCRSolrConfigSetProvider> getLocalConfigSets() {
        Map<String, Callable<Object>> instances = MCRConfiguration2.getInstances(CONFIG_SET_PROPERTY_PREFIX);
        Map<String, MCRSolrConfigSetProvider> configSets = new HashMap<>(instances.size());

        instances.forEach((name, supplier) -> {
            try {
                Object cs = supplier.call();
                if (cs instanceof MCRSolrConfigSetProvider configSet) {
                    configSets.put(name.substring(CONFIG_SET_PROPERTY_PREFIX.length()), configSet);
                } else {
                    throw new MCRConfigurationException("Invalid config set instance " + name);
                }
            } catch (Exception e) {
                throw new MCRConfigurationException("Error while initializing config set " + name, e);
            }
        });

        return configSets;
    }

    /**
     * Transfers a config set to the remote Solr server.
     * @param solrServerURL The URL of the Solr server.
     * @param remoteName The name of the config set.
     * @param configSetProvider The config set provider.
     */
    public static void transferConfigSetToRemoteSolrServer(URI solrServerURL,
        String remoteName,
        MCRSolrConfigSetProvider configSetProvider) throws SolrServerException, IOException {
        try {
            List<String> remoteConfigSetNames = getRemoteConfigSetNames(solrServerURL);
            if (remoteConfigSetNames.contains(remoteName)) {
                throw new MCRConfigurationException("Config set " + remoteName + " already exists on the " +
                    "remote Solr server.");
            }
        } catch (IOException | URISyntaxException e) {
            throw new MCRConfigurationException("Error while checking for existing config sets on the remote Solr " +
                "server.", e);
        }

        ConfigSetAdminRequest.Upload request = new ConfigSetAdminRequest.Upload();
        MCRSolrAuthenticationHelper.addAuthentication(request,
            MCRSolrAuthenticationHelper.AuthenticationLevel.ADMIN);

        request.setConfigSetName(remoteName);

        request.setUploadStream(new ContentStreamBase() {
            @Override
            public String getContentType() {
                return "application/octet-stream";
            }

            @Override
            public InputStream getStream()  {
                return configSetProvider.getStreamSupplier().get();
            }
        });

        ConfigSetAdminResponse uploadResponse = request.process(MCRSolrHttpHelper.getSolrClient(solrServerURL));

        if (uploadResponse.getStatus() != 0) {
            throw new MCRConfigurationException("Error while transferring config set to remote Solr server. " +
                "Status code: " + uploadResponse.getStatus() + "\n  " + uploadResponse.getErrorMessages());

        }

        LOGGER.info("Config set {} transferred to remote Solr server.", remoteName);
    }

    /**
     * Deletes a config set from the remote Solr server.
     * @param solrServerURL The URL of the Solr server.
     * @param name The name of the config set.
     */
    public static void deleteConfigSetFromRemoteSolrServer(URI solrServerURL, String name) {
        ConfigSetAdminRequest.Delete request = new ConfigSetAdminRequest.Delete();
        MCRSolrAuthenticationHelper.addAuthentication(request,
            MCRSolrAuthenticationHelper.AuthenticationLevel.ADMIN);

        request.setConfigSetName(name);

        try {
            ConfigSetAdminResponse deleteResponse = request.process(MCRSolrHttpHelper.getSolrClient(solrServerURL));
            if (deleteResponse.getStatus() != 0) {
                throw new MCRConfigurationException("Error while deleting config set from remote Solr server. " +
                    "Status code: " + deleteResponse.getStatus() + "\n  " + deleteResponse.getErrorMessages());
            }
        } catch (IOException | SolrServerException e) {
            throw new MCRConfigurationException("Error while deleting config set from remote Solr server.", e);
        }

        LOGGER.info("Config set {} deleted from remote Solr server.", name);
    }

    /**
     * Returns the config set which is configured for a specific core.
     * @param configCoreName The name of the core.
     * @return The name of the config set.
     */
    public static String getConfigSetForCore(String configCoreName) {
        MCRSolrCore core = MCRSolrClientFactory.get(configCoreName)
                .orElseThrow(() -> MCRSolrUtils.getCoreConfigMissingException(configCoreName));
        return core.getConfigSet();
    }

    /**
     * When a config set is uploaded to the remote Solr server, it is stored under a name that is a combination of the
     * core name and the local config set name. This method constructs the name of the remote config set.
     * This is required because the schema API is used to configure the core further, and it does not modify only the
     * core's configuration, but also the configuration of the config set. Therefore, the config set must be uniquely
     * identifiable.
     *
     * @param remoteCoreName The name of the core on the remote Solr server.
     * @param localConfigSetName The name of the local config set.
     * @return The name of the remote config set.
     */
    public static String buildRemoteConfigSetName(String remoteCoreName, String localConfigSetName) {
        return remoteCoreName + "_" + localConfigSetName;
    }

}
