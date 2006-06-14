/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.backend.lucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexModifier;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRNormalizer;
import org.mycore.common.events.MCRShutdownThread;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRFieldValue;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearcher;
import org.mycore.services.fieldquery.MCRSortBy;
import org.mycore.services.plugins.TextFilterPluginManager;

/**
 * This class builds indexes from mycore meta data.
 * 
 * @author Harald Richter
 * @author Thomas Scheffler (yagee)
 */
public class MCRLuceneSearcher extends MCRSearcher {
    /** The logger */
    private final static Logger LOGGER = Logger.getLogger(MCRLuceneSearcher.class);

    String IndexDir = "";

    static String LOCK_DIR = "";

    static int INT_BEFORE = 10;

    static int DEC_BEFORE = 10;

    static int DEC_AFTER = 4;

    private static TextFilterPluginManager PLUGIN_MANAGER = null;

    static Analyzer analyzer = new PerFieldAnalyzerWrapper(new GermanAnalyzer());

    private IndexModifierThread indexer;

    public void init(String ID) {
        super.init(ID);

        MCRConfiguration config = MCRConfiguration.instance();
        IndexDir = config.getString(prefix + "IndexDir");
        LOGGER.info(prefix + "indexDir: " + IndexDir);
        File f = new File(IndexDir);
        if (!f.exists())
            f.mkdirs();
        if (!f.isDirectory()) {
            String msg = IndexDir + " is not a directory!";
            throw new MCRConfigurationException(msg);
        }
        if (!f.canWrite()) {
            String msg = IndexDir + " is not writeable!";
            throw new MCRConfigurationException(msg);
        }

        LOCK_DIR = config.getString("MCR.Lucene.LockDir");
        LOGGER.info("MCR.Lucene.LockDir: " + LOCK_DIR);
        f = new File(LOCK_DIR);
        if (!f.exists())
            f.mkdirs();
        if (!f.isDirectory()) {
            String msg = LOCK_DIR + " is not a directory!";
            throw new MCRConfigurationException(msg);
        }
        if (!f.canWrite()) {
            String msg = LOCK_DIR + " is not writeable!";
            throw new MCRConfigurationException(msg);
        }

        System.setProperty("org.apache.lucene.lockDir", LOCK_DIR);
        deleteLuceneLocks(LOCK_DIR, 0);

        try {
            indexer = new IndexModifierThread(IndexDir);
        } catch (Exception e) {
            throw new MCRException("Cannot start IndexModifier thread.", e);
        }
        // should work like GermanAnalyzer without stemming
        StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
        List fds = MCRFieldDef.getFieldDefs(getIndex());
        for (int i = 0; i < fds.size(); i++) {
            MCRFieldDef fd = (MCRFieldDef) (fds.get(i));
            if ("name".equals(fd.getDataType())) {
                ((PerFieldAnalyzerWrapper) analyzer).addAnalyzer(fd.getName(), standardAnalyzer);
            }
        }
        indexer.start();
    }

    private static void deleteLuceneLocks(String lockDir, long age) {
        File file = new File(lockDir);

        GregorianCalendar cal = new GregorianCalendar();

        File f[] = file.listFiles();
        for (int i = 0; i < f.length; i++) {
            if (!f[i].isDirectory()) {
                String n = f[i].getName().toLowerCase();
                if (n.startsWith("lucene") && n.endsWith(".lock")) {
                    // age of file in seconds
                    long l = (cal.getTimeInMillis() - f[i].lastModified()) / 1000;
                    if (l > age) {
                        LOGGER.info("Delete lucene lock file " + f[i].getAbsolutePath() + " Age " + l);
                        f[i].delete();
                    }
                }
            }
        }
    }

    public static String handleNumber(String content, String type, long add) {
        int before, after;
        int dez;
        long l;
        if ("decimal".equals(type)) {
            before = DEC_BEFORE;
            after = DEC_AFTER;
            dez = before + after;
            double d = Double.parseDouble(content);
            d = d * Math.pow(10, after) + Math.pow(10, dez);
            l = (long) d;
        } else {
            before = INT_BEFORE;
            dez = before;
            l = Long.parseLong(content);
            l = l + (long) (Math.pow(10, dez) + 0.1);
        }

        long m = l + add;
        String n = "0000000000000000000";
        String h = Long.toString(m);
        return n.substring(0, dez + 1 - h.length()) + h;
    }

