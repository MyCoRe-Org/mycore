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

import static org.mycore.solr.MCRSolrDefaultPropertyConstants.DEFAULT_REPLICA_COUNT;
import static org.mycore.solr.MCRSolrDefaultPropertyConstants.DEFAULT_SHARD_COUNT;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudHttp2SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClusterStateProvider;
import org.apache.solr.client.solrj.impl.HttpSolrClientBuilderBase;
import org.apache.solr.client.solrj.jetty.HttpJettySolrClient;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.solr.MCRAbstractSolrHttpBasedIndexConfigAdapter;
import org.mycore.solr.MCRSolrIndexType;
import org.mycore.solr.MCRSolrUtils;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;
import org.mycore.solr.cloud.collection.MCRConfigurableSolrCloudCollection.ConfigAdapter;

@MCRConfigurationProxy(proxyClass = ConfigAdapter.class)
public class MCRConfigurableSolrCloudCollection implements MCRSolrCloudCollection {

    private final CloudSolrClient client;
    private final CloudSolrClient baseClient;
    private final String collectionName;
    private final Set<MCRSolrIndexType> indexTypes;

    private final Integer numShards;
    private final Integer numNrtReplicas;
    private final Integer numTlogReplicas;
    private final Integer numPullReplicas;

    private final String configSetTemplate;

    public MCRConfigurableSolrCloudCollection(CloudSolrClient client, CloudSolrClient baseClient,
        String collectionName, Set<MCRSolrIndexType> indexTypes, Integer numShards,
        Integer numNrtReplicas, Integer numTlogReplicas, Integer numPullReplicas,
        String configSetTemplate) {
        this.client = client;
        this.baseClient = baseClient;
        this.collectionName = collectionName;
        this.indexTypes = indexTypes;
        this.numShards = numShards;
        this.numNrtReplicas = numNrtReplicas;
        this.numTlogReplicas = numTlogReplicas;
        this.numPullReplicas = numPullReplicas;
        this.configSetTemplate = configSetTemplate;
    }

    @Override
    public String getName() {
        return this.collectionName;
    }

    @Override
    public SolrClient getClient() {
        return client;
    }

    @Override
    public SolrClient getBaseClient() {
        return baseClient;
    }

    @Override
    public Set<MCRSolrIndexType> getIndexTypes() {
        return indexTypes;
    }

    @Override
    public void close() throws IOException {
        MCRSolrUtils.shutdownSolrClient(this.client);
        MCRSolrUtils.shutdownSolrClient(this.baseClient);
    }

    @Override
    public Integer getNumShards() {
        return numShards;
    }

    @Override
    public Integer getNumNrtReplicas() {
        return numNrtReplicas;
    }

    @Override
    public Integer getNumTlogReplicas() {
        return numTlogReplicas;
    }

    @Override
    public Integer getNumPullReplicas() {
        return numPullReplicas;
    }

    @Override
    public String getConfigSetTemplate() {
        return configSetTemplate;
    }

