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
import org.apache.solr.client.solrj.impl.ConcurrentUpdateBaseSolrClient;
import org.apache.solr.client.solrj.impl.HttpClusterStateProvider;
import org.apache.solr.client.solrj.impl.HttpSolrClientBuilderBase;
import org.apache.solr.client.solrj.jetty.HttpJettySolrClient;
import org.mycore.common.MCRException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.solr.MCRAbstractHttpBasedIndexConfigAdapter;
import org.mycore.solr.MCRIndexType;
import org.mycore.solr.MCRSolrUtils;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;
import org.mycore.solr.cloud.collection.MCRConfigurableSolrCloudCollection.ConfigAdapter;

@MCRConfigurationProxy(proxyClass = ConfigAdapter.class)
public class MCRConfigurableSolrCloudCollection implements MCRSolrCloudCollection {

    private final CloudSolrClient client;
    private final CloudSolrClient baseClient;
    private final String collectionName;
    private final Set<MCRIndexType> coreTypes;

    private final Integer numShards;
    private final Integer numNrtReplicas;
    private final Integer numTlogReplicas;
    private final Integer numPullReplicas;

    private final String configSetTemplate;

    public MCRConfigurableSolrCloudCollection(CloudSolrClient client, CloudSolrClient baseClient,
        String collectionName, Set<MCRIndexType> coreTypes, Integer numShards,
        Integer numNrtReplicas, Integer numTlogReplicas, Integer numPullReplicas,
        String configSetTemplate) {
        this.client = client;
        this.baseClient = baseClient;
        this.collectionName = collectionName;
        this.coreTypes = coreTypes;
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
    public Optional<ConcurrentUpdateBaseSolrClient> getConcurrentClient() {
        // You should use the normal Cloud Client, when using SolrCloud
        return Optional.empty();
    }

    @Override
    public Set<MCRIndexType> getCoreTypes() {
        return coreTypes;
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
        MCRAbstractHttpBasedIndexConfigAdapter<MCRSolrCloudCollection> {

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
                List<String> zkUrlList = List.of(zkUrls.split(","));
                builder = new CloudHttp2SolrClient.Builder(zkUrlList, Optional.ofNullable(zkChroot));
            } else if (solrUrls != null && !solrUrls.isEmpty()) {
                builder = getURLBaseCloudHttp2SolrClientBuilder();
            } else {
                throw new IllegalStateException(
                    "No Solr URLs or Zookeeper URL configured for SolrCloud collection " + collectionName);
            }

            HttpSolrClientBuilderBase baseClientBuilder = getBuilder();

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
                List<String> solrUrlList = List.of(solrUrls.split(","));
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
                buildCoreTypes(),
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
