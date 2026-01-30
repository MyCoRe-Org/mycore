package org.mycore.solr.standalone.core;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateBaseSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClientBase;
import org.apache.solr.client.solrj.impl.HttpSolrClientBuilderBase;
import org.apache.solr.client.solrj.jetty.ConcurrentUpdateJettySolrClient;
import org.apache.solr.client.solrj.jetty.HttpJettySolrClient;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.solr.MCRAbstractHttpBasedIndexConfigAdapter;
import org.mycore.solr.MCRIndexType;
import org.mycore.solr.MCRSolrIndex;
import org.mycore.solr.MCRSolrUtils;
import org.mycore.solr.standalone.core.MCRConfigurableSolrCore.ConfigAdapter;

@MCRConfigurationProxy(proxyClass = ConfigAdapter.class)
public class MCRConfigurableSolrCore implements MCRSolrIndex {

    private final String coreName;
    private final HttpSolrClientBase client;
    private final HttpSolrClientBase baseClient;
    private final ConcurrentUpdateBaseSolrClient concurrentClient;
    private final Set<MCRIndexType> coreTypes;

    MCRConfigurableSolrCore(HttpSolrClientBase client, HttpSolrClientBase baseClient,
        ConcurrentUpdateBaseSolrClient concurrentClient, String coreName,
        Set<MCRIndexType> coreTypes) {
        this.client = client;
        this.baseClient = baseClient;
        this.concurrentClient = concurrentClient;
        this.coreName = coreName;
        this.coreTypes = coreTypes;
    }

    @Override
    public String getName() {
        return coreName;
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
        return Optional.ofNullable(this.concurrentClient);
    }

    @Override
    public Set<MCRIndexType> getCoreTypes() {
        return coreTypes;
    }

    @Override
    public void close() throws IOException {
        MCRSolrUtils.shutdownSolrClient(this.client);
        MCRSolrUtils.shutdownSolrClient(this.baseClient);
        MCRSolrUtils.shutdownSolrClient(concurrentClient);
    }

    public static class ConfigAdapter extends
        MCRAbstractHttpBasedIndexConfigAdapter<MCRConfigurableSolrCore> {

        private String solrUrl;
        private String coreName;

        public String getCoreName() {
            return coreName;
        }

        @MCRProperty(name = "CoreName")
        public void setCoreName(String coreName) {
            this.coreName = coreName;
        }

        public String getSolrUrl() {
            return solrUrl;
        }

        @MCRProperty(name = "SolrUrl")
        public void setSolrUrl(String solrUrl) {
            this.solrUrl = solrUrl;
        }

        @Override
        public MCRConfigurableSolrCore get() {

            String normalizedSolrUrl = getSolrUrl();

            if (!normalizedSolrUrl.endsWith("/")) {
                normalizedSolrUrl += "/";
            }

            HttpSolrClientBuilderBase builder = getBuilder();

            applySettings(builder);

            HttpSolrClientBase base = builder
                .withBaseSolrUrl(normalizedSolrUrl + "solr/")
                .build();

            HttpSolrClientBase client = builder
                .withBaseSolrUrl(normalizedSolrUrl + "solr/")
                .withDefaultCollection(getCoreName())
                .build();

            ConcurrentUpdateBaseSolrClient concurrentClient =
                useJettyHttpClient() && client instanceof HttpJettySolrClient jc
                    ? new ConcurrentUpdateJettySolrClient.Builder(normalizedSolrUrl, jc).build() : null;

            return new MCRConfigurableSolrCore(client, base, concurrentClient, getCoreName(), buildCoreTypes());
        }

    }
}
