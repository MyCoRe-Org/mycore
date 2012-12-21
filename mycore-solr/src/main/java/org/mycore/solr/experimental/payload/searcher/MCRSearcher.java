/**
 * 
 */
package org.mycore.solr.experimental.payload.searcher;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.payloads.PayloadSpanUtil;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.mycore.solr.experimental.payload.analyzers.MCRPayloadAnalyzer;



/**
 * @author shermann
 *
 */
public class MCRSearcher {

    public static void main(String arg[]) throws Exception {
        String indexDir = "/home/shermann/lucene-test/index";
        String q = "Silvio Haus";

        MCRSearcher.search(indexDir, q);
        //MCRSearcher.printPayloads(indexDir);
    }

    /**
     * @param indexDir
     * @param q
     * @throws Exception
     */
    public static void search(String indexDir, String q) throws Exception {
        Directory dir = FSDirectory.open(new File(indexDir));
        IndexReader reader = IndexReader.open(dir);
        IndexSearcher indexSearcher = new IndexSearcher(reader);

        QueryParser parser = new QueryParser(Version.LUCENE_CURRENT, "alto", new MCRPayloadAnalyzer(Version.LUCENE_CURRENT));
        Query query = parser.parse(q);

        System.out.print("Start searching...");
        long s = System.currentTimeMillis();
        TopDocs hits = indexSearcher.search(query, 10);
        System.out.println("done in " + (System.currentTimeMillis() - s) + "ms\n");

        if (hits.totalHits == 0) {
            System.out.println("Nothing found");
        }

        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document matchedDoc = indexSearcher.doc(scoreDoc.doc);
            System.out.println(matchedDoc.get("x"));
            HashMap<Term, List<byte[]>> payloads = getActualPayload(scoreDoc, query, indexSearcher.getIndexReader());
            for (Term term : payloads.keySet()) {
                List<byte[]> payload4Term = payloads.get(term);
                System.out.print(term + " payloads:");
                for (byte[] payload : payload4Term) {
                    System.out.print(" " + new String(payload, "UTF-16"));
                }
                System.out.println();
            }
            System.out.println("\n");
        }

        indexSearcher.close();
        reader.close();
    }

    private static HashMap<Term, List<byte[]>> getActualPayload(ScoreDoc scoreDoc, Query q, IndexReader reader) throws IOException {
        long start = System.currentTimeMillis();
        Set<Term> termSet = new HashSet<Term>();
        q.extractTerms(termSet);
        Iterator<Term> iterator = termSet.iterator();

        HashMap<Term, List<byte[]>> map = new HashMap<Term, List<byte[]>>();

        while (iterator.hasNext()) {
            Term term = iterator.next();
            //get the docs where this term occures
            TermDocs termDocs = reader.termDocs(term);

            // move straight to the term for the given doc
            // probably not needed 
            boolean exists = termDocs.skipTo(scoreDoc.doc);
            if (!exists) {
                continue;
            }

            List<byte[]> payloads = new Vector<byte[]>();
            // get the position for the current term
            TermPositions termPositions = reader.termPositions(term);
            // go straight to the tp for the given scoreDoc
            termPositions.skipTo(scoreDoc.doc);

            for (int i = 0; i < termPositions.freq(); i++) {
                termPositions.nextPosition();
                payloads.add(termPositions.getPayload(new byte[termPositions.getPayloadLength()], 0));
            }
            map.put(term, payloads);
        }
        System.out.println("Getting payloads for scoreDoc \"" + scoreDoc.doc + "\" took " + (System.currentTimeMillis() - start) + "ms");

        return map;
    }

    public static void printPayloadsForQuery(IndexSearcher indexSearcher, Query query) throws Exception {
        PayloadSpanUtil util = new PayloadSpanUtil(indexSearcher.getIndexReader());
        Collection<byte[]> payloadsForQuery = util.getPayloadsForQuery(query);

        for (byte[] b : payloadsForQuery) {
            System.out.println(new String(b, "UTF-16"));
        }
    }

    /**
     * @param indexDir
     * @throws Exception
     */
    public static void printPayloads(String indexDir) throws Exception {
        Directory dir = FSDirectory.open(new File(indexDir));
        IndexReader reader = IndexReader.open(dir);

        TermEnum termEnum = reader.terms();

        while (termEnum.next()) {
            Term term = termEnum.term();
            TermPositions tp = reader.termPositions(term);

            while (tp.next()) { //next term
                for (int i = 0; i < tp.freq(); i++) {
                    tp.nextPosition(); //next occ. of term
                    String payloadData = null;
                    boolean isPayloadAvailable = false;
                    if ((isPayloadAvailable = tp.isPayloadAvailable())) {
                        byte[] buf = new byte[tp.getPayloadLength()];
                        tp.getPayload(buf, 0);
                        payloadData = new String(buf, "UTF-16");
                    }
                    System.out.println(term + " isPayloadAvailable: " + isPayloadAvailable + " payloadData:" + payloadData);
                }
            }
        }
        reader.close();
    }
}
