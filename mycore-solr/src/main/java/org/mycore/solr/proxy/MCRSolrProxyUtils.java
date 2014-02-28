package org.mycore.solr.proxy;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.NamedList;
import org.mycore.common.MCRCoreVersion;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.solr.MCRSolrConstants;
import org.mycore.solr.MCRSolrServerFactory;

class MCRSolrProxyUtils {

    private static final SolrQuery mbeansQuery = new SolrQuery().setParam("cat", "QUERYHANDLER")
        .setParam(CommonParams.OMIT_HEADER, true).setRequestHandler("/admin/mbeans");

    private static final Logger LOGGER = Logger.getLogger(MCRSolrProxyUtils.class);

    private MCRSolrProxyUtils() {
    }

    static CloseableHttpClient getHttpClient(HttpClientConnectionManager connectionManager, int maxConnections) {

        RequestConfig requestConfig = RequestConfig.custom().setStaleConnectionCheckEnabled(false)
            .setConnectTimeout(30000).setSocketTimeout(30000).build();

        ConnectionConfig connectionConfig = ConnectionConfig.custom().setCharset(Charset.forName("UTF-8")).build();
        SocketConfig socketConfig = SocketConfig.custom().setTcpNoDelay(true).setSoKeepAlive(true)
            .setSoReuseAddress(true).build();

        String userAgent = MessageFormat
            .format("MyCoRe/{0} ({1}; java {2})", MCRCoreVersion.getCompleteVersion(), MCRConfiguration.instance()
                .getString("MCR.NameOfProject", "undefined"), System.getProperty("java.version"));
        //setup http client
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager)
            .setUserAgent(userAgent).setRetryHandler(new MCRSolrRetryHandler(maxConnections))
            .setDefaultRequestConfig(requestConfig).setDefaultConnectionConfig(connectionConfig)
            .setDefaultSocketConfig(socketConfig).build();
        return httpClient;
    }

    static PoolingHttpClientConnectionManager getConnectionManager(int maxConnections) {
        //configure connection manager
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(maxConnections);
        connectionManager.setMaxTotal(maxConnections);
        return connectionManager;
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

    static NamedList<NamedList<Object>> getQueryHandlerList() throws SolrServerException, IOException {
        SolrServer solrServer = MCRSolrServerFactory.getSolrServer();
        SolrRequest request = new QueryRequest(mbeansQuery);
        NamedList<Object> response = solrServer.request(request);
        //<lst name="solr-mbeans">
        @SuppressWarnings("unchecked")
        NamedList<NamedList<NamedList<Object>>> solrMBeans = (NamedList<NamedList<NamedList<Object>>>) response
            .getVal(0);
        //<lst name="QUERYHANDLER">
        NamedList<NamedList<Object>> queryHandler = solrMBeans.getVal(0);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("queryHandler: " + queryHandler.toString());
        }
        return queryHandler;
    }

    static Map<String, MCRSolrQueryHandler> getQueryHandlerMap() throws SolrServerException, IOException {
        NamedList<NamedList<Object>> list = null;
        list = getQueryHandlerList();
        int initialCapacity = list == null ? 2 : list.size();
        HashMap<String, MCRSolrQueryHandler> map = new HashMap<>(initialCapacity);
        MCRSolrQueryHandler standardHandler = getStandardHandler(list);
        map.put(standardHandler.getPath(), standardHandler);
        if (list != null) {
            for (Entry<String, NamedList<Object>> handler : list) {
                if (handler.getKey().charAt(0) != '/') {
                    continue;
                }
                map.put(handler.getKey(), new MCRSolrQueryHandler(handler.getKey(), handler.getValue()));
            }
        }
        return map;
    }

    private static MCRSolrQueryHandler getStandardHandler(NamedList<NamedList<Object>> list) {
        MCRSolrQueryHandler standardHandler = null;
        if (list != null) {
            NamedList<Object> byPath = list.get(MCRSolrConstants.QUERY_PATH);
            if (byPath != null) {
                standardHandler = new MCRSolrQueryHandler(MCRSolrConstants.QUERY_PATH, byPath);
            } else {
                for (Entry<String, NamedList<Object>> test : list) {
                    if (test.getKey().equals("org.apache.solr.handler.StandardRequestHandler")) {
                        standardHandler = new MCRSolrQueryHandler(MCRSolrConstants.QUERY_PATH, test.getValue());
                        break;
                    }
                }
            }
        }
        if (standardHandler == null) {
            standardHandler = new MCRSolrQueryHandler(MCRSolrConstants.QUERY_PATH, null);

        }
        return standardHandler;
    }
}
