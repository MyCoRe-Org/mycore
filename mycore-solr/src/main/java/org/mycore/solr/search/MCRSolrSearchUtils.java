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

package org.mycore.solr.search;

import static org.mycore.solr.auth.MCRSolrAuthenticationLevel.SEARCH;
import static org.mycore.solr.search.MCRSolrParameter.FIELD_LIST;
import static org.mycore.solr.search.MCRSolrParameter.QUERY;
import static org.mycore.solr.search.MCRSolrParameter.REQUEST_HANDLER;
import static org.mycore.solr.search.MCRSolrParameter.ROWS;
import static org.mycore.solr.search.MCRSolrParameter.START;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.InputStreamResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.jdom2.Document;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.parsers.bool.MCRSetCondition;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Some solr search utils.
 *
 * @author Matthias Eichner
 */
public final class MCRSolrSearchUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    private MCRSolrSearchUtils() {
    }

    /**
     * Returns the first document.
     *
     * @param solrClient solr server connection
     * @param query solr query
     * @return first solr document or null
     * @throws SolrServerException communication with the solr server failed in any way
     */
    public static SolrDocument first(SolrClient solrClient, String query) throws SolrServerException, IOException {
        ModifiableSolrParams p = new ModifiableSolrParams();
        p.set(QUERY, query);
        p.set(ROWS, 1);
        QueryRequest queryRequest = new QueryRequest(p);
        MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(queryRequest,
            SEARCH);
        QueryResponse response = queryRequest.process(solrClient);
        return response.getResults().isEmpty() ? null : response.getResults().getFirst();
    }

    @SuppressWarnings("rawtypes")
    public static SolrQuery getSolrQuery(MCRQuery query, Document input, HttpServletRequest request) {
        int rows = query.getNumPerPage();
        List<String> returnFields = query.getReturnFields();
        MCRCondition condition = query.getCondition();
        Map<String, List<MCRCondition>> table;

        if (condition instanceof MCRSetCondition setCondition) {
            table = MCRConditionTransformer.groupConditionsByIndex(setCondition);
        } else {
            // if there is only one condition its no set condition. we don't need to group
            LOGGER.warn("Condition is not SetCondition.");
            table = new HashMap<>();

            List<MCRCondition> conditionList = new ArrayList<>();
            conditionList.add(condition);

            table.put("metadata", conditionList);

        }

        boolean booleanAnd = !(condition instanceof MCROrCondition<?>);
        SolrQuery mergedSolrQuery = MCRConditionTransformer.buildMergedSolrQuery(query.getSortBy(), false, booleanAnd,
            table, rows, returnFields);
        String qt = input.getRootElement().getAttributeValue("qt");
        if (qt != null) {
            mergedSolrQuery.setParam(REQUEST_HANDLER, qt);
        }

        String mask = input.getRootElement().getAttributeValue("mask");
        if (mask != null) {
            mergedSolrQuery.setParam("mask", mask);
            mergedSolrQuery.setParam("_session", request.getParameter("_session"));
        }
        return mergedSolrQuery;
    }

    /**
     * Returns a list of ids found by the given query. Returns an empty list
     * when nothing is found.
     *
     * @param solrClient solr server connection
     * @param query solr query
     * @return list of id's
     */
    public static List<String> listIDs(SolrClient solrClient, String query) {
        ModifiableSolrParams p = new ModifiableSolrParams();
        p.set(QUERY, query);
        p.set(FIELD_LIST, "id");
        return stream(solrClient, p).map(doc -> doc.getFieldValue("id").toString()).collect(Collectors.toList());
    }

    /**
     * Creates a stream of SolrDocument's.
     *
     * @param solrClient the client to query
     * @param params solr parameter
     * @return stream of solr documents
     */
    public static Stream<SolrDocument> stream(SolrClient solrClient, SolrParams params) {
        return stream(solrClient, params, true, 1000);
    }

    public static Stream<SolrDocument> stream(SolrClient solrClient, SolrParams params, boolean parallel,
        int rowsPerRequest) {
        SolrDocumentSpliterator solrDocumentSpliterator = new SolrDocumentSpliterator(solrClient, params, 0,
            rowsPerRequest);
        return StreamSupport.stream(solrDocumentSpliterator, parallel);
    }

    /**
     * Streams raw xml solr response.
     *
     * @param client the client to query
     * @param params solr parameter
     * @return stream of the raw xml
     * @throws SolrServerException Communication with the solr server failed in any way.
     * @throws IOException If there is a low-level I/O error.
     */
    public static InputStream streamRawXML(SolrClient client, SolrParams params)
        throws SolrServerException, IOException {
        return streamRawXML(client, "/select", params);
    }

    /**
     * Streams raw xml solr response.
     * <p>
     * While <code>qt</code> parameter is deprecated, it still overwrites <code>path</code> if it's defined.
     *
     * @param client the client to query
     * @param path the request handler path, e.g. "/select"
     * @param params solr parameter
     * @return stream of the raw xml
     * @throws SolrServerException Communication with the solr server failed in any way.
     * @throws IOException If there is a low-level I/O error.
     */
    public static InputStream streamRawXML(SolrClient client, String path, SolrParams params)
        throws SolrServerException, IOException {
        QueryRequest request = new QueryRequest(params);
        request.setPath(Objects.requireNonNull(path));
        MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(request, SEARCH);
        InputStreamResponseParser responseParser = new InputStreamResponseParser("xml");
        request.setResponseParser(responseParser);
        NamedList<Object> nl = client.request(request);
        return (InputStream) nl.get("stream");
    }

    /**
     * Spliterator for solr documents.
     */
    public static class SolrDocumentSpliterator implements Spliterator<SolrDocument> {

        protected SolrClient solrClient;

        protected SolrParams params;

        protected long start;

        protected long rows;

        protected Long size;

        protected QueryResponse response;

        public SolrDocumentSpliterator(SolrClient solrClient, SolrParams params, long start, long rows) {
            this(solrClient, params, start, rows, null);
        }

        public SolrDocumentSpliterator(SolrClient solrClient, SolrParams params, long start, long rows, Long size) {
            this.solrClient = solrClient;
            this.params = params;
            this.start = start;
            this.rows = rows;
            this.size = size;
        }

        @Override
        public int characteristics() {
            return SIZED | SUBSIZED | ORDERED;
        }

        @Override
        public long estimateSize() {
            if (this.size == null) {
                ModifiableSolrParams sizeParams = new ModifiableSolrParams(this.params);
                sizeParams.set(START, 0);
                sizeParams.set(ROWS, 0);
                try {
                    QueryRequest queryRequest = new QueryRequest(sizeParams);
                    MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(queryRequest,
                        SEARCH);
                    QueryResponse response = queryRequest.process(solrClient);
                    this.size = response.getResults().getNumFound();
                } catch (SolrServerException | IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            return this.size;
        }

        @Override
        public void forEachRemaining(Consumer<? super SolrDocument> action) {
            if (action == null) {
                throw new IllegalArgumentException("Action cannot be null");
            }
            ModifiableSolrParams p = new ModifiableSolrParams(params);
            p.set(ROWS, (int) rows);
            long start = this.start;
            long size = estimateSize();
            long fetched = 0;
            while (fetched < size) {
                p.set(START, (int) (start + fetched));
                response = query(p);
                SolrDocumentList results = response.getResults();
                for (SolrDocument doc : results) {
                    action.accept(doc);
                }
                fetched += results.size();
            }
        }

        protected QueryResponse query(SolrParams params) {
            try {
                QueryRequest queryRequest = new QueryRequest(params);
                MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(queryRequest,
                    SEARCH);
                return queryRequest.process(solrClient);
            } catch (SolrServerException | IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super SolrDocument> action) {
            if (action == null) {
                throw new IllegalArgumentException("Action cannot be null");
            }
            long i = start;
            long size = estimateSize();
            if (size > 0) {
                if (response == null) {
                    ModifiableSolrParams p = new ModifiableSolrParams(params);
                    p.set(START, (int) i);
                    p.set(ROWS, (int) rows);
                    response = query(p);
                }
                action.accept(response.getResults().get(response.getResults().size() - (int) size));
                this.start = i + 1;
                this.size -= 1;
                return true;
            }
            return false;
        }

        @Override
        public Spliterator<SolrDocument> trySplit() {
            long s = estimateSize();
            long i = start;
            long l = rows;
            if (l >= s) {
                return null;
            }
            this.size = l;
            return new SolrDocumentSpliterator(solrClient, params, i + l, l, s - l);
        }
    }

}