    protected void removeFromIndex(String entryID) {
        LOGGER.info("MCRLuceneSearcher removing indexed data of " + entryID);

        try {
            deleteLuceneDocument("mcrid", entryID);
        } catch (Exception e) {
            LOGGER.warn(e.getMessage());
        }
    }

    /**
     * Delete all documents in Lucene with id
     * 
     * @param fieldname
     *            string name of lucene field with stored id
     * @param id
     *            string document id
     * @param indexDir *
     *            the directory where index is stored
     * 
     */
    public void deleteLuceneDocument(String fieldname, String id) throws Exception {
        Term deleteTerm = new Term(fieldname, id);
        QueueElement qe = QueueElement.removeAction(deleteTerm);
        addQueueElement(qe);
    }

    public MCRResults search(MCRCondition condition, int maxResults, List sortBy, boolean addSortData) {
        MCRResults results = new MCRResults();

        try {
            List f = new ArrayList();
            f.add(condition.toXML());

            boolean reqf = true;
            // required flag Term with AND (true) or OR (false) combined
            Query luceneQuery = MCRBuildLuceneQuery.buildLuceneQuery(null, reqf, f, analyzer);
            LOGGER.debug("Lucene Query: " + luceneQuery.toString());
            results = getLuceneHits(luceneQuery, maxResults, sortBy, addSortData);
        } catch (Exception e) {
            LOGGER.error("Exception in MCRLuceneSearcher", e);
        }

        return results;
    }

    /**
     * method does lucene query
     * 
     * @return result set
     */
    private MCRResults getLuceneHits(Query luceneQuery, int maxResults, List sortBy, boolean addSortData) throws Exception {
        if (maxResults <= 0)
            maxResults = 1000000;

        IndexSearcher searcher = null;
        try {
            searcher = new IndexSearcher(IndexDir);
        } catch (IOException e) {
            LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
            LOGGER.error(MCRException.getStackTraceAsString(e));
            deleteLuceneLocks(LOCK_DIR, 100);
        }

        TopDocs hits = searcher.search(luceneQuery, null, maxResults);
        int found = hits.scoreDocs.length;

        LOGGER.info("Number of Objects found : " + found);

        MCRResults result = new MCRResults();

        for (int i = 0; i < found; i++) {
            // org.apache.lucene.document.Document doc = hits.doc(i);
            org.apache.lucene.document.Document doc = searcher.doc(hits.scoreDocs[i].doc);

            String id = doc.get("returnid");
            MCRHit hit = new MCRHit(id);

            for (int j = 0; j < sortBy.size(); j++) {
                MCRSortBy sb = (MCRSortBy) sortBy.get(j);
                MCRFieldDef fds = sb.getField();
                if (null != fds) {
                    String field = fds.getName();
                    String value = doc.get(field);
                    if (null != value) {
                        MCRFieldDef fd = MCRFieldDef.getDef(field);
                        MCRFieldValue fv = new MCRFieldValue(fd, value);
                        hit.addSortData(fv);
                    }
                }
            }
            result.addHit(hit);
        }

        searcher.close();

        return result;
    }

    protected void addToIndex(String entryID, String returnID, List fields) {
        LOGGER.info("MCRLuceneSearcher indexing data of " + entryID);

        if ((fields == null) || (fields.size() == 0)) {
            return;
        }

        try {
            Document doc = buildLuceneDocument(fields);
            doc.add(new Field("mcrid", entryID, Field.Store.YES, Field.Index.UN_TOKENIZED));
            doc.add(new Field("returnid", returnID, Field.Store.YES, Field.Index.UN_TOKENIZED));
            LOGGER.debug("lucene document build " + entryID);
            addDocumentToLucene(doc, analyzer);
        } catch (Exception e) {
            LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
            LOGGER.error(MCRException.getStackTraceAsString(e));
        }
    }

    /**
     * Adds document to Lucene
     * 
     * @param doc
     *            lucene document to add to index
     * 
     */
    private void addDocumentToLucene(Document doc, Analyzer analyzer) throws Exception {
        QueueElement qe = QueueElement.addAction(doc, analyzer);
        addQueueElement(qe);
    }

    private void addQueueElement(QueueElement qe) {
        indexer.queue.offer(qe);
    }