    public static class ConfigAdapter extends
        MCRAbstractSolrHttpBasedIndexConfigAdapter<MCRSolrCloudCollection> {

        private String solrUrls;
        private String zkUrls;
        private String zkChroot;
        private String collectionName;

        private String numShards;
        private String numNrtReplicas;
        private String numTlogReplicas;
        private String numPullReplicas;
        private String configSetTemplate;

        public String getNumShards() {
            return numShards;
        }

        @MCRProperty(name = "NumShards", defaultName = DEFAULT_SHARD_COUNT)
        public void setNumShards(String numShards) {
            this.numShards = numShards;
        }

        public String getNumNrtReplicas() {
            return numNrtReplicas;
        }

        @MCRProperty(name = "NumNrtReplicas", defaultName = DEFAULT_REPLICA_COUNT)
        public void setNumNrtReplicas(String numNrtReplicas) {
            this.numNrtReplicas = numNrtReplicas;
        }

        public String getNumTlogReplicas() {
            return numTlogReplicas;
        }

        @MCRProperty(name = "NumTlogReplicas", required = false)
        public void setNumTlogReplicas(String numTlogReplicas) {
            this.numTlogReplicas = numTlogReplicas;
        }

        public String getNumPullReplicas() {
            return numPullReplicas;
        }

        @MCRProperty(name = "NumPullReplicas", required = false)
        public void setNumPullReplicas(String numPullReplicas) {
            this.numPullReplicas = numPullReplicas;
        }

        public String getCollectionName() {
            return collectionName;
        }

        @MCRProperty(name = "CollectionName")
        public void setCollectionName(String collectionName) {
            this.collectionName = collectionName;
        }

        public String getZkChroot() {
            return zkChroot;
        }

        @MCRProperty(name = "ZkChroot", required = false)
        public void setZkChroot(String zkChroot) {
            this.zkChroot = zkChroot;
        }

        public String getZkUrl() {
            return zkUrls;
        }

        @MCRProperty(name = "ZkUrls", required = false)
        public void setZkUrl(String zkUrl) {
            this.zkUrls = zkUrl;
        }

        public String getSolrUrls() {
            return solrUrls;
        }

        @MCRProperty(name = "SolrUrls", required = false)
        public void setSolrUrls(String solrUrls) {
            this.solrUrls = solrUrls;
        }

        private ClientPair buildClient() {
            CloudHttp2SolrClient.Builder builder;
            if (zkUrls != null && !zkUrls.isEmpty()) {
                List<String> zkUrlList = MCRConfiguration2.splitValue(zkUrls).toList();
                builder = new CloudHttp2SolrClient.Builder(zkUrlList, Optional.ofNullable(zkChroot));
            } else if (solrUrls != null && !solrUrls.isEmpty()) {
                builder = getURLBaseCloudHttp2SolrClientBuilder();
            } else {
                throw new IllegalStateException(
                    "No Solr URLs or Zookeeper URL configured for SolrCloud collection " + collectionName);
            }

            HttpSolrClientBuilderBase<?, ?> baseClientBuilder = getBuilder();

            applySettings(baseClientBuilder);

            if (!(baseClientBuilder instanceof HttpJettySolrClient.Builder http2Builder)) {
                throw new IllegalStateException(
                    "Only HttpJettySolrClient.Builder is supported for SolrCloud collections");
            }
            builder.withHttpClientBuilder(http2Builder);

            CloudHttp2SolrClient baseClient = builder.build();
            CloudHttp2SolrClient client = builder.withDefaultCollection(getCollectionName()).build();

            return new ClientPair(client, baseClient);
        }

        private CloudHttp2SolrClient.Builder getURLBaseCloudHttp2SolrClientBuilder() {
            // the Http2ClusterStateProvider requires a Http2SolrClient to fetch the cluster state,
            // the Client needs admin level authentication to fetch the cluster state
            try {
                List<String> solrUrlList = MCRConfiguration2.splitValue(solrUrls)
                    .map(String::trim)
                    .map(p -> p.endsWith("/") ? p : p + "/")
                    .map(p -> p.endsWith("solr/") ? p : p + "solr/")
                    .toList();
                HttpJettySolrClient.Builder clusterStateHttpClientBuilder = new HttpJettySolrClient.Builder();
                MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(clusterStateHttpClientBuilder,
                    MCRSolrAuthenticationLevel.ADMIN);
                HttpJettySolrClient clusterStateHttpClient = clusterStateHttpClientBuilder.build();
                HttpClusterStateProvider<HttpJettySolrClient> clusterStateProvider =
                    new HttpClusterStateProvider<>(solrUrlList, clusterStateHttpClient);
                return new CloudHttp2SolrClient.Builder(clusterStateProvider);
            } catch (Exception e) {
                throw new MCRException("Error building SolrCloud client for collection " + collectionName, e);
            }
        }

        @Override
        public MCRConfigurableSolrCloudCollection get() {
            ClientPair clientPair = buildClient();

            return new MCRConfigurableSolrCloudCollection(
                clientPair.client,
                clientPair.baseClient,
                getCollectionName(),
                buildIndexTypes(),
                numShards != null ? Integer.parseInt(numShards) : null,
                numNrtReplicas != null ? Integer.parseInt(numNrtReplicas) : null,
                numTlogReplicas != null ? Integer.parseInt(numTlogReplicas) : null,
                numPullReplicas != null ? Integer.parseInt(numPullReplicas) : null,
                getConfigSetTemplate());
        }

        public String getConfigSetTemplate() {
            return configSetTemplate;
        }

        @MCRProperty(name = "ConfigSetTemplate")
        public void setConfigSetTemplate(String configSetTemplate) {
            this.configSetTemplate = configSetTemplate;
        }

        private record ClientPair(CloudHttp2SolrClient client, CloudHttp2SolrClient baseClient) {
        }
    }
}
