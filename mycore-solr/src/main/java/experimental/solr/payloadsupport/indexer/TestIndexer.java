package experimental.solr.payloadsupport.indexer;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import experimental.solr.payloadsupport.analyzers.PayloadAnalyzer;
import experimental.solr.payloadsupport.analyzers.XML2StringWithPayloadProvider;


public class TestIndexer {
    private IndexWriter writer;

    /**
     * @param arg
     */
    public static void main(String arg[]) throws Exception {
        /* where the index is stored */
        String INDEX_DIR = "/home/shermann/lucene-test/index";

        /* where the data resides*/
        String DATA_DIR = "/home/shermann/lucene-test/";

        TestIndexer indexer = new TestIndexer(INDEX_DIR);

        int numIndexed = indexer.index(DATA_DIR);
        System.out.println("Indexed " + numIndexed + " documents");
        indexer.close();
    }

    public TestIndexer(String indexDir) throws Exception {
        Directory dir = FSDirectory.open(new File(indexDir));
        PayloadAnalyzer payloadAnalyzer = new PayloadAnalyzer(Version.LUCENE_CURRENT);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_CURRENT, payloadAnalyzer);
        writer = new IndexWriter(dir, config);
    }

    /**
     * @param dataDir
     * @return
     * @throws Exception
     */
    public int index(String dataDir) throws Exception {
        File[] files = new File(dataDir).listFiles();
        for (File f : files) {
            if (!f.isDirectory()) {
                indexFile(f);
            }
        }
        return writer.numDocs();
    }

    /**
     * @param f
     * @throws Exception
     */
    private void indexFile(File f) throws Exception {
        System.out.print("Indexing " + f.getCanonicalPath());
        long s = System.currentTimeMillis();
        Document doc = getDocument(f);
        if (doc != null) {
            writer.addDocument(doc);
            System.out.println(" (in " + (System.currentTimeMillis() - s) + "ms)");
        }
    }

    /**
     * @param f
     * @return
     */
    private Document getDocument(File f) throws Exception {
        Document doc = new Document();
        XML2StringWithPayloadProvider keywordsWithPayloadProvider = new XML2StringWithPayloadProvider(f);
        doc.add(new Field("alto", new StringReader(keywordsWithPayloadProvider.getFlatDocument()), TermVector.WITH_POSITIONS_OFFSETS));
        doc.add(new Field("x", f.getCanonicalPath(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        return doc;
    }

    public void close() throws IOException {
        writer.close();
    }

}