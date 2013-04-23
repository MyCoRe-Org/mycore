package org.mycore.solr.search;

import static org.mycore.solr.MCRSolrConstants.QUERY_PATH;
import static org.mycore.solr.MCRSolrConstants.QUERY_XML_PROTOCOL_VERSION;
import static org.mycore.solr.MCRSolrConstants.SERVER_URL;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.solr.MCRSolrConstants;

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

    private static int MAX_CONNECTIONS = MCRConfiguration.instance().getInt(MCRSolrConstants.CONFIG_PREFIX + "SelectProxy.MaxConnections",
        20);

    private HttpClient httpClient;

    private MultiThreadedHttpConnectionManager connectionManager;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        Map<String, String[]> solrParameter = getSolrQueryParameter(request);

        HttpMethod solrHttpMethod = MCRSolrSelectProxyServlet.getSolrHttpMethod(solrParameter);
        try {
            LOGGER.info("Sending Request: " + solrHttpMethod.getURI());
            int statusCode = httpClient.executeMethod(solrHttpMethod);
            InputStream solrResponseStream = solrHttpMethod.getResponseBodyAsStream();

            HttpServletResponse resp = job.getResponse();

            // set status code
            resp.setStatus(statusCode);

            // set all header
            for (Header header : solrHttpMethod.getResponseHeaders()) {
                if (!("Transfer-Encoding".equals(header.getName()) && statusCode == HttpStatus.SC_OK)) {
                    resp.setHeader(header.getName(), header.getValue());
                }
            }

            boolean isXML = solrHttpMethod.getResponseHeader("Content-Type").getValue().contains("/xml");

            if (statusCode == HttpStatus.SC_OK && isXML) {
                MCRStreamContent solrResponse = new MCRStreamContent(solrResponseStream, solrHttpMethod.getURI().toString(), "response");
                MCRLayoutService.instance().doLayout(request, resp, solrResponse);
                return;
            }

            // copy solr response to servlet outputstream
            OutputStream servletOutput = resp.getOutputStream();
            IOUtils.copy(solrResponseStream, servletOutput);
        } finally {
            solrHttpMethod.releaseConnection();
        }
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

        connectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams connectionManagerParams = connectionManager.getParams();
        connectionManagerParams.setDefaultMaxConnectionsPerHost(MAX_CONNECTIONS);
        connectionManagerParams.setMaxTotalConnections(MAX_CONNECTIONS);

        httpClient = new HttpClient(connectionManager);
    }

    @Override
    public void destroy() {
        connectionManager.closeIdleConnections(0);
        connectionManager.deleteClosedConnections();
        super.destroy();
    }

    /**
     * Gets a GetMethod to make a request to the Solr-Server.
     * 
     * @param parameterMap
     *            Parameters to use with the Request
     * @return a method to make the request
     */
    public static HttpMethod getSolrHttpMethod(Map<String, String[]> parameterMap) {
        String queryString = getQueryString(parameterMap);
        if (!parameterMap.containsKey("version")) {
            queryString += "&version=" + QUERY_XML_PROTOCOL_VERSION;
        }
        GetMethod getMethod = new GetMethod(MessageFormat.format("{0}{1}?{2}", SERVER_URL, QUERY_PATH, queryString));

        return getMethod;
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
}
