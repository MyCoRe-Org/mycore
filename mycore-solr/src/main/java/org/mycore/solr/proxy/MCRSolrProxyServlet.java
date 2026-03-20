/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import static org.mycore.access.MCRAccessManager.PERMISSION_READ;
import static org.mycore.solr.MCRSolrConstants.SOLR_CONFIG_PREFIX;
import static org.mycore.solr.MCRSolrConstants.SOLR_QUERY_PATH;
import static org.mycore.solr.MCRSolrConstants.SOLR_QUERY_XML_PROTOCOL_VERSION;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serial;
import java.net.http.HttpClient;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.InputStreamResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.MultiMapSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.services.http.MCRHttpUtils;
import org.mycore.solr.MCRSolrConstants;
import org.mycore.solr.MCRSolrIndex;
import org.mycore.solr.MCRSolrIndexRegistryManager;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;
import org.xml.sax.SAXException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This class implements a proxy for access to the SOLR backend.<br><br>
 *
 * With the following configuration properties
 * you can manipulate the response header. The entries will be replace the attributes of the incomming header.
 * If the new attribute text is empty, it will be remove the attribute.<br><br>
 * MCR.Solr.HTTPResponseHeader.{response_header_attribute_name}={new_response_header_attribute}
 * MCR.Solr.HTTPResponseHeader....=<br><br>
 *
 * You can set the maximum of connections to the SOLR server with the property<br><br>
 * MCR.Solr.SelectProxy.MaxConnections={number}
 */
public class MCRSolrProxyServlet extends MCRServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger();

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

    public static final MCRSolrAuthenticationManager SOLR_AUTHENTICATION_MANAGER =
        MCRSolrAuthenticationManager.obtainInstance();

    private HttpClient httpClient;

    private Set<String> queryHandlerWhitelist;

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

        String ruleID = "solr:" + queryHandlerPath;
        if (MCRAccessManager.hasRule(ruleID, PERMISSION_READ)
            && !MCRAccessManager.checkPermission(ruleID, PERMISSION_READ)) {
            job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN);
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
        NamedList<Object> namedList = new SimpleOrderedMap<>(solrQueryParameter);
        //disabled for MCR-953 and https://issues.apache.org/jira/browse/SOLR-7508
        //Map<String, String[]> parameters = ModifiableSolrParams.toMultiMap(namedList);
        Map<String, String[]> parameters = new HashMap<>();
        for (int i = 0; i < namedList.size(); i++) {
            String name = namedList.getName(i);
            Object val = namedList.getVal(i);
            if (val instanceof String[] strings) {
                MultiMapSolrParams.addParam(name, strings, parameters);
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
        throws IOException {
        Map<String, String[]> parameters = new LinkedHashMap<>();
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
        String requestURL = new MessageFormat("{0}solr{1}{2}", Locale.ROOT)
            .format(new Object[] { getServletBaseURL(), queryHandlerPath, toSolrParams(parameters).toQueryString() });
        LOGGER.info("Redirect to: {}", requestURL);
        resp.sendRedirect(resp.encodeRedirectURL(requestURL));
    }

    private void handleQuery(String queryHandlerPath, HttpServletRequest request, HttpServletResponse resp)
        throws IOException, TransformerException, SAXException {
        ModifiableSolrParams solrParameter = getSolrQueryParameter(request);
        filterParams(solrParameter);

        QueryRequest queryRequest = new QueryRequest(solrParameter);
        queryRequest.setPath(queryHandlerPath);
        SOLR_AUTHENTICATION_MANAGER.applyAuthentication(queryRequest, MCRSolrAuthenticationLevel.SEARCH);

        String core = Optional.ofNullable(request.getParameter(QUERY_CORE_PARAMETER))
            .orElse(MCRSolrConstants.MAIN_INDEX_ID);

        Optional<MCRSolrIndex> optionalIndex = MCRSolrIndexRegistryManager.obtainRegistry().getIndex(core);
        if (optionalIndex.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No such core: " + core);
            return;
        }
        MCRSolrIndex solrIndex = optionalIndex.get();
        String writerType = getWriterType(request);
        queryRequest.setResponseParser(new InputStreamResponseParser(writerType));

        try {
            NamedList<Object> solrResponse = solrIndex.getClient().request(queryRequest);
            try (InputStream is = (InputStream) solrResponse.get("stream")) {
                if (solrResponse.get("responseStatus") != null) {
                    Integer responseStatus = (Integer) solrResponse.get("responseStatus");
                    resp.setStatus(responseStatus);
                }

                if (!writerType.equals("xml")) {
                    // copy solr response to servlet outputstream
                    OutputStream servletOutput = resp.getOutputStream();
                    is.transferTo(servletOutput);
                } else {
                    MCRStreamContent solrResponseContent = new MCRStreamContent(is,
                        request.getRequestURI(),
                        "response");
                    MCRLayoutService.obtainInstance().doLayout(request, resp, solrResponseContent);
                }
            }
        } catch (SolrServerException e) {
            throw new IOException("Error while processing query request", e);
        }
    }

    private String getWriterType(HttpServletRequest request) {
        return Optional.ofNullable(request.getParameter("wt"))
            .orElse("xml");
    }

    private void filterParams(ModifiableSolrParams solrParameter) {
        MCRConfiguration2.getString("MCR.Solr.Disallowed.Facets")
            .ifPresent(disallowedFacets -> MCRConfiguration2.splitValue(disallowedFacets)
                .forEach(disallowedFacet -> solrParameter.remove("facet.field", disallowedFacet)));

        MCRConfiguration2.getString("MCR.Solr.Proxy.Disallowed.Parameter")
            .ifPresent(disallowedParameter -> MCRConfiguration2.splitValue(disallowedParameter)
                .forEach(solrParameter::remove));
    }

    private void updateQueryHandlerMap(HttpServletResponse resp) throws IOException {
        this.updateQueryHandlerMap();
        PrintWriter writer = resp.getWriter();
        queryHandlerWhitelist.forEach(handler -> writer.append(handler).append('\n'));
    }

    private void updateQueryHandlerMap() {
        List<String> whitelistPropertyList = MCRConfiguration2.getString(SOLR_CONFIG_PREFIX + "Proxy.WhiteList")
            .map(MCRConfiguration2::splitValue)
            .map(s -> s.collect(Collectors.toList()))
            .orElseGet(() -> Collections.singletonList("/select"));
        this.queryHandlerWhitelist = new HashSet<>(whitelistPropertyList);
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
        httpClient = MCRHttpUtils.getHttpClient();
    }

    @Override
    public void destroy() {
        httpClient.close();
        super.destroy();
    }

    private static ModifiableSolrParams toSolrParams(Map<String, String[]> parameters) {
        // to maintain order
        Map<String, String[]> copy = new LinkedHashMap<>(parameters);
        ModifiableSolrParams solrParams = new ModifiableSolrParams(copy);
        if (!parameters.containsKey("version") && !parameters.containsKey("wt")) {
            solrParams.set("version", SOLR_QUERY_XML_PROTOCOL_VERSION);
        }
        return solrParams;
    }
}