    /**
     * Build lucene document from transformed xml list
     * 
     * @param fields
     *            corresponding to lucene fields
     * 
     * @return The lucene document
     * 
     */
    public static Document buildLuceneDocument(List fields) throws Exception {
        Document doc = new Document();

        for (int i = 0; i < fields.size(); i++) {
            MCRFieldValue field = (MCRFieldValue) (fields.get(i));
            String name = field.getField().getName();
            String type = field.getField().getDataType();
            String content = field.getValue();
            MCRFile mcrfile = field.getFile();

            if (null != mcrfile) {
                if (PLUGIN_MANAGER == null) {
                    PLUGIN_MANAGER = TextFilterPluginManager.getInstance();
                }
                if (PLUGIN_MANAGER.isSupported(mcrfile.getContentType())) {
                    LOGGER.debug("####### Index MCRFile: " + mcrfile.getPath());

                    BufferedReader in = new BufferedReader(PLUGIN_MANAGER.transform(mcrfile.getContentType(), mcrfile.getContentAsInputStream()));
                    String s;
                    StringBuffer text = new StringBuffer();
                    while ((s = in.readLine()) != null) {
                        text.append(s).append(" ");
                    }

                    s = text.toString();
                    s = MCRNormalizer.normalizeString(s);

                    doc.add(new Field(name, s, Field.Store.NO, Field.Index.TOKENIZED));
                }
            } else {
                if ("date".equals(type) || "time".equals(type) || "timestamp".equals(type)) {
                    type = "identifier";
                } else if ("boolean".equals(type)) {
                    content = "true".equals(content) ? "1" : "0";
                    type = "identifier";
                } else if ("decimal".equals(type)) {
                    content = handleNumber(content, "decimal", 0);
                    type = "identifier";
                } else if ("integer".equals(type)) {
                    content = handleNumber(content, "integer", 0);
                    type = "identifier";
                }

                if (type.equals("identifier")) {
                    doc.add(new Field(name, content, Field.Store.YES, Field.Index.UN_TOKENIZED));
                }

                if (type.equals("Text") || type.equals("name") || (type.equals("text") && field.getField().isSortable())) {
                    doc.add(new Field(name, content, Field.Store.YES, Field.Index.TOKENIZED));
                } else if (type.equals("text")) {
                    doc.add(new Field(name, content, Field.Store.NO, Field.Index.TOKENIZED));
                }
            }
        }

        return doc;
    }

    public void addSortData(Iterator hits, List sortBy) {
        try {
            IndexSearcher searcher = new IndexSearcher(IndexDir);

            if (null == searcher) {
                return;
            }

            while (hits.hasNext()) {
                MCRHit hit = (MCRHit) hits.next();
                String id = hit.getID();
                Term te1 = new Term("mcrid", id);

                TermQuery qu = new TermQuery(te1);

                Hits hitl = searcher.search(qu);
                if (hitl.length() > 0) {
                    org.apache.lucene.document.Document doc = hitl.doc(0);
                    for (int j = 0; j < sortBy.size(); j++) {
                        MCRFieldDef fd = (MCRFieldDef) sortBy.get(j);
                        String value = doc.get(fd.getName());
                        if (null != value) {
                            MCRFieldValue fv = new MCRFieldValue(fd, value);
                            hit.addSortData(fv);
                        }
                    }
                }
            }

            searcher.close();
        } catch (IOException e) {
            LOGGER.error("Exception in MCRLuceneSearcher (addSortData)", e);
        }
    }

    /**
     * a single thread for every <code>indexDir</code> that performs all write
     * operations.
     * 
     * @author Thomas Scheffler (yagee)
     */
    private static class IndexModifierThread extends Thread implements MCRShutdownThread.Closeable {
        private Queue queue;

        private boolean running;

        private IndexModifier indexModifier;

        public IndexModifierThread(String indexDir) throws Exception {
            queue = new Queue();
            this.running = true;
            indexModifier = getLuceneModifier(indexDir, true);
            MCRShutdownThread.addCloseable(this);
        }

        public void close() {
            LOGGER.debug("close()");
            running = false;
            interrupt();
        }

        public void run() {
            LOGGER.debug("IndexModifierThread started");
            while (running) {
                QueueElement qe;
                try {
                    qe = queue.poll();
                } catch (InterruptedException e1) {
                    if (running) {
                        LOGGER.warn("Catched InterruptException.", e1);
                    }
                    continue;
                }
                if (qe.delete) {
                    try {
                        deleteDocument(qe);
                    } catch (IOException e) {
                        LOGGER.error("Error while writing Lucene Document: " + qe.doc.toString(), e);
                    }
                } else {
                    try {
                        addDocument(qe);
                    } catch (IOException e) {
                        LOGGER.error("Error while writing Lucene Document: " + qe.doc.toString(), e);
                    }
                }
            }
            try {
                if (indexModifier != null) {
                    indexModifier.close();
                }
            } catch (IOException e) {
                LOGGER.warn("Error while closing IndexWriter.", e);
            }
            MCRShutdownThread.removeCloseable(this);
            LOGGER.debug("IndexModifierThread stopped");
        }

