package org.mycore.solr.proxy;

import java.net.URI;
import java.text.MessageFormat;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRCoreVersion;

class MCRSolrProxyUtils {

    private MCRSolrProxyUtils() {
    }

    static DefaultHttpClient getHttpClient(int maxConnections) {
        //configure connection manager
        PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(maxConnections);
        connectionManager.setMaxTotal(maxConnections);

        //setup http client
        DefaultHttpClient defaultHttpClient = new DefaultHttpClient(connectionManager);
        defaultHttpClient.setHttpRequestRetryHandler(new MCRSolrRetryHandler(maxConnections));
        String userAgent = MessageFormat.format("MyCoRe/{0} ({1}; java {2})", MCRCoreVersion.getCompleteVersion(),
            MCRConfiguration.instance().getString("MCR.NameOfProject"), System.getProperty("java.version"));
        defaultHttpClient.getParams().setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, "UTF-8")
            .setParameter(CoreProtocolPNames.USER_AGENT, userAgent)
            .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
            .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false);
        return defaultHttpClient;
    }

    static HttpHost getHttpHost(String serverUrl) {
        HttpHost host = null;
        //determine host name
        HttpGet serverGet = new HttpGet(serverUrl);
        final URI requestURI = serverGet.getURI();
        if (requestURI.isAbsolute()) {
            host = URIUtils.extractHost(requestURI);
        }
        return host;
    }

}
