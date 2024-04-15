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

import static org.mycore.access.MCRAccessManager.PERMISSION_READ;
import static org.mycore.solr.MCRSolrConstants.SOLR_CONFIG_PREFIX;
import static org.mycore.solr.MCRSolrConstants.SOLR_QUERY_PATH;

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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.protocol.HTTP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.MultiMapSolrParams;
import org.apache.solr.common.util.NamedList;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.solr.proxy.MCRSolrProxyHttpClient.McrSolrHttpResult;

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

    static final Logger LOGGER = LogManager.getLogger(MCRSolrProxyServlet.class);

    private static final long serialVersionUID = 1L;

    public static final String QUERY_HANDLER_PAR_NAME = "qt";

    private MCRSolrProxyHttpClient mcrSolrHttpClient = new MCRSolrProxyHttpClient();

    private Set<String> queryHandlerWhitelist;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse resp = job.getResponse();
        //handle redirects
        if (request.getParameter(QUERY_HANDLER_PAR_NAME) != null
            || request.getAttribute(MCRSolrProxyHttpClient.MAP_KEY) != null) {
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

        McrSolrHttpResult mcrHttpSolrResult = mcrSolrHttpClient.handleQuery(queryHandlerPath, request, resp);

        HttpEntity solrResponseEntity = mcrHttpSolrResult.response().getEntity();

        if (solrResponseEntity != null) {
            try (InputStream solrResponseStream = solrResponseEntity.getContent()) {
                boolean isXML
                    = mcrHttpSolrResult.response().getFirstHeader(HTTP.CONTENT_TYPE).getValue().contains("/xml");
                boolean justCopyInput = !isXML;
                if (justCopyInput) {
                    // copy solr response to servlet outputstream
                    OutputStream servletOutput = resp.getOutputStream();
                    IOUtils.copy(solrResponseStream, servletOutput);
                } else {
                    MCRStreamContent solrStreamResponse = new MCRStreamContent(solrResponseStream,
                        mcrHttpSolrResult.solrHttpMethod().toString(), "response");
                    MCRLayoutService.instance().doLayout(request, resp, solrStreamResponse);
                }
            }
        }
        mcrHttpSolrResult.solrHttpMethod().releaseConnection();
    }

    /**
     * redirects to query handler by using value of 'qt' parameter
     */
    private void redirectToQueryHandler(HttpServletRequest request, HttpServletResponse resp)
        throws IOException {
        ModifiableSolrParams solrQueryParameter = mcrSolrHttpClient.getSolrQueryParameter(request);
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
    private void redirectToQueryHandler(Document input, HttpServletResponse resp)
        throws IOException {
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
    private void doRedirectToQueryHandler(HttpServletResponse resp, String queryHandlerPath,
        Map<String, String[]> parameters)
        throws IOException {
        String requestURL = new MessageFormat("{0}solr{1}{2}", Locale.ROOT)
            .format(new Object[] { getServletBaseURL(), queryHandlerPath,
                mcrSolrHttpClient.toSolrParams(parameters).toQueryString() });
        LOGGER.info("Redirect to: {}", requestURL);
        resp.sendRedirect(resp.encodeRedirectURL(requestURL));
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

    @Override
    public void init() throws ServletException {
        super.init();

        this.updateQueryHandlerMap();
        mcrSolrHttpClient.init();

    }

    @Override
    public void destroy() {
        mcrSolrHttpClient.close();
        super.destroy();
    }

}
