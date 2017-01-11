package org.mycore.solr.proxy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.NamedList;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrConstants;

class MCRSolrProxyUtils {

    private static final SolrQuery mbeansQuery = new SolrQuery().setParam("cat", "QUERYHANDLER")
        .setParam(CommonParams.OMIT_HEADER, true).setRequestHandler("/admin/mbeans");

    private static final Logger LOGGER = LogManager.getLogger(MCRSolrProxyUtils.class);

    private MCRSolrProxyUtils() {
    }

    static NamedList<NamedList<Object>> getQueryHandlerList() throws SolrServerException, IOException {
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        SolrRequest request = new QueryRequest(mbeansQuery);
        NamedList<Object> response = solrClient.request(request);
        //<lst name="solr-mbeans">
        @SuppressWarnings("unchecked")
        NamedList<NamedList<NamedList<Object>>> solrMBeans = (NamedList<NamedList<NamedList<Object>>>) response
            .getVal(0);
        //<lst name="QUERYHANDLER">
        NamedList<NamedList<Object>> queryHandler = solrMBeans.getVal(0);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("queryHandler: " + queryHandler.toString());
        }
        return queryHandler;
    }

    static Map<String, MCRSolrQueryHandler> getQueryHandlerMap() throws SolrServerException, IOException {
        NamedList<NamedList<Object>> list = null;
        list = getQueryHandlerList();
        int initialCapacity = list == null ? 2 : list.size();
        HashMap<String, MCRSolrQueryHandler> map = new HashMap<>(initialCapacity);
        MCRSolrQueryHandler standardHandler = getStandardHandler(list);
        map.put(standardHandler.getPath(), standardHandler);
        if (list != null) {
            for (Entry<String, NamedList<Object>> handler : list) {
                if (handler.getKey().charAt(0) != '/') {
                    continue;
                }
                map.put(handler.getKey(), new MCRSolrQueryHandler(handler.getKey(), handler.getValue()));
            }
        }
        return map;
    }

    static MCRSolrQueryHandler getStandardHandler(NamedList<NamedList<Object>> list) {
        MCRSolrQueryHandler standardHandler = null;
        if (list != null) {
            NamedList<Object> byPath = list.get(MCRSolrConstants.QUERY_PATH);
            if (byPath != null) {
                standardHandler = new MCRSolrQueryHandler(MCRSolrConstants.QUERY_PATH, byPath);
            } else {
                for (Entry<String, NamedList<Object>> test : list) {
                    if (test.getKey().equals("org.apache.solr.handler.StandardRequestHandler")) {
                        standardHandler = new MCRSolrQueryHandler(MCRSolrConstants.QUERY_PATH, test.getValue());
                        break;
                    }
                }
            }
        }
        if (standardHandler == null) {
            standardHandler = new MCRSolrQueryHandler(MCRSolrConstants.QUERY_PATH, null);

        }
        return standardHandler;
    }
}
