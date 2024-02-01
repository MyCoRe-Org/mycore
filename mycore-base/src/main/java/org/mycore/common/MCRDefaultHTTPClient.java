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

package org.mycore.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.http.client.cache.HttpCacheContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.services.http.MCRHttpUtils;

public class MCRDefaultHTTPClient implements MCRHTTPClient {
    private static Logger logger = LogManager.getLogger();

    private long maxObjectSize;

    private int maxCacheEntries;

    private int requestTimeout;

    private CloseableHttpClient restClient;

    public MCRDefaultHTTPClient() {
        CacheConfig cacheConfig = CacheConfig.custom()
            .setMaxObjectSize(maxObjectSize)
            .setMaxCacheEntries(maxCacheEntries)
            .build();
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(requestTimeout)
            .setSocketTimeout(requestTimeout)
            .build();
        this.restClient = CachingHttpClients.custom()
            .setCacheConfig(cacheConfig)
            .setDefaultRequestConfig(requestConfig)
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
        MCRContent retContent = null;
        try (CloseableHttpResponse response = restClient.execute(get, context);
            InputStream content = response.getEntity().getContent();) {
            logger.debug("http query: {}", hrefURI);
            logger.debug("http resp status: {}", response.getStatusLine());
            logger.debug(() -> getCacheDebugMsg(hrefURI, context));
            retContent = (new MCRStreamContent(content)).getReusableCopy();
        } finally {
            get.reset();
        }
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
            };
    }
}
