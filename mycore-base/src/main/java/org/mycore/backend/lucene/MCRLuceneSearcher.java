/*
 * 
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRNormalizer;
import org.mycore.common.events.MCRShutdownHandler;
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
public class MCRLuceneSearcher extends MCRSearcher implements MCRShutdownHandler.Closeable {
    private static final String SORTABLE_SUFFIX = ".sortable";

    /** The logger */
    private final static Logger LOGGER = Logger.getLogger(MCRLuceneSearcher.class);

    static int INT_BEFORE = 10;

    static int DEC_BEFORE = 10;

    static int DEC_AFTER = 4;

    private static TextFilterPluginManager PLUGIN_MANAGER = null;

    static Analyzer analyzer = new PerFieldAnalyzerWrapper(new GermanAnalyzer());

    File IndexDir;

    private IndexWriteExecutor modifyExecutor;

    private boolean useRamDir = false;

    private RAMDirectory ramDir = null;

    private IndexWriter writerRamDir;

    private int ramDirEntries = 0;

    private IndexReader indexReader = null;

    private IndexSearcher indexSearcher = null;

    private FSDirectory indexDir;

    private Vector<MCRFieldDef> addableFields = new Vector<MCRFieldDef>();

    public void init(String ID) {
        super.init(ID);

        MCRConfiguration config = MCRConfiguration.instance();
        IndexDir = new File(config.getString(prefix + "IndexDir"));
        LOGGER.info(prefix + "indexDir: " + IndexDir);
        if (!IndexDir.exists())
            IndexDir.mkdirs();
        if (!IndexDir.isDirectory()) {
            String msg = IndexDir + " is not a directory!";
            throw new MCRConfigurationException(msg);
        }
        if (!IndexDir.canWrite()) {
            String msg = IndexDir + " is not writeable!";
            throw new MCRConfigurationException(msg);
        }

        // is index directory initialized, .....?
        try {
            indexDir = FSDirectory.open(IndexDir);
            IndexWriter writer = MCRLuceneTools.getLuceneWriter(indexDir, true);
            writer.close();
        } catch (IOException e) {
            LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
            LOGGER.error(MCRException.getStackTraceAsString(e));
        } catch (Exception e) {
            LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
            LOGGER.error(MCRException.getStackTraceAsString(e));
        }

        deleteLuceneLockFile();

        long writeLockTimeout = config.getLong("MCR.Lucene.writeLockTimeout", 5000);
        LOGGER.debug("Property MCR.Lucene.writeLockTimeout: " + writeLockTimeout);
        IndexWriter.setDefaultWriteLockTimeout(writeLockTimeout);

        try {
            modifyExecutor = new IndexWriteExecutor(new LinkedBlockingQueue<Runnable>(), indexDir);
        } catch (Exception e) {
            throw new MCRException("Cannot start IndexWriter thread.", e);
        }
        // should work like GermanAnalyzer without stemming and removing of stopwords
        SimpleAnalyzer simpleAnalyzer = new SimpleAnalyzer();
        List<MCRFieldDef> fds = MCRFieldDef.getFieldDefs(getIndex());
        for (MCRFieldDef fd : fds) {
            if ("name".equals(fd.getDataType())) {
                ((PerFieldAnalyzerWrapper) analyzer).addAnalyzer(fd.getName(), simpleAnalyzer);
            }
            if (fd.isAddable())
                addableFields.add(fd);
        }
        MCRShutdownHandler.getInstance().addCloseable(this);
    }

    private void deleteLuceneLockFile() {
        GregorianCalendar cal = new GregorianCalendar();

        File file = new File(IndexDir, "write.lock");

        if (file.exists()) {
            long l = (cal.getTimeInMillis() - file.lastModified()) / 1000; // age of file in seconds
            if (l > 100) {
                LOGGER.info("Delete lucene lock file " + file.getAbsolutePath() + " Age " + l);
                file.delete();
            }
        }
    }

    public static String handleNumber2(String content, String type, long add) {
        int before, after;
        int dez;
        long l;
        try {
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
                if (content.indexOf('.') > 0)
                    content = content.substring(content.lastIndexOf('.') + 1);
                l = Long.parseLong(content);
                l = l + (long) (Math.pow(10, dez) + 0.1);
            }
            long m = l + add;
            String n = "0000000000000000000";
            String h = Long.toString(m);
            return n.substring(0, dez + 1 - h.length()) + h;
        } catch (Exception all) {
            LOGGER.info("MCRLuceneSearcher can't format this Number, ignore this content: " + content);
            return "0";
        }
    }

    public void removeFromIndex(String entryID) {
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
        IndexWriterAction modifyAction = IndexWriterAction.removeAction(modifyExecutor, deleteTerm);
        modifyIndex(modifyAction);
    }

    /**
     * As opposed to {@link MCRSearcher} the returned MCRResult is read only.
     * @see MCRSearcher#search(MCRCondition, int, List, boolean)
     */
    public MCRResults search(MCRCondition condition, int maxResults, List<MCRSortBy> sortBy, boolean addSortData) {
        try {
            List<Element> f = new ArrayList<Element>();
            f.add(condition.toXML());

            boolean reqf = true;
            // required flag Term with AND (true) or OR (false) combined
            Query luceneQuery = MCRBuildLuceneQuery.buildLuceneQuery(null, reqf, f, analyzer);
            LOGGER.debug("Lucene Query: " + luceneQuery.toString());
            return getLuceneHits(luceneQuery, maxResults, sortBy, addSortData);
        } catch (Exception e) {
            LOGGER.error("Exception in MCRLuceneSearcher", e);
            return new MCRResults();
        }
    }

    /**
     * method does lucene query
     * 
     * @return result set
     */
    private MCRResults getLuceneHits(Query luceneQuery, int maxResults, List<MCRSortBy> sortBy, boolean addSortData) throws Exception {
        if (maxResults <= 0)
            maxResults = 1000000;

        long start = System.currentTimeMillis();
        if (indexReader == null && indexSearcher == null) {
            indexReader = IndexReader.open(indexDir, true);
            indexSearcher = new IndexSearcher(indexReader);
        } else {
            if (!indexReader.isCurrent()) {
                IndexReader newReader = indexReader.reopen();
                if (newReader != indexReader) {
                    LOGGER.info("new Searcher for index: " + ID);
                    indexReader.close();
                    indexSearcher.close();
                    indexReader = newReader;
                    indexSearcher = new IndexSearcher(indexReader);
                }
            }
        }
        if (indexReader.maxDoc() == 0) {
            //lucene index is empty
            LOGGER.warn("Searching on empty index " + super.index);
            return new MCRResults();
        }
        Sort sortFields = buildSortFields(sortBy);
        if (sortFields.getSort().length == 0) {
            //one sort criteria is needed for TopFieldCollector, using internal document id then
            sortFields.setSort(SortField.FIELD_DOC);
        }
        TopFieldCollector collector = TopFieldCollector.create(sortFields, maxResults, false, false, false, false);
        indexSearcher.search(luceneQuery, collector);
        //Lucene 2.4.1 has a bug: be sure to call collector.topDocs() just once
        //see http://issues.apache.org/jira/browse/LUCENE-942
        TopFieldDocs topFieldDocs = (TopFieldDocs) collector.topDocs();
        LOGGER.info("Number of Objects found: " + topFieldDocs.scoreDocs.length + " Time for Search: "
                + (System.currentTimeMillis() - start));
        return new MCRLuceneResults(indexSearcher, topFieldDocs, addableFields);
    }

    private Sort buildSortFields(List<MCRSortBy> sortBy) {
        ArrayList<SortField> sortList = new ArrayList<SortField>(sortBy.size());
        for (MCRSortBy sortByElement : sortBy) {
            SortField sortField;
            if (sortByElement.getField().getName().equals("score"))
                sortField = SortField.FIELD_SCORE;
            else {
                String name = sortByElement.getField().getName();
                //TODO: use dataType to get FieldType (how to handle dates here?)
                int fieldType = getFieldType(sortByElement.getField());
                if (isTokenized(sortByElement.getField())) {
                    name += SORTABLE_SUFFIX;
                }
                sortField = new SortField(name, fieldType, sortByElement.getSortOrder() == MCRSortBy.DESCENDING);
            }
            sortList.add(sortField);
        }
        if (LOGGER.isDebugEnabled()) {
            for (SortField sortField : sortList) {
                String name = (SortField.FIELD_SCORE == sortField ? "score" : sortField.getField());
                LOGGER.debug("Sort by: " + name + (sortField.getReverse() ? " descending" : " accending"));
            }
        }
        return new Sort(sortList.toArray(new SortField[0]));
    }

    private int getFieldType(MCRFieldDef fieldDef) {
        String mcrType = fieldDef.getDataType();
        if (mcrType.equals("identifier") || mcrType.equals("name") || mcrType.equals("text") || mcrType.equals("boolean")) {
            return SortField.STRING;
        }
        if (mcrType.equals("date") || mcrType.equals("timestamp") || mcrType.equals("time") || mcrType.equals("integer")) {
            return SortField.LONG;
        }
        if (mcrType.equals("decimal")) {
            return SortField.FLOAT;
        }
        LOGGER.warn("Cannot match " + mcrType + " to a Lucene field type. Using STRING as default.");
        return SortField.STRING;
    }

    /**
     * @param sortBy
     * @param doc
     *            lucene document to get sortdata from
     * @param hit
     *            sortdata are added
     * @param score
     *            of hit
     */
    private void addSortDataToHit(List<MCRSortBy> sortBy, org.apache.lucene.document.Document doc, MCRHit hit, String score) {
        for (int j = 0; j < sortBy.size(); j++) {
            MCRSortBy sb = sortBy.get(j);
            MCRFieldDef fds = sb.getField();
            if (null != fds) {
                String field = fds.getName();
                if ("score".equals(field)) {
                    if (null != score) {
                        MCRFieldDef fd = MCRFieldDef.getDef(field);
                        MCRFieldValue fv = new MCRFieldValue(fd, score);
                        hit.addSortData(fv);
                    }
                } else {
                    if (isTokenized(fds)) {
                        field += SORTABLE_SUFFIX;
                    }
                    String values[] = doc.getValues(field);
                    for (int i = 0; i < values.length; i++) {
                        MCRFieldValue fv = new MCRFieldValue(fds, values[i]);
                        hit.addSortData(fv);
                    }
                }
            }
        }
    }

    public void addToIndex(String entryID, String returnID, List<MCRFieldValue> fields) {
        LOGGER.info("MCRLuceneSearcher indexing data of " + entryID);

        if ((fields == null) || (fields.size() == 0)) {
            return;
        }

        try {
            Document doc = buildLuceneDocument(fields);
            doc.add(new Field("mcrid", entryID, Field.Store.YES, Field.Index.NOT_ANALYZED));
            doc.add(new Field("returnid", returnID, Field.Store.YES, Field.Index.NOT_ANALYZED));
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
        if (useRamDir) {
            writerRamDir.addDocument(doc, analyzer);
            ramDirEntries++;
            if (ramDirEntries > 5000) {
                writerRamDir.close();
                IndexWriterAction modifyAction = IndexWriterAction.addRamDir(modifyExecutor, ramDir);
                modifyIndex(modifyAction);
                ramDir = new RAMDirectory();
                writerRamDir = new IndexWriter(ramDir, analyzer, true, MaxFieldLength.LIMITED);
                ramDirEntries = 0;
            }
        } else {
            IndexWriterAction modifyAction = IndexWriterAction.addAction(modifyExecutor, doc, analyzer);
            modifyIndex(modifyAction);
        }
    }

    private void modifyIndex(IndexWriterAction modifyAction) {
        modifyExecutor.submit(modifyAction);
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
    public static Document buildLuceneDocument(List<MCRFieldValue> fields) throws Exception {
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

                    BufferedReader in = new BufferedReader(PLUGIN_MANAGER.transform(mcrfile.getContentType(), mcrfile
                            .getContentAsInputStream()));
                    String s;
                    StringBuffer text = new StringBuffer();
                    while ((s = in.readLine()) != null) {
                        text.append(s).append(" ");
                    }

                    s = text.toString();
                    s = MCRNormalizer.normalizeString(s);

                    doc.add(new Field(name, s, Field.Store.NO, Field.Index.ANALYZED));
                }
            } else {
                if ("date".equals(type) || "time".equals(type) || "timestamp".equals(type)) {
                    doc.add(new NumericField(name).setLongValue(MCRLuceneTools.getLongValue(content)));
                } else if ("boolean".equals(type)) {
                    content = "true".equals(content) ? "1" : "0";
                    type = "identifier";
                } else if ("decimal".equals(type)) {
                    doc.add(new NumericField(name).setFloatValue(Float.parseFloat(content)));
                } else if ("integer".equals(type)) {
                    doc.add(new NumericField(name).setLongValue(Long.parseLong(content)));
                }

                if (type.equals("identifier")) {
                    doc.add(new Field(name, content, Field.Store.YES, Field.Index.NOT_ANALYZED));
                }

                if (type.equals("Text") || type.equals("name") || (type.equals("text") && field.getField().isSortable())) {
                    doc.add(new Field(name, content, Field.Store.YES, Field.Index.ANALYZED));
                    if (field.getField().isSortable())
                        doc.add(new Field(name + SORTABLE_SUFFIX, content, Field.Store.YES, Field.Index.NOT_ANALYZED));
                } else if (type.equals("text")) {
                    doc.add(new Field(name, content, Field.Store.NO, Field.Index.ANALYZED));
                }
            }
        }

        return doc;
    }

    private boolean isTokenized(MCRFieldDef fieldDef) {
        String type = fieldDef.getDataType();
        if (type.equals("Text") || type.equals("name") || type.equals("text"))
            return true;
        return false;
    }

    public void addSortData(Iterator<MCRHit> hits, List<MCRSortBy> sortBy) {
        try {
            while (hits.hasNext()) {
                MCRHit hit = hits.next();
                String id = hit.getID();
                Term te1 = new Term("mcrid", id);

                TermQuery qu = new TermQuery(te1);

                TopDocs hitl = indexSearcher.search(qu, 1);
                if (hitl.totalHits > 0) {
                    org.apache.lucene.document.Document doc = indexSearcher.doc(hitl.scoreDocs[0].doc);
                    addSortDataToHit(sortBy, doc, hit, null);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Exception in MCRLuceneSearcher (addSortData)", e);
        }
    }

    public void clearIndex() {
        try {
            IndexWriter writer = new IndexWriter(indexDir, analyzer, true, MaxFieldLength.LIMITED);
            writer.close();
        } catch (IOException e) {
            LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
            LOGGER.error(MCRException.getStackTraceAsString(e));
        }
    }

    public void clearIndex(String fieldname, String value) {
        try {
            deleteLuceneDocument(fieldname, value);
        } catch (Exception e) {
            LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
            LOGGER.error(MCRException.getStackTraceAsString(e));
        }
    }

    public void notifySearcher(String mode) {
        LOGGER.info("mode: " + mode);

        handleRamDir();

        useRamDir = false;

        if ("rebuild".equals(mode) || "insert".equals(mode)) {
            try {
                ramDir = new RAMDirectory();
                writerRamDir = new IndexWriter(ramDir, analyzer, true, MaxFieldLength.LIMITED);
                ramDirEntries = 0;
                useRamDir = true;
            } catch (Exception e) {
            }
        } else if ("optimize".equals(mode)) {
            IndexWriterAction modifyAction = IndexWriterAction.optimizeAction(modifyExecutor);
            modifyIndex(modifyAction);
        } else if (!"finish".equals(mode))
            LOGGER.error("invalid mode " + mode);
    }

    private void handleRamDir() {
        if (useRamDir) {
            try {
                writerRamDir.close();
            } catch (IOException e) {
                LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
                LOGGER.error(MCRException.getStackTraceAsString(e));
            }
            if (ramDirEntries > 0) {
                IndexWriterAction modifyAction = IndexWriterAction.addRamDir(modifyExecutor, ramDir);
                modifyIndex(modifyAction);
            }
        }
    }

    public void close() {
        try {
            if (null != indexReader)
                indexReader.close();
            if (null != indexSearcher)
                indexSearcher.close();
        } catch (IOException e1) {
            LOGGER.warn("Error while closing indexreader " + toString(), e1);
        }
        handleRamDir();
        LOGGER.info("Closing " + toString() + "...");
        modifyExecutor.shutdown();
        try {
            modifyExecutor.awaitTermination(60 * 60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.warn("Error while closing " + toString(), e);
        }
        LOGGER.info("Processed " + modifyExecutor.getCompletedTaskCount() + " modification requests.");
    }

    public String toString() {
        return getClass().getSimpleName() + ":" + ID;
    }

    private static class IndexWriteExecutor extends ThreadPoolExecutor {
        boolean modifierClosed, firstJob, closeModifierEarly;

        private IndexWriter indexWriter;

        private FSDirectory indexDir;

        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        private final DelayedIndexWriterCloser delayedCloser = new DelayedIndexWriterCloser(this);

        private ScheduledFuture<?> delayedFuture;

        private int maxIndexWriteActions;

        private ReadWriteLock IndexCloserLock = new ReentrantReadWriteLock(true);

        private ThreadLocal<Lock> writeAccess = new ThreadLocal<Lock>() {

            @Override
            protected Lock initialValue() {
                return IndexCloserLock.readLock();
            }
        };

        public IndexWriteExecutor(BlockingQueue<Runnable> workQueue, FSDirectory indexDir) {
            // single thread mode
            super(1, 1, 0, TimeUnit.SECONDS, workQueue);
            this.indexDir = indexDir;
            modifierClosed = true;
            firstJob = true;
            closeModifierEarly = MCRConfiguration.instance().getBoolean("MCR.Lucene.closeModifierEarly", false);
            maxIndexWriteActions = MCRConfiguration.instance().getInt("MCR.Lucene.maxIndexWriteActions", 500);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            //allow to close the IndexWriter
            writeAccess.get().unlock();
            if (firstJob)
                firstJob = false;
            if (closeModifierEarly || this.getCompletedTaskCount() % maxIndexWriteActions == 0)
                closeIndexWriter();
            else {
                cancelDelayedIndexCloser();
                try {
                    delayedFuture = scheduler.schedule(delayedCloser, 2, TimeUnit.SECONDS);
                } catch (RejectedExecutionException e) {
                    LOGGER.warn("Cannot schedule delayed IndexWriter closer. Closing IndexWriter now.");
                    closeIndexWriter();
                }
            }
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            //do not close IndexWriter while IndexWriterActions is processed
            writeAccess.get().lock();
            cancelDelayedIndexCloser();
            if (modifierClosed)
                openIndexWriter();
            super.beforeExecute(t, r);
        }

        private void cancelDelayedIndexCloser() {
            if (delayedFuture != null && !delayedFuture.isDone()) {
                delayedFuture.cancel(false);
            }
        }

        @Override
        public void shutdown() {
            cancelDelayedIndexCloser();
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(60 * 60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.warn("Error while closing DelayedIndexWriterCloser", e);
            }
            closeIndexWriter();
            super.shutdown();
        }

        private synchronized void openIndexWriter() {
            try {
                LOGGER.debug("Opening Lucene index for writing.");
                if (indexWriter == null)
                    indexWriter = getLuceneWriter(indexDir, firstJob);
            } catch (Exception e) {
                LOGGER.warn("Error while reopening IndexWriter.", e);
            } finally {
                modifierClosed = false;
            }
        }

        private synchronized void closeIndexWriter() {
            //TODO: check if indexWriter.commit() is sufficient here
            Lock writerLock = IndexCloserLock.writeLock();
            try {
                //do not allow IndexWriterAction being processed while closing IndexWriter
                writerLock.lock();
                if (indexWriter != null) {
                    LOGGER.debug("Writing Lucene index changes to disk.");
                    indexWriter.close();
                }
            } catch (IOException e) {
                LOGGER.warn("Error while closing IndexWriter.", e);
            } catch (IllegalStateException e) {
                LOGGER.debug("IndexWriter was allready closed.");
            } finally {
                modifierClosed = true;
                indexWriter = null;
                writerLock.unlock();
            }
        }

        private static IndexWriter getLuceneWriter(FSDirectory indexDir, boolean first) throws Exception {
            IndexWriter modifier;
            Analyzer analyzer = new GermanAnalyzer();
            boolean create = false;
            // check if indexDir is empty before creating a new index
            if (first && (indexDir.list().length == 0)) {
                LOGGER.info("No Entries in Directory, initialize: " + indexDir);
                create = true;
            }
            modifier = new IndexWriter(indexDir, analyzer, create, MaxFieldLength.LIMITED);
            modifier.setMergeFactor(200);
            modifier.setMaxBufferedDocs(2000);
            return modifier;
        }

        public IndexWriter getIndexWriter() {
            return indexWriter;
        }

        @Override
        protected void finalize() {
            closeIndexWriter();
            super.finalize();
        }

    }

    private static class IndexWriterAction implements Runnable {
        private IndexWriteExecutor executor;

        private Document doc;

        private Analyzer analyzer;

        private boolean add = false;

        private boolean delete = false;

        private boolean optimize = false;

        private Term deleteTerm;

        private RAMDirectory ramDir;

        private IndexWriterAction(IndexWriteExecutor executor) {
            this.executor = executor;
        }

        public static IndexWriterAction addAction(IndexWriteExecutor executor, Document doc, Analyzer analyzer) {
            IndexWriterAction e = new IndexWriterAction(executor);
            e.doc = doc;
            e.analyzer = analyzer;
            e.add = true;
            return e;
        }

        public static IndexWriterAction removeAction(IndexWriteExecutor executor, Term deleteTerm) {
            IndexWriterAction e = new IndexWriterAction(executor);
            e.delete = true;
            e.deleteTerm = deleteTerm;
            return e;
        }

        public static IndexWriterAction optimizeAction(IndexWriteExecutor executor) {
            IndexWriterAction e = new IndexWriterAction(executor);
            e.optimize = true;
            return e;
        }

        public static IndexWriterAction addRamDir(IndexWriteExecutor executor, RAMDirectory ramDir) {
            IndexWriterAction e = new IndexWriterAction(executor);
            e.ramDir = ramDir;
            return e;
        }

        public void run() {
            try {
                if (delete) {
                    deleteDocument();
                } else if (add) {
                    addDocument();
                } else if (optimize) {
                    optimizeIndex();
                } else
                    addDirectory();
            } catch (Exception e) {
                LOGGER.error("Error while writing Lucene Index ", e);
            }
        }

        private void addDocument() throws IOException {
            LOGGER.debug("add Document:" + toString());
            executor.getIndexWriter().addDocument(doc, analyzer);
            LOGGER.debug("adding done.");
        }

        private void deleteDocument() throws IOException {
            LOGGER.debug("delete Document:" + toString());
            executor.getIndexWriter().deleteDocuments(deleteTerm);
        }

        private void optimizeIndex() throws IOException {
            LOGGER.info("optimize Index:" + toString());
            executor.getIndexWriter().optimize();
            LOGGER.info("Optimizing done.");
        }

        private void addDirectory() throws IOException {
            LOGGER.info("add Directory");
            executor.getIndexWriter().addIndexesNoOptimize(new Directory[] { ramDir });
            LOGGER.info("Adding done.");
        }

        public String toString() {
            if (doc != null)
                return doc.toString();
            if (deleteTerm != null)
                return deleteTerm.toString();
            return "empty IndexWriterAction";
        }
    }

    private static class DelayedIndexWriterCloser implements Runnable {
        private IndexWriteExecutor executor;

        private DelayedIndexWriterCloser(IndexWriteExecutor executor) {
            this.executor = executor;
        }

        public void run() {
            if (!executor.modifierClosed && executor.getQueue().isEmpty()) {
                executor.closeIndexWriter();
            }
        }

    }

    /**
     * This class is a special Lucene version of MCRResults
     * It is read only but fast on large result set as it is filled lazy. 
     * @author Thomas Scheffler (yagee)
     */
    private static class MCRLuceneResults extends MCRResults {

        private TopFieldDocs topDocs;

        private IndexSearcher indexSearcher;

        private Collection<MCRFieldDef> addableFields;

        private static final DecimalFormat df = new DecimalFormat("0.00000000000");

        private boolean loadComplete = false;

        public MCRLuceneResults(IndexSearcher indexSearcher, TopFieldDocs topDocs, Collection<MCRFieldDef> addableFields) {
            super();
            this.indexSearcher = indexSearcher;
            this.topDocs = topDocs;
            this.addableFields = addableFields;
            topDocs.totalHits = topDocs.scoreDocs.length;
            super.hits = new ArrayList<MCRHit>(topDocs.totalHits);
            super.hits.addAll(Collections.nCopies(topDocs.totalHits, (MCRHit) null));
            setSorted(true);
        }

        @Override
        public boolean isReadonly() {
            return true;
        }

        @Override
        public void addHit(MCRHit hit) {
            throw new UnsupportedOperationException("MCRResults are read only");
        }

        @Override
        protected int merge(org.jdom.Document doc, String hostAlias) {
            throw new UnsupportedOperationException("MCRResults are read only");
        }

        @Override
        protected MCRHit getHit(String key) {
            if (!loadComplete) {
                for (int i = 0; i < getNumHits(); i++)
                    inititializeTopDoc(i);
                loadComplete = true;
            }
            return super.getHit(key);
        }

        @Override
        public MCRHit getHit(int i) {
            if (i < 0 || i > topDocs.totalHits) {
                return null;
            }
            MCRHit hit = super.getHit(i);
            if (hit == null) {
                inititializeTopDoc(i);
                hit = super.getHit(i);
            }
            return hit;
        }

        private void inititializeTopDoc(int i) {
            //initialize
            MCRHit hit;
            try {
                hit = getMCRHit(topDocs.scoreDocs[i]);
            } catch (Exception e) {
                if (topDocs.scoreDocs.length <= i) {
                    throw new MCRException("TopDocs is not initialized.", e);
                }
                throw new MCRException("Error while fetching Lucene document: " + topDocs.scoreDocs[i].doc, e);
            }
            super.hits.set(i, hit);
            MCRHit oldHit = super.map.get(hit.getKey());
            if (oldHit != null)
                oldHit.merge(hit);
            else
                super.map.put(hit.getKey(), hit);
        }

        private MCRHit getMCRHit(ScoreDoc scoreDoc) throws CorruptIndexException, IOException {
            org.apache.lucene.document.Document doc = indexSearcher.doc(scoreDoc.doc);

            String id = doc.get("returnid");
            MCRHit hit = new MCRHit(id);

            for (MCRFieldDef fd : addableFields) {
                String[] values = doc.getValues(fd.getName());
                for (String value : values) {
                    MCRFieldValue fv = new MCRFieldValue(fd, value);
                    hit.addMetaData(fv);
                }
            }

            String score = df.format(scoreDoc.score);
            addSortDataToHit(doc, hit, score, topDocs.fields);
            return hit;
        }

        private static void addSortDataToHit(org.apache.lucene.document.Document doc, MCRHit hit, String score, SortField[] sortFields) {
            for (SortField sortField : sortFields) {
                if (SortField.FIELD_SCORE == sortField || sortField.getField() == null) {
                    if (score != null)
                        hit.addSortData(new MCRFieldValue(MCRFieldDef.getDef("score"), score));
                } else {
                    String fieldName = sortField.getField();
                    if (fieldName.endsWith(SORTABLE_SUFFIX))
                        fieldName = fieldName.substring(0, fieldName.length() - SORTABLE_SUFFIX.length());

                    String values[] = doc.getValues(fieldName);
                    for (int i = 0; i < values.length; i++) {
                        MCRFieldValue fv = new MCRFieldValue(MCRFieldDef.getDef(fieldName), values[i]);
                        hit.addSortData(fv);
                    }
                }
            }
        }

        @Override
        public int getNumHits() {
            return topDocs.totalHits;
        }

        @Override
        public void cutResults(int maxResults) {
            while ((hits.size() > maxResults) && (maxResults > 0)) {
                MCRHit hit = hits.remove(hits.size() - 1);
                topDocs.totalHits--;
                if (hit != null)
                    map.remove(hit.getKey());
            }
        }

        @Override
        public Iterator<MCRHit> iterator() {
            return new Iterator<MCRHit>() {
                int i = 0;

                public boolean hasNext() {
                    return i < topDocs.totalHits;
                }

                public MCRHit next() {
                    MCRHit hit = getHit(i);
                    i++;
                    return hit;
                }

                public void remove() {
                    throw new UnsupportedOperationException("MCRResults are read only");
                }

            };
        }
    }
}