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

package org.mycore.solr;

import static org.mycore.solr.MCRSolrConstants.SOLR_CONFIG_PREFIX;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;

/**
 * Core instance of a solr server.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrCore {

    private static final Logger LOGGER = LogManager.getLogger(MCRSolrCore.class);

    public static final int DEFAULT_SHARD_COUNT = 1;

    private static boolean USE_CONCURRENT_SERVER;

    protected String serverURL;

    protected String name;

    protected String configSet;

    protected Integer shardCount;

    // todo: maybe add support for replicaCount and compositeId if required

    protected HttpSolrClient solrClient;

    protected HttpSolrClient baseSolrClient;

    protected ConcurrentUpdateSolrClient concurrentClient;

    static {
        USE_CONCURRENT_SERVER = MCRConfiguration2
            .getOrThrow(SOLR_CONFIG_PREFIX + "ConcurrentUpdateSolrClient.Enabled", Boolean::parseBoolean);
    }

    private Set<MCRSolrCoreType> types;

    /**
     * Creates a new solr server core instance.
     * 
     * @param serverURL
     *            base url of the solr server e.g. http://localhost:8296
     * @param name
     *            name of the core e.g. docportal
     * @deprecated use {@link #MCRSolrCore(String, String, String, Integer, Set)} instead
     */
    @Deprecated
    public MCRSolrCore(String serverURL, String name) {

        setup(serverURL, name, null, DEFAULT_SHARD_COUNT, name.equals("classification") ?
                Set.of(MCRSolrCoreType.CLASSIFICATION) : Set.of(MCRSolrCoreType.MAIN));
    }

    /**
     * Creates a new solr server core instance.
     *
     * @param serverURL
     *            base url of the solr server e.g. http://localhost:8296
     * @param name
     *            name of the core e.g. docportal
     * @param configSet
     *            name of the config set
     * @param shardCount
     *            number of shards
     */
    public MCRSolrCore(String serverURL, String name, String configSet, Integer shardCount, Set<MCRSolrCoreType> type) {
        setup(serverURL, name, configSet, shardCount, type);
    }

    protected void setup(String serverURL, String name, String configSet,
                         Integer shardCount, Set<MCRSolrCoreType> type) {
        if (!serverURL.endsWith("/")) {
            serverURL += "/";
        }
        this.serverURL = serverURL;
        this.name = name;
        this.configSet = configSet;
        this.shardCount = Objects.requireNonNull(shardCount, "shardCount must not be null");
        this.types = new LinkedHashSet<>(Objects.requireNonNull(type, "type must not be null"));
        String coreURL = getV1CoreURL();
        int connectionTimeout = MCRConfiguration2
            .getOrThrow(SOLR_CONFIG_PREFIX + "SolrClient.ConnectionTimeout", Integer::parseInt);
        int socketTimeout = MCRConfiguration2
            .getOrThrow(SOLR_CONFIG_PREFIX + "SolrClient.SocketTimeout", Integer::parseInt);

        // default server
        solrClient = new HttpSolrClient.Builder(coreURL)
            .withConnectionTimeout(connectionTimeout)
            .withSocketTimeout(socketTimeout)
            .build();
        solrClient.setRequestWriter(new BinaryRequestWriter());

        baseSolrClient = new HttpSolrClient.Builder(getServerURL() + "solr/")
                .withConnectionTimeout(connectionTimeout)
                .withSocketTimeout(socketTimeout)
                .build();
        baseSolrClient.setRequestWriter(new BinaryRequestWriter());

        // concurrent server
        if (USE_CONCURRENT_SERVER) {
            int queueSize = MCRConfiguration2
                .getOrThrow(SOLR_CONFIG_PREFIX + "ConcurrentUpdateSolrClient.QueueSize", Integer::parseInt);
            int threadCount = MCRConfiguration2
                .getOrThrow(SOLR_CONFIG_PREFIX + "ConcurrentUpdateSolrClient.ThreadCount", Integer::parseInt);
            concurrentClient = new ConcurrentUpdateSolrClient.Builder(coreURL)
                .withQueueSize(queueSize)
                .withConnectionTimeout(connectionTimeout)
                .withSocketTimeout(socketTimeout)
                .withThreadCount(threadCount)
                .build();
            concurrentClient.setRequestWriter(new BinaryRequestWriter());
        }
        // shutdown handler
        MCRShutdownHandler.getInstance().addCloseable(new MCRShutdownHandler.Closeable() {

            @Override
            public int getPriority() {
                return Integer.MIN_VALUE + 5;
            }

            @Override
            public void close() {
                shutdown();
            }
        });
    }

    public String getV1CoreURL() {
        return this.serverURL + "solr/" + this.name;
    }

    public synchronized void shutdown() {
        try {
            shutdownGracefully(solrClient, true);
            solrClient = null;
        } catch (SolrServerException | IOException e) {
            LOGGER.error("Error while shutting down SOLR client.", e);
        }
        try {
            shutdownGracefully(baseSolrClient, false);
            baseSolrClient = null;
        } catch (SolrServerException | IOException e) {
            LOGGER.error("Error while shutting down SOLR client.", e);
        }
        try {
            shutdownGracefully(concurrentClient, true);
            concurrentClient = null;
        } catch (SolrServerException | IOException e) {
            LOGGER.error("Error while shutting down SOLR client.", e);
        }
        LOGGER.info("Solr shutdown process completed.");
    }

    private void shutdownGracefully(SolrClient client, boolean commit) throws SolrServerException, IOException {
        if (client != null) {
            LOGGER.info("Shutting down solr client: {}", client);
            if (commit) {
                UpdateRequest updateRequest = new UpdateRequest();
                updateRequest.setAction(UpdateRequest.ACTION.COMMIT, false, false);
                MCRSolrAuthenticationManager.getInstance().applyAuthentication(updateRequest,
                    MCRSolrAuthenticationLevel.INDEX);
                updateRequest.process(client);
            }
            client.close();
        }
    }

    /**
     * Returns the name of the core.
     */
    public String getName() {
        return name;
    }

    public String getServerURL() {
        return serverURL;
    }

    /**
     * Returns the default solr client instance. Use this for queries.
     */
    public HttpSolrClient getClient() {
        return solrClient;
    }

    /**
     * Returns the base solr client instance, without core information. Use this for admin operations.
     */
    public HttpSolrClient getBaseClient() {
        return baseSolrClient;
    }

    public String buildRemoteConfigSetName() {
        return this.getName() + "_" + this.getConfigSet();
    }

    /**
     * Returns the concurrent solr client instance. Use this for indexing.
     */
    public SolrClient getConcurrentClient() {
        return concurrentClient != null ? concurrentClient : solrClient;
    }

    /**
     * Returns the ConfigSet assigned in the properties
     * @return the ConfigSet
     */
    public String getConfigSet() {
        return configSet;
    }

    /**
     * Returns the shard count assigned in the properties
     * @return the shard count
     */
    public Integer getShardCount() {
        return shardCount;
    }

    /**
     * Sets the shard count, it does not change an existing core, but is used for creating a new core.
     * @param shardCount the new shard count
     */
    public void setShardCount(Integer shardCount) {
        this.shardCount = shardCount;
    }

    /**
     * Sets the ConfigSet, it does not change an existing core, but is used for creating a new core.
     * @param configSet the new ConfigSet
     */
    public void setConfigSet(String configSet) {
        this.configSet = configSet;
    }

    /**
     * Sets the server URL, it does not change an existing core, but is used for creating a new core.
     * @param serverURL the new server URL
     */
    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    /**
     * Returns which type of core this is
     * @return the type of core
     */
    public Set<MCRSolrCoreType> getTypes() {
        return types;
    }
}
