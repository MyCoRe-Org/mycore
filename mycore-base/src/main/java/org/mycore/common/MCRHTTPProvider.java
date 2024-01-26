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
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.services.http.MCRHttpUtils;

public class MCRHTTPProvider {
	private static org.apache.logging.log4j.Logger logger = LogManager.getLogger();
	
	static class MCRDefaultHTTPClient implements MCRHTTPClient {
		// TODO: subject to change! - stays same for backward-compatible safety
		// tmp. keep the old and add a new prefix?
		private static final String CONFIG_PREFIX = "MCR.URIResolver.";
		
        private static final long MAX_OBJECT_SIZE = MCRConfiguration2.getLong(CONFIG_PREFIX + "REST.MaxObjectSize")
                .orElse(128 * 1024L);

        private static final int MAX_CACHE_ENTRIES = MCRConfiguration2.getInt(CONFIG_PREFIX + "REST.MaxCacheEntries")
                .orElse(1000);

        private static final int REQUEST_TIMEOUT = MCRConfiguration2.getInt(CONFIG_PREFIX + "REST.RequestTimeout")
                .orElse(30000);
		
        CloseableHttpClient restClient;
		
		MCRDefaultHTTPClient() {
            CacheConfig cacheConfig = CacheConfig.custom()
                    .setMaxObjectSize(MAX_OBJECT_SIZE)
                    .setMaxCacheEntries(MAX_CACHE_ENTRIES)
                    .build();
                RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(REQUEST_TIMEOUT)
                    .setSocketTimeout(REQUEST_TIMEOUT)
                    .build();
                this.restClient = CachingHttpClients.custom()
                    .setCacheConfig(cacheConfig)
                    .setDefaultRequestConfig(requestConfig)
                    .setUserAgent(MCRHttpUtils.getHttpUserAgent())
                    .useSystemProperties()
                    .build();
                MCRShutdownHandler.getInstance().addCloseable(this::close);
		}

        public void close() {
            try {
                restClient.close();
            } catch (IOException e) {
                LogManager.getLogger().warn("Exception while closing http client.", e);
            }
        }

		@Override
		public MCRContent get(URI hrefURI) throws IOException {
			HttpCacheContext context = HttpCacheContext.create();
            HttpGet get = new HttpGet(hrefURI);
            try ( 	CloseableHttpResponse response = restClient.execute(get, context);
            		InputStream content = response.getEntity().getContent();
            	) {
            	logger.debug("http query: {}", hrefURI);
            	logger.debug("http resp status: {}", response.getStatusLine());
            	logger.debug(() -> getCacheDebugMsg(hrefURI, context));
            	//resource warning: do not make a one-line return-statement out of the following:
            	MCRContent cnt = (new MCRStreamContent(content)).getReusableCopy();
            	return cnt;
            } finally {
            	get.reset();
            }
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

	public static MCRHTTPClient getMCRHTTPClient() {
		String className = MCRConfiguration2.getString("MCR.HTTPClient.Class").orElse(null);
		if (className == null) {
			return new MCRDefaultHTTPClient();
		} else {
	        try {
	        	MCRHTTPClient cl = (MCRHTTPClient) MCRConfiguration2.getInstanceOf("MCR.HTTPClient.Class").get();
	        	//	.orElseThrow(() -> MCRConfiguration2.createConfigurationException("MCR.HTTPClient.Class"));
	        	return cl;
	        } catch (RuntimeException re) {
	        	throw new MCRException("Cannot instantiate " + className + " ", re);
	        }
		}
	}
}