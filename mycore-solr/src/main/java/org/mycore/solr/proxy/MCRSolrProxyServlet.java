package org.mycore.solr.proxy;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;
import static org.mycore.solr.MCRSolrConstants.QUERY_PATH;
import static org.mycore.solr.MCRSolrConstants.QUERY_XML_PROTOCOL_VERSION;
import static org.mycore.solr.MCRSolrConstants.SERVER_URL;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

public class MCRSolrProxyServlet extends MCRServlet {

    static final Logger LOGGER = Logger.getLogger(MCRSolrProxyServlet.class);

    private static final long serialVersionUID = 1L;

    /**
     * Attribute key to store Query parameters as <code>Map&lt;String, String[]&gt;</code> for SOLR.
     * 
     * This takes precedence over any {@link HttpServletRequest} parameter.
     */
    public static final String MAP_KEY = MCRSolrProxyServlet.class.getName() + ".map";

    /**
     * Attribute key to store a {@link SolrQuery}.
     * 
     * This takes precedence over {@link #MAP_KEY} or any {@link HttpServletRequest} parameter.
     */
    public static final String QUERY_KEY = MCRSolrProxyServlet.class.getName() + ".query";

    private static int MAX_CONNECTIONS = MCRConfiguration.instance().getInt(
        CONFIG_PREFIX + "SelectProxy.MaxConnections");

    private HttpClient httpClient;

    private MCRIdleConnectionMonitorThread idleConnectionMonitorThread;

    protected HttpHost solrHost;

    private Map<String, MCRSolrQueryHandler> queryHandlerMap;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse resp = job.getResponse();

        String queryHandlerPath = request.getPathInfo();
        if (queryHandlerPath == null) {
            boolean refresh = "true".equals(getProperty(request, "refresh"));
            if (refresh) {
                updateQueryHandlerMap(resp);
                return;
            }
            redirectToDefaultQueryHandler(request, resp);
            return;
        }
        MCRSolrQueryHandler queryHandler = queryHandlerMap.get(queryHandlerPath);
        if (queryHandler == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (queryHandler.isRestricted()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "No access to " + queryHandler.toString());
            return;
        }

        handleQuery(queryHandler, request, resp);
    }

    private void handleQuery(MCRSolrQueryHandler queryHandler, HttpServletRequest request, HttpServletResponse resp)
        throws IOException {
        ModifiableSolrParams solrParameter = getSolrQueryParameter(request);
        HttpGet solrHttpMethod = MCRSolrProxyServlet.getSolrHttpMethod(queryHandler, solrParameter);
        try {
            LOGGER.info("Sending Request: " + solrHttpMethod.getURI());
            HttpResponse response = httpClient.execute(solrHost, solrHttpMethod);
            int statusCode = response.getStatusLine().getStatusCode();

            // set status code
            resp.setStatus(statusCode);

            boolean isXML = response.getFirstHeader(HTTP.CONTENT_TYPE).getValue().contains("/xml");
            boolean justCopyInput = !isXML;

            // set all headers
            for (Header header : response.getAllHeaders()) {
                if (justCopyInput || !HTTP.TRANSFER_ENCODING.equals(header.getName())) {
                    resp.setHeader(header.getName(), header.getValue());
                }
            }

            HttpEntity solrResponseEntity = response.getEntity();
            if (solrResponseEntity != null) {
                try (InputStream solrResponseStream = solrResponseEntity.getContent()) {
                    if (justCopyInput) {
                        // copy solr response to servlet outputstream
                        OutputStream servletOutput = resp.getOutputStream();
                        IOUtils.copy(solrResponseStream, servletOutput);
                    } else {
                        MCRStreamContent solrResponse = new MCRStreamContent(solrResponseStream, solrHttpMethod
                            .getURI().toString(), "response");
                        MCRLayoutService.instance().doLayout(request, resp, solrResponse);
                    }
                }
            }
        } catch (IOException ex) {
            solrHttpMethod.abort();
            throw ex;
        }
        solrHttpMethod.releaseConnection();
    }

    private void redirectToDefaultQueryHandler(HttpServletRequest request, HttpServletResponse resp) throws IOException {
        String selectProxyURL = MessageFormat.format("{0}solr{1}?{2}", MCRServlet.getServletBaseURL(), QUERY_PATH,
            getSolrQueryParameter(request).toString());
        resp.sendRedirect(resp.encodeRedirectURL(selectProxyURL));
    }

    private void updateQueryHandlerMap(HttpServletResponse resp) throws IOException {
        Map<String, MCRSolrQueryHandler> handlerMap = MCRSolrProxyUtils.getQueryHandlerMap();
        queryHandlerMap = handlerMap;
        MCRSolrQueryHandler[] handler = handlerMap.values().toArray(new MCRSolrQueryHandler[handlerMap.size()]);
        Arrays.sort(handler, MCRSolrQueryHandler.getPathComparator());
        PrintWriter writer = resp.getWriter();
        for (MCRSolrQueryHandler h : handler) {
            writer.write(h.toString());
            writer.append('\n');
        }
    }

    /**
     * Gets a HttpGet to make a request to the Solr-Server.
     * @param queryHandler 
     * 
     * @param parameterMap
     *            Parameters to use with the Request
     * @return a method to make the request
     */
    private static HttpGet getSolrHttpMethod(MCRSolrQueryHandler queryHandler, ModifiableSolrParams params) {
        HttpGet httpGet = new HttpGet(MessageFormat.format("{0}{1}?{2}", SERVER_URL, queryHandler.getPath(),
            params.toString()));
        return httpGet;
    }

    @SuppressWarnings("unchecked")
    private static ModifiableSolrParams getSolrQueryParameter(HttpServletRequest request) {
        SolrQuery query = (SolrQuery) request.getAttribute(QUERY_KEY);
        if (query != null) {
            return query;
        }
        Map<String, String[]> solrParameter;
        solrParameter = (Map<String, String[]>) request.getAttribute(MAP_KEY);
        if (solrParameter == null) {
            //good old way
            solrParameter = request.getParameterMap();
        }
        return getQueryString(solrParameter);
    }

    @Override
    public void init() throws ServletException {
        super.init();

        LOGGER.info("Initializing SOLR connection to \"" + SERVER_URL + "\"");

        queryHandlerMap = MCRSolrProxyUtils.getQueryHandlerMap();

        solrHost = MCRSolrProxyUtils.getHttpHost(SERVER_URL);
        if (solrHost == null) {
            throw new ServletException("URI does not specify a valid host name: " + SERVER_URL);
        }
        httpClient = MCRSolrProxyUtils.getHttpClient(MAX_CONNECTIONS);

        //start thread to monitor stalled connections
        idleConnectionMonitorThread = new MCRIdleConnectionMonitorThread(httpClient.getConnectionManager());
        idleConnectionMonitorThread.start();
    }

    @Override
    public void destroy() {
        idleConnectionMonitorThread.shutdown();
        ClientConnectionManager clientConnectionManager = httpClient.getConnectionManager();
        clientConnectionManager.shutdown();
        super.destroy();
    }

    private static ModifiableSolrParams getQueryString(Map<String, String[]> parameters) {
        //to maintain order
        LinkedHashMap<String, String[]> copy = new LinkedHashMap<String, String[]>(parameters);
        ModifiableSolrParams solrParams = new ModifiableSolrParams(copy);
        if (!parameters.containsKey("version") && !parameters.containsKey("wt")) {
            solrParams.set("version", QUERY_XML_PROTOCOL_VERSION);
        }
        return solrParams;
    }
}
