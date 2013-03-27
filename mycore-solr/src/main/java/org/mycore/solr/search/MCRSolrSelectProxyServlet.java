package org.mycore.solr.search;

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
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.solr.logging.MCRSolrLogLevels;
import org.mycore.solr.utils.MCRSolrUtils;

public class MCRSolrSelectProxyServlet extends MCRServlet {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrSelectProxyServlet.class);

    private static final long serialVersionUID = 1L;

    private static final String SOLR_QUERY_VERSION = MCRSolrUtils.getSolrPropertyValue("XMLProtocolVersion", "4.0");

    private static final String SOLR_SELECT_PATH = MCRSolrUtils.getSolrPropertyValue("SelectPath", "select/");

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

    private HttpClient httpClient;

    private MultiThreadedHttpConnectionManager connectionManager;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        Map<String, String[]> solrParameter = getSolrQueryParameter(request);

        HttpMethod solrHttpMethod = MCRSolrSelectProxyServlet.getSolrHttpMethod(solrParameter);
        try {
            LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Sending Request: " + solrHttpMethod.getURI());
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

        int maxConnections = Integer.parseInt(MCRSolrUtils.getSolrPropertyValue("SelectProxy.MaxConnections", "20"));
        connectionManager = new MultiThreadedHttpConnectionManager();

        HttpConnectionManagerParams connectionManagerParams = connectionManager.getParams();
        connectionManagerParams.setDefaultMaxConnectionsPerHost(maxConnections);
        connectionManagerParams.setMaxTotalConnections(maxConnections);

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
        String solrServerUrl = MCRSolrUtils.getSolrPropertyValue("ServerURL");
        if (solrServerUrl == null) {
            throw new MCRException(MessageFormat.format("Property \"{0}ServerURL\" is undefined.", MCRSolrUtils.CONFIG_PREFIX));
        }
        if (!solrServerUrl.endsWith("/")) {
            solrServerUrl += '/';
        }

        String queryString = getQueryString(parameterMap);
        if (!parameterMap.containsKey("version")) {
            queryString += "&version=" + SOLR_QUERY_VERSION;
        }
        GetMethod getMethod = new GetMethod(MessageFormat.format("{0}{1}?{2}", solrServerUrl, SOLR_SELECT_PATH, queryString));

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
