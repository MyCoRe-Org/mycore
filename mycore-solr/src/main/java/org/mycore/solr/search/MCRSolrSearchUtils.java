package org.mycore.solr.search;

import java.io.IOException;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

/**
 * Some solr search utils.
 * 
 * @author Matthias Eichner
 */
public abstract class MCRSolrSearchUtils {

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
        p.set("q", query);
        p.set("rows", 1);
        QueryResponse response = solrClient.query(p);
        return response.getResults().isEmpty() ? null : response.getResults().get(0);
    }

    /**
     * Returns a list of ids found by the given query. Returns an empty list
     * when nothing is found.
     * 
     * @param solrClient solr server connection
     * @param query solr query
     * @return list of id's
     * @throws SolrServerException communication with the solr server failed in any way
     */
    public static List<String> listIDs(SolrClient solrClient, String query) throws SolrServerException {
        ModifiableSolrParams p = new ModifiableSolrParams();
        p.set("q", query);
        p.set("fl", "id");
        return stream(solrClient, p).map(doc -> {
            return doc.getFieldValue("id").toString();
        }).collect(Collectors.toList());
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

        protected QueryResponse query(SolrParams params) {
            try {
                return solrClient.query(params);
            } catch (SolrServerException | IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super SolrDocument> action) {
            if (action == null)
                throw new NullPointerException();
            long i = start, size = estimateSize();
            if (size > 0) {
                if (response == null) {
                    ModifiableSolrParams p = new ModifiableSolrParams(params);
                    p.set("start", (int) i);
                    p.set("rows", (int) rows);
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
        public void forEachRemaining(Consumer<? super SolrDocument> action) {
            if (action == null)
                throw new NullPointerException();
            ModifiableSolrParams p = new ModifiableSolrParams(params);
            p.set("rows", (int) rows);
            long start = this.start, size = estimateSize(), fetched = 0;
            while (fetched < size) {
                p.set("start", (int) (start + fetched));
                response = query(p);
                SolrDocumentList results = response.getResults();
                for (SolrDocument doc : results) {
                    action.accept(doc);
                }
                fetched += results.size();
            }
        }

        @Override
        public Spliterator<SolrDocument> trySplit() {
            long s = estimateSize(), i = start, l = rows;
            if (l >= s) {
                return null;
            }
            this.size = l;
            return new SolrDocumentSpliterator(solrClient, params, i + l, l, s - l);
        }

        @Override
        public long estimateSize() {
            if (this.size == null) {
                ModifiableSolrParams sizeParams = new ModifiableSolrParams(this.params);
                sizeParams.set("start", 0);
                sizeParams.set("rows", 0);
                try {
                    QueryResponse response = solrClient.query(sizeParams);
                    this.size = response.getResults().getNumFound();
                } catch (SolrServerException | IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            return this.size;
        }

        @Override
        public int characteristics() {
            return Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.ORDERED;
        }
    }

}
