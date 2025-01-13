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

package org.mycore.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.cache.HttpCacheContext;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.cache.CacheConfig;
import org.apache.hc.client5.http.impl.cache.CachingHttpClients;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.services.http.MCRHttpUtils;

public class MCRDefaultHTTPClient implements MCRHTTPClient {
    private static Logger logger = LogManager.getLogger();
    @SuppressWarnings("PMD.SingularField")
    private long maxObjectSize;
    @SuppressWarnings("PMD.SingularField")
    private int maxCacheEntries;
    @SuppressWarnings("PMD.SingularField")
    private int requestTimeout;

    private CloseableHttpClient restClient;

    public MCRDefaultHTTPClient() {
        CacheConfig cacheConfig = CacheConfig.custom()
            .setMaxObjectSize(maxObjectSize)
            .setMaxCacheEntries(maxCacheEntries)
            .build();
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
            .setConnectTimeout(requestTimeout, TimeUnit.MILLISECONDS)
            .setSocketTimeout(requestTimeout, TimeUnit.MILLISECONDS)
            .build();
        HttpClientConnectionManager connManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultConnectionConfig(connectionConfig)
            .build();
        this.restClient = CachingHttpClients.custom()
            .setCacheConfig(cacheConfig)
            .setConnectionManager(connManager)
            .setUserAgent(MCRHttpUtils.getHttpUserAgent())
            .useSystemProperties()
            .build();
        MCRShutdownHandler.getInstance().addCloseable(this::close);
    }

    @MCRProperty(name = "MaxObjectSize")
    public void setMaxObjectSize(String size) {
        this.maxObjectSize = Long.parseLong(size);
    }

    @MCRProperty(name = "MaxCacheEntries")
    public void setMaxCacheEntries(String size) {
        this.maxCacheEntries = Integer.parseInt(size);
    }

    @MCRProperty(name = "RequestTimeout")
    public void setRequestTimeout(String size) {
        this.requestTimeout = Integer.parseInt(size);
    }

    @Override
    public void close() {
        try {
            restClient.close();
        } catch (IOException e) {
            logger.warn("Exception while closing http client.", e);
        }
    }

    @Override
    public MCRContent get(URI hrefURI) throws IOException {
        HttpCacheContext context = HttpCacheContext.create();
        HttpGet get = new HttpGet(hrefURI);
        MCRContent retContent = restClient.execute(get, context, new AbstractHttpClientResponseHandler<MCRContent>() {
            @Override
            public MCRContent handleResponse(ClassicHttpResponse response) throws IOException {
                logger.debug("http query: {}", hrefURI);
                logger.debug("http resp status: {} {}", response.getCode(), response.getReasonPhrase());
                logger.debug(() -> getCacheDebugMsg(hrefURI, context));
                return super.handleResponse(response);
            }

            @Override
            public MCRContent handleEntity(HttpEntity entity) throws IOException {
                try (InputStream content = entity.getContent()) {
                    return new MCRStreamContent(content, hrefURI.toString()).getReusableCopy();
                }
            }
        });
        return retContent;
    }

    private String getCacheDebugMsg(URI hrefURI, HttpCacheContext context) {
        return hrefURI.toASCIIString() + ": " +
            switch (context.getCacheResponseStatus()) {
                case CACHE_HIT -> "A response was generated from the cache with no requests sent upstream";
                case CACHE_MODULE_RESPONSE -> "The response was generated directly by the caching module";
                case CACHE_MISS -> "The response came from an upstream server";
                case VALIDATED -> "The response was generated from the cache after validating the entry "
                    + "with the origin server";
                case FAILURE -> "The response came from an upstream server after a cache failure";
            };
    }
}
