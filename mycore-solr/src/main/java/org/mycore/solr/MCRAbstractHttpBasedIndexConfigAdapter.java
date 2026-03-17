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

package org.mycore.solr;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClientBuilderBase;
import org.apache.solr.client.solrj.jetty.HttpJettySolrClient;
import org.mycore.common.config.annotation.MCRProperty;

public abstract class MCRAbstractHttpBasedIndexConfigAdapter<T extends MCRSolrIndex> implements Supplier<T> {

    private String coreTypes;

    private String idleTimeout;

    private String idleTimeoutUnit;

    private String connectionTimeout;

    private String connectionTimeoutUnit;

    private String requestTimeout;

    private String requestTimeoutUnit;

    private String useHttp11;

    private boolean useJettyHttpClient;

    public String getCoreTypes() {
        return coreTypes;
    }

    @MCRProperty(name = "CoreTypes")
    public void setCoreTypes(String coreTypes) {
        this.coreTypes = coreTypes;
    }

    public String getIdleTimeout() {
        return idleTimeout;
    }

    @MCRProperty(name = "IdleTimeout", defaultName = MCRSolrDefaultPropertyConstants.CLIENT_IDLE_TIMEOUT,
        required = false)
    public void setIdleTimeout(String idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public String getIdleTimeoutUnit() {
        return idleTimeoutUnit;
    }

    @MCRProperty(name = "IdleTimeout.Unit", defaultName = MCRSolrDefaultPropertyConstants.CLIENT_IDLE_TIMEOUT_UNIT,
        required = false)
    public void setIdleTimeoutUnit(String idleTimeoutUnit) {
        this.idleTimeoutUnit = idleTimeoutUnit;
    }

    public String getConnectionTimeout() {
        return connectionTimeout;
    }

    @MCRProperty(name = "ConnectionTimeout", defaultName = MCRSolrDefaultPropertyConstants.CLIENT_CONNECTION_TIMEOUT,
        required = false)
    public void setConnectionTimeout(String connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public String getConnectionTimeoutUnit() {
        return connectionTimeoutUnit;
    }

    @MCRProperty(name = "ConnectionTimeout.Unit",
        defaultName = MCRSolrDefaultPropertyConstants.CLIENT_CONNECTION_TIMEOUT_UNIT,
        required = false)
    public void setConnectionTimeoutUnit(String connectionTimeoutUnit) {
        this.connectionTimeoutUnit = connectionTimeoutUnit;
    }

    public String getRequestTimeout() {
        return requestTimeout;
    }

    @MCRProperty(name = "RequestTimeout", defaultName = MCRSolrDefaultPropertyConstants.CLIENT_REQUEST_TIMEOUT,
        required = false)
    public void setRequestTimeout(String requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public String getRequestTimeoutUnit() {
        return requestTimeoutUnit;
    }

    @MCRProperty(name = "RequestTimeout.Unit",
        defaultName = MCRSolrDefaultPropertyConstants.CLIENT_REQUEST_TIMEOUT_UNIT,
        required = false)
    public void setRequestTimeoutUnit(String requestTimeoutUnit) {
        this.requestTimeoutUnit = requestTimeoutUnit;
    }

    public String getUseHttp11() {
        return useHttp11;
    }

    public void setUseHttp11(String useHttp11) {
        this.useHttp11 = useHttp11;
    }

    @MCRProperty(name = "UseJettyHttpClient", defaultName = MCRSolrDefaultPropertyConstants.USE_JETTY_HTTP_CLIENT)
    public void setUseJettyHttpClient(String useJettyHttpClient) {
        this.useJettyHttpClient = Boolean.parseBoolean(useJettyHttpClient);
    }

    public boolean useJettyHttpClient() {
        return useJettyHttpClient;
    }

    protected HttpSolrClientBuilderBase getBuilder() {
        return useJettyHttpClient() ? new HttpJettySolrClient.Builder()
            : new HttpJdkSolrClient.Builder();
    }

    protected void applySettings(HttpSolrClientBuilderBase builder) {

        String idleTimeout = getIdleTimeout();
        String idleTimeoutUnit = getIdleTimeoutUnit();
        if (idleTimeout != null && idleTimeoutUnit != null) {
            builder.withIdleTimeout(Long.parseLong(idleTimeout), parseTimeUnit(idleTimeoutUnit));
        }

        String connectionTimeout = getConnectionTimeout();
        String connectionTimeoutUnit = getConnectionTimeoutUnit();
        if (connectionTimeout != null && connectionTimeoutUnit != null) {
            builder.withConnectionTimeout(Long.parseLong(connectionTimeout),
                parseTimeUnit(connectionTimeoutUnit));
        }

        String requestTimeout = getRequestTimeout();
        String requestTimeoutUnit = getRequestTimeoutUnit();
        if (requestTimeout != null && requestTimeoutUnit != null) {
            builder.withRequestTimeout(Long.parseLong(requestTimeout), parseTimeUnit(requestTimeoutUnit));

        }

        String useHttp11 = getUseHttp11();
        if (useHttp11 != null) {
            builder.useHttp1_1(Boolean.parseBoolean(useHttp11));
        }

    }

    protected Set<MCRIndexType> buildCoreTypes() {
        return Stream.of(coreTypes.split(","))
            .map(String::trim)
            .map(MCRIndexType::new)
            .collect(java.util.stream.Collectors.toSet());
    }

    private TimeUnit parseTimeUnit(String timeUnit) {
        try {
            return TimeUnit.valueOf(timeUnit);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid time unit: " + timeUnit, e);
        }
    }

}
