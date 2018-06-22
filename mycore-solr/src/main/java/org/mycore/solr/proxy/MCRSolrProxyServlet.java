/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.solr.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
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
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrConstants;
import org.xml.sax.SAXException;

import static org.mycore.solr.MCRSolrConstants.SOLR_CONFIG_PREFIX;
import static org.mycore.solr.MCRSolrConstants.SOLR_QUERY_PATH;
import static org.mycore.solr.MCRSolrConstants.SOLR_QUERY_XML_PROTOCOL_VERSION;

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

    public static final String QUERY_CORE_PARAMETER = "core";

    private static int MAX_CONNECTIONS = MCRConfiguration.instance()
        .getInt(SOLR_CONFIG_PREFIX + "SelectProxy.MaxConnections");

    private CloseableHttpClient httpClient;

    private MCRIdleConnectionMonitorThread idleConnectionMonitorThread;

    private Set<String> queryHandlerWhitelist;

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
        if (!queryHandlerWhitelist.contains(queryHandlerPath)) {
            // query handler path is not registered and therefore not allowed
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "No access to " + queryHandlerPath);
            return;
        }
        handleQuery(queryHandlerPath, request, resp);
    }

    /**
     * redirects to query handler by using value of 'qt' parameter
     */
    private static void redirectToQueryHandler(HttpServletRequest request, HttpServletResponse resp)
        throws IOException {
        ModifiableSolrParams solrQueryParameter = getSolrQueryParameter(request);
        String queryHandlerPath = solrQueryParameter.get(QUERY_HANDLER_PAR_NAME, SOLR_QUERY_PATH);
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
        LOGGER.info("Redirect to: {}", requestURL);
        resp.sendRedirect(resp.encodeRedirectURL(requestURL));
    }

    private void handleQuery(String queryHandlerPath, HttpServletRequest request, HttpServletResponse resp)
        throws IOException, TransformerException, SAXException {
        ModifiableSolrParams solrParameter = getSolrQueryParameter(request);
        HttpGet solrHttpMethod = MCRSolrProxyServlet.getSolrHttpMethod(queryHandlerPath, solrParameter,
            Optional.ofNullable(request.getParameter(QUERY_CORE_PARAMETER)).orElse(MCRSolrConstants.MAIN_CORE_TYPE));
        try {
            LOGGER.info("Sending Request: {}", solrHttpMethod.getURI());
            HttpResponse response = httpClient.execute(solrHttpMethod);
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
        this.updateQueryHandlerMap();
        PrintWriter writer = resp.getWriter();
        queryHandlerWhitelist.forEach(handler -> writer.append(handler).append('\n'));
    }

    private void updateQueryHandlerMap() {
        List<String> whitelistPropertyList = MCRConfiguration.instance().getStrings(
            SOLR_CONFIG_PREFIX + "Proxy.WhiteList",
            Collections.singletonList("/select"));
        this.queryHandlerWhitelist = new HashSet<>(whitelistPropertyList);
    }

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

        return new HttpGet(MessageFormat.format("{0}{1}{2}", serverURL, queryHandlerPath, params.toQueryString()));
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

        this.updateQueryHandlerMap();

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
        LinkedHashMap<String, String[]> copy = new LinkedHashMap<>(parameters);
        ModifiableSolrParams solrParams = new ModifiableSolrParams(copy);
        if (!parameters.containsKey("version") && !parameters.containsKey("wt")) {
            solrParams.set("version", SOLR_QUERY_XML_PROTOCOL_VERSION);
        }
        return solrParams;
    }
}