        private void addDocument(QueueElement qe) throws IOException {
            indexModifier.addDocument(qe.doc, qe.analyzer);
        }

        private void deleteDocument(QueueElement qe) throws IOException {
            indexModifier.deleteDocuments(qe.deleteTerm);
        }

        /**
         * Get LuceneModifier
         * 
         * @param indexDir
         *            directory where lucene index is store first check
         *            existance of index directory, if it does nor exist create
         *            it
         * 
         * @return the lucene modifier, calling programm must close modifier
         */
        private IndexModifier getLuceneModifier(String indexDir, boolean first) throws Exception {
            IndexModifier modifier;
            Analyzer analyzer = new GermanAnalyzer();

            // does directory for text index exist, if not build it
            if (first) {
                File file = new File(indexDir);

                if (!file.exists()) {
                    LOGGER.info("The Directory doesn't exist: " + indexDir + " try to build it");

                    IndexModifier modifier2 = new IndexModifier(indexDir, analyzer, true);
                    modifier2.close();
                } else if (file.isDirectory()) {
                    if (0 == file.list().length) {
                        LOGGER.info("No Entries in Directory, initialize: " + indexDir);

                        IndexModifier modifier2 = new IndexModifier(indexDir, analyzer, true);
                        modifier2.close();
                    }
                }
            } // if ( first

            modifier = new IndexModifier(indexDir, analyzer, false);
            modifier.setMergeFactor(200);
            modifier.setMaxBufferedDocs(2000);

            return modifier;
        }
    }

    /**
     * a thread safe implementation of a queue.
     * 
     * TODO: should be replaced by java.util.concurrent.ConcurrentLinkedQueue<E>
     * if we switch to J2SE > 5
     * 
     * @author Thomas Scheffler (yagee)
     */
    private static class Queue {
        private QueueElement head_;

        private QueueElement last_;

        private final Object lock_ = new Object();

        /**
         * the number of threads waiting for a poll
         */
        private int waitingForPoll_ = 0;

        // LinkedList list;
        public Queue() {
            head_ = new QueueElement();
            last_ = head_;
        }

        public QueueElement poll() throws InterruptedException {
            QueueElement qe = extract();
            if (qe != null) {
                return qe;
            } else {
                // we wait until the next element arrives
                synchronized (lock_) {
                    try {
                        ++waitingForPoll_;
                        while (true) {
                            // check for new element
                            qe = extract();
                            if (qe != null) {
                                --waitingForPoll_;
                                return qe;
                            } else {
                                lock_.wait();// wait
                            }
                        }
                    } catch (InterruptedException ex) {
                        --waitingForPoll_;
                        lock_.notify();
                        throw ex;
                    }
                }
            }
        }

        private synchronized QueueElement extract() {
            synchronized (head_) {
                QueueElement o = null;
                QueueElement first = head_.next;
                if (first != null) {
                    o = first;
                    // delete reference for the gc
                    head_.next = null;
                    head_ = first;
                }
                return o;
            }
        }

        public boolean offer(QueueElement o) {
            synchronized (lock_) {
                synchronized (last_) {
                    last_.next = o;
                    last_ = o;
                }
                if (waitingForPoll_ > 0) {
                    lock_.notify();
                }
            }
            return true;
        }
    }

    /**
     * holds info on what to do with the lucene index.
     * 
     * currently this include adding and deleting lucene documents.
     * 
     * @author Thomas Scheffler (yagee)
     */
    private static class QueueElement {
        private Document doc;

        private Analyzer analyzer;

        private boolean delete = false;

        private Term deleteTerm;

        // internal Queue structure
        private QueueElement next;

        private QueueElement() {
        }

        public static QueueElement addAction(Document doc, Analyzer analyzer) {
            QueueElement e = new QueueElement();
            e.doc = doc;
            e.analyzer = analyzer;
            return e;
        }

        public static QueueElement removeAction(Term deleteTerm) {
            QueueElement e = new QueueElement();
            e.delete = true;
            e.deleteTerm = deleteTerm;
            return e;
        }

        public String toString() {
            if (doc != null)
                return doc.toString();
            if (deleteTerm != null)
                return deleteTerm.toString();
            return "empty QueueElement";
        }
    }
}