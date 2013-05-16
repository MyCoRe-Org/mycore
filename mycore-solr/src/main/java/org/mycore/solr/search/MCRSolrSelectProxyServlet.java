package org.mycore.solr.search;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;
import static org.mycore.solr.MCRSolrConstants.QUERY_PATH;
import static org.mycore.solr.MCRSolrConstants.QUERY_XML_PROTOCOL_VERSION;
import static org.mycore.solr.MCRSolrConstants.SERVER_URL;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRCoreVersion;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

public class MCRSolrSelectProxyServlet extends MCRServlet {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrSelectProxyServlet.class);

    private static final long serialVersionUID = 1L;

    /**
     * Attribute key to store Query parameters as <code>Map&lt;String, String[]&gt;</code> for SOLR.
     * 
     * This takes precedence over any {@link HttpServletRequest} parameter.
     */
    public static final String MAP_KEY = MCRSolrSelectProxyServlet.class.getName() + ".map";

    /**
     * Attribute key to store a {@link SolrQuery}.
     * 
     * This takes precedence over {@link #MAP_KEY} or any {@link HttpServletRequest} parameter.
     */
    public static final String QUERY_KEY = MCRSolrSelectProxyServlet.class.getName() + ".query";

    private static int MAX_CONNECTIONS = MCRConfiguration.instance().getInt(
        CONFIG_PREFIX + "SelectProxy.MaxConnections", 20);

    private HttpClient httpClient;

    private IdleConnectionMonitorThread idleConnectionMonitorThread;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        Map<String, String[]> solrParameter = getSolrQueryParameter(request);

        HttpGet solrHttpMethod = MCRSolrSelectProxyServlet.getSolrHttpMethod(solrParameter);
        try {
            LOGGER.info("Sending Request: " + solrHttpMethod.getURI());
            HttpResponse response = httpClient.execute(solrHttpMethod);
            int statusCode = response.getStatusLine().getStatusCode();

            HttpServletResponse resp = job.getResponse();

            // set status code
            resp.setStatus(statusCode);

            // set all header
            for (Header header : response.getAllHeaders()) {
                if (!(HTTP.TRANSFER_ENCODING.equals(header.getName()) && statusCode == HttpStatus.SC_OK)) {
                    resp.setHeader(header.getName(), header.getValue());
                }
            }

            boolean isXML = response.getFirstHeader(HTTP.CONTENT_TYPE).getValue().contains("/xml");

            HttpEntity solrResponseEntity = response.getEntity();
            if (solrResponseEntity != null) {
                try (InputStream solrResponseStream = solrResponseEntity.getContent()) {
                    if (statusCode == HttpStatus.SC_OK && isXML) {
                        String[] xslt = solrParameter.get("xslt");
                        MCRStreamContent solrResponse = new MCRStreamContent(solrResponseStream, solrHttpMethod
                            .getURI().toString(), xslt != null ? xslt[0] : "response");
                        MCRLayoutService.instance().doLayout(request, resp, solrResponse);
                    } else {
                        // copy solr response to servlet outputstream
                        OutputStream servletOutput = resp.getOutputStream();
                        IOUtils.copy(solrResponseStream, servletOutput);
                    }
                }
            }
        } catch (IOException ex) {
            solrHttpMethod.abort();
            throw ex;
        }
        solrHttpMethod.releaseConnection();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String[]> getSolrQueryParameter(HttpServletRequest request) {
        SolrQuery query = (SolrQuery) request.getAttribute(QUERY_KEY);
        Map<String, String[]> solrParameter;
        if (query == null) {
            solrParameter = (Map<String, String[]>) request.getAttribute(MAP_KEY);
            if (solrParameter == null) {
                //good old way
                solrParameter = request.getParameterMap();
            }
        } else {
            solrParameter = getMap(query);
        }
        return solrParameter;
    }

    private Map<String, String[]> getMap(SolrQuery query) {
        Map<String, String[]> parameter = new HashMap<String, String[]>();
        for (String name : query.getParameterNames()) {
            parameter.put(name, query.getParams(name));
        }
        return parameter;
    }

    @Override
    public void init() throws ServletException {
        super.init();

        PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS);
        connectionManager.setMaxTotal(MAX_CONNECTIONS);

        HttpRequestRetryHandler retryHandler = new SolrRetryHandler(MAX_CONNECTIONS);

        DefaultHttpClient defaultHttpClient = new DefaultHttpClient(connectionManager);
        defaultHttpClient.setHttpRequestRetryHandler(retryHandler);

        String userAgent = MessageFormat.format("MyCoRe/{0} ({1}; java {2})", MCRCoreVersion.getCompleteVersion(),
            MCRConfiguration.instance().getString("MCR.NameOfProject"), System.getProperty("java.version"));
        defaultHttpClient.getParams().setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, "UTF-8")
            .setParameter(CoreProtocolPNames.USER_AGENT, userAgent)
            .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
            .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false);

        httpClient = defaultHttpClient;
        this.idleConnectionMonitorThread = new IdleConnectionMonitorThread(connectionManager);
        idleConnectionMonitorThread.start();
    }

    @Override
    public void destroy() {
        this.idleConnectionMonitorThread.shutdown();
        ClientConnectionManager clientConnectionManager = httpClient.getConnectionManager();
        clientConnectionManager.shutdown();
        super.destroy();
    }

    /**
     * Gets a GetMethod to make a request to the Solr-Server.
     * 
     * @param parameterMap
     *            Parameters to use with the Request
     * @return a method to make the request
     */
    public static HttpGet getSolrHttpMethod(Map<String, String[]> parameterMap) {
        String queryString = getQueryString(parameterMap);
        if (!parameterMap.containsKey("version")) {
            queryString += "&version=" + QUERY_XML_PROTOCOL_VERSION;
        }
        HttpGet httpGet = new HttpGet(MessageFormat.format("{0}{1}?{2}", SERVER_URL, QUERY_PATH, queryString));

        return httpGet;
    }

    private static String getQueryString(Map<String, String[]> parameters) {
        StringBuilder sb = new StringBuilder();
        try {
            for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
                for (String value : entry.getValue()) {
                    sb.append('&').append(entry.getKey()).append('=').append(URLEncoder.encode(value, "UTF-8"));
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new MCRException(e);
        }
        if (sb.length() != 0) {
            sb.deleteCharAt(0);// removes first "&"
        }
        return sb.toString();
    }

    private static class SolrRetryHandler implements HttpRequestRetryHandler {
        int maxExecutionCount;

        public SolrRetryHandler(int maxExecutionCount) {
            super();
            this.maxExecutionCount = maxExecutionCount;
        }

        @Override
        public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
            if (executionCount >= maxExecutionCount) {
                // Do not retry if over max retry count
                return false;
            }
            if (exception instanceof InterruptedIOException) {
                // Timeout
                return true;
            }
            if (exception instanceof UnknownHostException) {
                // Unknown host
                return false;
            }
            if (exception instanceof ConnectException) {
                // Connection refused
                return true;
            }
            if (exception instanceof SSLException) {
                // SSL handshake exception
                return false;
            }
            return true;
        }

    }

    private static class IdleConnectionMonitorThread extends Thread {
        private final ClientConnectionManager connMgr;

        private volatile boolean shutdown;

        public IdleConnectionMonitorThread(ClientConnectionManager connMgr) {
            super();
            this.connMgr = connMgr;
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        wait(5000);
                        // Close expired connections
                        connMgr.closeExpiredConnections();
                        // Close inactive connection
                        connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
                    }
                }
            } catch (InterruptedException ex) {
                // terminate
            }
        }

        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }

    }
}
