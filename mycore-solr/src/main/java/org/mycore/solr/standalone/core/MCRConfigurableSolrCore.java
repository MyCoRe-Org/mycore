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

package org.mycore.solr.standalone.core;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateBaseSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClientBase;
import org.apache.solr.client.solrj.impl.HttpSolrClientBuilderBase;
import org.apache.solr.client.solrj.jetty.ConcurrentUpdateJettySolrClient.Builder;
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
    private final Set<MCRIndexType> indexTypes;

    MCRConfigurableSolrCore(HttpSolrClientBase client, HttpSolrClientBase baseClient,
        ConcurrentUpdateBaseSolrClient concurrentClient, String coreName,
        Set<MCRIndexType> indexTypes) {
        this.client = client;
        this.baseClient = baseClient;
        this.concurrentClient = concurrentClient;
        this.coreName = coreName;
        this.indexTypes = indexTypes;
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
    public Set<MCRIndexType> getIndexTypes() {
        return indexTypes;
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
        private Integer concurrentQueueSize;
        private Integer concurrentThreadCount;
        private boolean concurrentEnabled;

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

        public Integer getConcurrentQueueSize() {
            return concurrentQueueSize;
        }

        @MCRProperty(name = "Concurrent.QueueSize", required = false,
            defaultName = "MCR.Solr.Default.ConcurrentClient.QueueSize")
        public void setConcurrentQueueSize(String concurrentQueueSize) {
            this.concurrentQueueSize = Integer.parseInt(concurrentQueueSize);
        }

        public Integer getConcurrentThreadCount() {
            return concurrentThreadCount;
        }

        @MCRProperty(name = "Concurrent.ThreadCount", required = false,
            defaultName = "MCR.Solr.Default.ConcurrentClient.ThreadCount")
        public void setConcurrentThreadCount(String concurrentThreadCount) {
            this.concurrentThreadCount = Integer.parseInt(concurrentThreadCount);
        }

        public boolean isConcurrentEnabled() {
            return concurrentEnabled;
        }

        @MCRProperty(name = "Concurrent.Enabled", required = false,
            defaultName = "MCR.Solr.Default.ConcurrentClient.Enabled")
        public void setConcurrentEnabled(String concurrentEnabled) {
            this.concurrentEnabled = Boolean.parseBoolean(concurrentEnabled);
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

            ConcurrentUpdateBaseSolrClient concurrentClient = null;

            if (this.isConcurrentEnabled() && useJettyHttpClient()) {
                if (client instanceof HttpJettySolrClient jc) {
                    Builder concurrentBuilder = new Builder(normalizedSolrUrl, jc);

                    concurrentBuilder.withThreadCount(getConcurrentThreadCount());
                    concurrentBuilder.withQueueSize(getConcurrentQueueSize());

                    concurrentClient = concurrentBuilder.build();
                }
            }

            return new MCRConfigurableSolrCore(client, base, concurrentClient, getCoreName(), buildIndexTypes());
        }

    }
}
