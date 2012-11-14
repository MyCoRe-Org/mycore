package org.mycore.solr.search;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Map;

import javax.servlet.ServletException;
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
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.solr.utils.MCRSolrUtils;

public class MCRSolrSelectProxyServlet extends MCRServlet {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrSelectProxyServlet.class);
    private static final long serialVersionUID = 1L;
    private static final String SOLR_QUERY_VERSION = MCRSolrUtils.getSolrPropertyValue("SelectProxy.Version", "3.6");
    private static final String SOLR_SELECT_PATH = "select/";
    private HttpClient httpClient;
    private MultiThreadedHttpConnectionManager connectionManager;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, String[]> parameterMap = job.getRequest().getParameterMap();
        HttpMethod solrHttpMethod = MCRSolrSelectProxyServlet.getSolrHttpMethod(parameterMap);
        try {
            LOGGER.info("Sending Request: " + solrHttpMethod.getURI());
            int statusCode = httpClient.executeMethod(solrHttpMethod);
            InputStream solrResponseStream = solrHttpMethod.getResponseBodyAsStream();

            HttpServletResponse resp = job.getResponse();

            // set status code
            resp.setStatus(statusCode);

            // set all header
            for (Header header : solrHttpMethod.getResponseHeaders()) {
                resp.setHeader(header.getName(), header.getValue());
            }

            if (statusCode == HttpStatus.SC_OK) {
                MCRStreamContent solrResponse = new MCRStreamContent(solrResponseStream, solrHttpMethod.getURI()
                        .toString());
                MCRLayoutService.instance().doLayout(job.getRequest(), resp, solrResponse);
                return;
            }

            // copy solr response to servlet outputstream
            OutputStream servletOutput = resp.getOutputStream();
            IOUtils.copy(solrResponseStream, servletOutput);
        } finally {
            solrHttpMethod.releaseConnection();
        }
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
            throw new MCRException(MessageFormat.format("Property \"{0}ServerURL\" is undefined.",
                    MCRSolrUtils.CONFIG_PREFIX));
        }

        String queryString = getQueryString(parameterMap);
        if (!parameterMap.containsKey("version")) {
            queryString += "&version=" + SOLR_QUERY_VERSION;
        }
        GetMethod getMethod = new GetMethod(MessageFormat.format("{0}{1}?{2}", solrServerUrl, SOLR_SELECT_PATH,
                queryString));

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
        if (sb.length() >= 0) {
            sb.deleteCharAt(0);// removes first "&"
        }
        return sb.toString();
    }
}
