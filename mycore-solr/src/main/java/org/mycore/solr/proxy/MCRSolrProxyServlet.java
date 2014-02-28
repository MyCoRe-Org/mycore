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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.xml.sax.SAXException;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

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

    private CloseableHttpClient httpClient;

    private MCRIdleConnectionMonitorThread idleConnectionMonitorThread;

    protected HttpHost solrHost;

    private Map<String, MCRSolrQueryHandler> queryHandlerMap;

    private PoolingHttpClientConnectionManager httpClientConnectionManager;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse resp = job.getResponse();
        Document input = (Document) request.getAttribute("MCRXEditorSubmission");
        if (input != null) {
            getQueryHandlerAndPrepareParameterMap(input, resp);
            return;
        }
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

    private void getQueryHandlerAndPrepareParameterMap(Document input, HttpServletResponse resp) throws IOException,
        TransformerException, SAXException {
        LinkedHashMap<String, String[]> parameters = new LinkedHashMap<>();
        List<Element> children = input.getRootElement().getChildren();
        for (Element param : children) {
            String attribute = param.getAttributeValue("name");
            if (attribute != null) {
                parameters.put(attribute, new String[] { param.getTextTrim() });
            }
        }
        String queryHandlerPath = parameters.get("qt")[0];
        parameters.remove("qt");
        String requestURL = MessageFormat.format("{0}solr{1}?{2}", getServletBaseURL(), queryHandlerPath,
            getQueryString(parameters));
        LOGGER.info("Redirect XEditor input to: " + requestURL);
        resp.sendRedirect(resp.encodeRedirectURL(requestURL));
    }

    private void handleQuery(MCRSolrQueryHandler queryHandler, HttpServletRequest request, HttpServletResponse resp)
        throws IOException, TransformerException, SAXException {
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
                if (!HTTP.TRANSFER_ENCODING.equals(header.getName())) {
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

    private void updateQueryHandlerMap(HttpServletResponse resp) throws IOException, SolrServerException {
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

        final ListeningScheduledExecutorService service = MoreExecutors.listeningDecorator(Executors
            .newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SOLR QueryHandler Resolver").build()));
        ListenableFuture<Map<String, MCRSolrQueryHandler>> queryHandlerMapFuture = service
            .submit(getQueryHandlerMapCallable());
        Futures.addCallback(queryHandlerMapFuture, getQueryHandlerMapCallBack(service, 0));
        solrHost = MCRSolrProxyUtils.getHttpHost(SERVER_URL);
        if (solrHost == null) {
            throw new ServletException("URI does not specify a valid host name: " + SERVER_URL);
        }
        httpClientConnectionManager = MCRSolrProxyUtils.getConnectionManager(MAX_CONNECTIONS);
        httpClient = MCRSolrProxyUtils.getHttpClient(httpClientConnectionManager, MAX_CONNECTIONS);

        //start thread to monitor stalled connections
        idleConnectionMonitorThread = new MCRIdleConnectionMonitorThread(httpClientConnectionManager);
        idleConnectionMonitorThread.start();
    }

    private FutureCallback<Map<String, MCRSolrQueryHandler>> getQueryHandlerMapCallBack(
        final ListeningScheduledExecutorService service, final int retries) {
        return new FutureCallback<Map<String, MCRSolrQueryHandler>>() {
            int numRetries = retries;

            @Override
            public void onFailure(Throwable t) {
                LOGGER.warn("Exception while executing task.", t);
                numRetries++;
                LOGGER.info("Getting query handler from SOLR was not successful, resubmitting (num retries: "
                    + numRetries + ")");
                ListenableScheduledFuture<Map<String, MCRSolrQueryHandler>> scheduledFuture = service.schedule(
                    getQueryHandlerMapCallable(), 1, TimeUnit.MINUTES);
                Futures.addCallback(scheduledFuture, getQueryHandlerMapCallBack(service, numRetries));
            }

            @Override
            public void onSuccess(Map<String, MCRSolrQueryHandler> result) {
                LOGGER.info("Got " + result.size() + " query handler from SOLR.");
                queryHandlerMap = result;
                service.shutdown();
            }
        };
    }

    private Callable<Map<String, MCRSolrQueryHandler>> getQueryHandlerMapCallable() {
        return new Callable<Map<String, MCRSolrQueryHandler>>() {
            @Override
            public Map<String, MCRSolrQueryHandler> call() throws Exception {
                return MCRSolrProxyUtils.getQueryHandlerMap();
            }
        };
    }

    @Override
    public void destroy() {
        idleConnectionMonitorThread.shutdown();
        try {
            httpClient.close();
        } catch (IOException e) {
            log("Could not close HTTP client to SOLR server.", e);
        }
        httpClientConnectionManager.shutdown();
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
