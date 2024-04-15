package org.mycore.solr.proxy;

import static org.mycore.solr.MCRSolrConstants.SOLR_CONFIG_PREFIX;
import static org.mycore.solr.MCRSolrConstants.SOLR_QUERY_XML_PROTOCOL_VERSION;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.services.http.MCRHttpUtils;
import org.mycore.services.http.MCRIdleConnectionMonitorThread;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrConstants;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MCRSolrProxyHttpClient {
    static final Logger LOGGER = LogManager.getLogger();

    public static final String QUERY_CORE_PARAMETER = "core";
    
    public static final String MAP_KEY = MCRSolrProxyServlet.class.getName() + ".map";

    private static int MAX_CONNECTIONS = MCRConfiguration2
        .getOrThrow(SOLR_CONFIG_PREFIX + "SelectProxy.MaxConnections", Integer::parseInt);

    /**
     * Attribute key to store Query parameters as <code>Map&lt;String, String[]&gt;</code> for SOLR. This takes
     * precedence over any {@link HttpServletRequest} parameter.
     */
    

    private static Map<String, String> NEW_HTTP_RESPONSE_HEADER = MCRConfiguration2
        .getSubPropertiesMap(SOLR_CONFIG_PREFIX + "HTTPResponseHeader.");

    private CloseableHttpClient httpClient;

    private MCRIdleConnectionMonitorThread idleConnectionMonitorThread;

    private PoolingHttpClientConnectionManager httpClientConnectionManager;

    /**
     * Attribute key to store a {@link SolrQuery}. This takes precedence over {@link #MAP_KEY} or any
     * {@link HttpServletRequest} parameter.
     */
    public static final String QUERY_KEY = MCRSolrProxyServlet.class.getName() + ".query";

    /**
     * Gets a HttpGet to make a request to the Solr-Server.
     *
     * @param queryHandlerPath
     *            The query handler path
     * @param params
     *            Parameters to use with the Request
     * @return a method to make the request
     */
    private static HttpGet getSolrHttpMethod(String queryHandlerPath, ModifiableSolrParams params, String type) {
        String serverURL = MCRSolrClientFactory.get(type).get().getV1CoreURL();

        return new HttpGet(new MessageFormat("{0}{1}{2}", Locale.ROOT)
            .format(new Object[] { serverURL, queryHandlerPath, params.toQueryString() }));
    }

    public void init() {
        httpClientConnectionManager = MCRHttpUtils.getConnectionManager(MAX_CONNECTIONS);
        httpClient = MCRHttpUtils.getHttpClient(httpClientConnectionManager, MAX_CONNECTIONS);

        // start thread to monitor stalled connections
        idleConnectionMonitorThread = new MCRIdleConnectionMonitorThread(httpClientConnectionManager);
        idleConnectionMonitorThread.start();
    }

    public McrSolrHttpResult handleQuery(String queryHandlerPath, HttpServletRequest request, HttpServletResponse resp)
        throws IOException {
        ModifiableSolrParams solrParameter = getSolrQueryParameter(request);
        filterParams(solrParameter);
        HttpGet solrHttpMethod = getSolrHttpMethod(queryHandlerPath, solrParameter,
            Optional.ofNullable(request.getParameter(QUERY_CORE_PARAMETER)).orElse(MCRSolrConstants.MAIN_CORE_TYPE));
        try {
            LOGGER.info("Sending Request: {}", solrHttpMethod.getURI());
            HttpResponse response = httpClient.execute(solrHttpMethod);
            int statusCode = response.getStatusLine().getStatusCode();

            // set status code
            resp.setStatus(statusCode);

            // set all headers
            for (Header header : response.getAllHeaders()) {
                LOGGER.debug("SOLR response header: {} - {}", header.getName(), header.getValue());
                String headerName = header.getName();
                if (NEW_HTTP_RESPONSE_HEADER.containsKey(headerName)) {
                    String headerValue = NEW_HTTP_RESPONSE_HEADER.get(headerName);
                    if (headerValue != null && headerValue.length() > 0) {
                        resp.setHeader(headerName, headerValue);
                    }
                } else {
                    resp.setHeader(header.getName(), header.getValue());
                }
            }
            solrHttpMethod.releaseConnection();
            return new McrSolrHttpResult(response, solrHttpMethod.getURI());

        } catch (IOException ex) {
            solrHttpMethod.abort();
            throw ex;
        }

    }

    protected ModifiableSolrParams getSolrQueryParameter(HttpServletRequest request) {
        SolrQuery query = (SolrQuery) request.getAttribute(QUERY_KEY);
        if (query != null) {
            return query;
        }
        @SuppressWarnings("unchecked")
        Map<String, String[]> solrParameter = (Map<String, String[]>) request.getAttribute(MAP_KEY);
        if (solrParameter == null) {
            // good old way
            solrParameter = request.getParameterMap();
        }
        return toSolrParams(solrParameter);
    }

    private void filterParams(ModifiableSolrParams solrParameter) {
        MCRConfiguration2.getString("MCR.Solr.Disallowed.Facets")
            .ifPresent(disallowedFacets -> MCRConfiguration2.splitValue(disallowedFacets)
                .forEach(disallowedFacet -> solrParameter.remove("facet.field", disallowedFacet)));
    }

    public void close() {
        idleConnectionMonitorThread.shutdown();
        try {
            httpClient.close();
        } catch (IOException e) {
            LOGGER.error("Could not close HTTP client to SOLR server.", e);
        }
        httpClientConnectionManager.shutdown();

    }

    public record McrSolrHttpResult(HttpResponse response, URI uri) {

    };

    public ModifiableSolrParams toSolrParams(Map<String, String[]> parameters) {
        // to maintain order
        LinkedHashMap<String, String[]> copy = new LinkedHashMap<>(parameters);
        ModifiableSolrParams solrParams = new ModifiableSolrParams(copy);
        if (!parameters.containsKey("version") && !parameters.containsKey("wt")) {
            solrParams.set("version", SOLR_QUERY_XML_PROTOCOL_VERSION);
        }
        return solrParams;
    }
}
