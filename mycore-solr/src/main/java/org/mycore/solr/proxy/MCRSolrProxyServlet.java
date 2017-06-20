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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.MultiMapSolrParams;
import org.apache.solr.common.util.NamedList;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.services.http.MCRHttpUtils;
import org.mycore.services.http.MCRIdleConnectionMonitorThread;
import org.xml.sax.SAXException;

public class MCRSolrProxyServlet extends MCRServlet {

    static final Logger LOGGER = LogManager.getLogger(MCRSolrProxyServlet.class);

    private static final long serialVersionUID = 1L;

    /**
     * Attribute key to store Query parameters as <code>Map&lt;String, String[]&gt;</code> for SOLR. This takes
     * precedence over any {@link HttpServletRequest} parameter.
     */
    public static final String MAP_KEY = MCRSolrProxyServlet.class.getName() + ".map";

    /**
     * Attribute key to store a {@link SolrQuery}. This takes precedence over {@link #MAP_KEY} or any
     * {@link HttpServletRequest} parameter.
     */
    public static final String QUERY_KEY = MCRSolrProxyServlet.class.getName() + ".query";

    public static final String QUERY_HANDLER_PAR_NAME = "qt";

    private static int MAX_CONNECTIONS = MCRConfiguration.instance()
        .getInt(CONFIG_PREFIX + "SelectProxy.MaxConnections");

    private CloseableHttpClient httpClient;

    private MCRIdleConnectionMonitorThread idleConnectionMonitorThread;

    protected HttpHost solrHost;

    private Map<String, MCRSolrQueryHandler> queryHandlerMap;

    private PoolingHttpClientConnectionManager httpClientConnectionManager;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse resp = job.getResponse();
        //handle redirects
        if (request.getParameter(QUERY_HANDLER_PAR_NAME) != null || request.getAttribute(MAP_KEY) != null) {
            //redirect to Request Handler
            redirectToQueryHandler(request, resp);
            return;
        }
        Document input = (Document) request.getAttribute("MCRXEditorSubmission");
        if (input != null) {
            redirectToQueryHandler(input, resp);
            return;
        }
        String queryHandlerPath = request.getPathInfo();
        if (queryHandlerPath == null) {
            boolean refresh = "true".equals(getProperty(request, "refresh"));
            if (refresh) {
                updateQueryHandlerMap(resp);
                return;
            }
            redirectToQueryHandler(request, resp);
            return;
        }
        //end of redirects
        //either we have a queryHandler specified by path here or use the default one
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

    /**
     * redirects to query handler by using value of 'qt' parameter
     */
    private static void redirectToQueryHandler(HttpServletRequest request, HttpServletResponse resp)
        throws IOException {
        ModifiableSolrParams solrQueryParameter = getSolrQueryParameter(request);
        String queryHandlerPath = solrQueryParameter.get(QUERY_HANDLER_PAR_NAME, QUERY_PATH);
        solrQueryParameter.remove(QUERY_HANDLER_PAR_NAME);
        Map<String, String[]> parameters = toMultiMap(solrQueryParameter);
        doRedirectToQueryHandler(resp, queryHandlerPath, parameters);
    }

    static Map<String, String[]> toMultiMap(ModifiableSolrParams solrQueryParameter) {
        NamedList<Object> namedList = solrQueryParameter.toNamedList();
        //disabled for MCR-953 and https://issues.apache.org/jira/browse/SOLR-7508
        //Map<String, String[]> parameters = ModifiableSolrParams.toMultiMap(namedList);
        HashMap<String, String[]> parameters = new HashMap<>();
        for (int i = 0; i < namedList.size(); i++) {
            String name = namedList.getName(i);
            Object val = namedList.getVal(i);
            if (val instanceof String[]) {
                MultiMapSolrParams.addParam(name, (String[]) val, parameters);
            } else {
                MultiMapSolrParams.addParam(name, val.toString(), parameters);
            }
        }
        //end of fix
        return parameters;
    }

    /**
     * redirects to query handler by using xeditor input document
     */
    private static void redirectToQueryHandler(Document input, HttpServletResponse resp)
        throws IOException, TransformerException, SAXException {
        LinkedHashMap<String, String[]> parameters = new LinkedHashMap<>();
        List<Element> children = input.getRootElement().getChildren();
        for (Element param : children) {
            String attribute = param.getAttributeValue("name");
            if (attribute != null) {
                parameters.put(attribute, new String[] { param.getTextTrim() });
            }
        }
        String queryHandlerPath = parameters.get(QUERY_HANDLER_PAR_NAME)[0];
        parameters.remove("qt");
        doRedirectToQueryHandler(resp, queryHandlerPath, parameters);
    }

    /**
     * used by
     */
    private static void doRedirectToQueryHandler(HttpServletResponse resp, String queryHandlerPath,
        Map<String, String[]> parameters)
        throws IOException {
        String requestURL = MessageFormat.format("{0}solr{1}{2}", getServletBaseURL(), queryHandlerPath,
            toSolrParams(parameters).toQueryString());
        LOGGER.info("Redirect to: " + requestURL);
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
                        MCRStreamContent solrResponse = new MCRStreamContent(solrResponseStream,
                            solrHttpMethod.getURI().toString(),
                            "response");
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
     * 
     * @param queryHandler
     * @param parameterMap
     *            Parameters to use with the Request
     * @return a method to make the request
     */
    private static HttpGet getSolrHttpMethod(MCRSolrQueryHandler queryHandler, ModifiableSolrParams params) {
        HttpGet httpGet = new HttpGet(
            MessageFormat.format("{0}{1}{2}", SERVER_URL, queryHandler.getPath(), params.toQueryString()));
        return httpGet;
    }

    private static ModifiableSolrParams getSolrQueryParameter(HttpServletRequest request) {
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

    @Override
    public void init() throws ServletException {
        super.init();

        LOGGER.info("Initializing SOLR connection to \"" + SERVER_URL + "\"");

        try {
            queryHandlerMap = MCRSolrProxyUtils.getQueryHandlerMap();
        } catch (SolrServerException | IOException e) {
            LOGGER.error("Error while getting query handler from SOLR.", e);
            MCRSolrQueryHandler standardHandler = MCRSolrProxyUtils.getStandardHandler(null);
            LOGGER.info("Adding standard handler: " + standardHandler);
            queryHandlerMap = new HashMap<String, MCRSolrQueryHandler>();
            queryHandlerMap.put(standardHandler.getPath(), standardHandler);
        }

        solrHost = MCRHttpUtils.getHttpHost(SERVER_URL);
        if (solrHost == null) {
            throw new ServletException("URI does not specify a valid host name: " + SERVER_URL);
        }
        httpClientConnectionManager = MCRHttpUtils.getConnectionManager(MAX_CONNECTIONS);
        httpClient = MCRHttpUtils.getHttpClient(httpClientConnectionManager, MAX_CONNECTIONS);

        // start thread to monitor stalled connections
        idleConnectionMonitorThread = new MCRIdleConnectionMonitorThread(httpClientConnectionManager);
        idleConnectionMonitorThread.start();
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

    private static ModifiableSolrParams toSolrParams(Map<String, String[]> parameters) {
        // to maintain order
        LinkedHashMap<String, String[]> copy = new LinkedHashMap<String, String[]>(parameters);
        ModifiableSolrParams solrParams = new ModifiableSolrParams(copy);
        if (!parameters.containsKey("version") && !parameters.containsKey("wt")) {
            solrParams.set("version", QUERY_XML_PROTOCOL_VERSION);
        }
        return solrParams;
    }
}
