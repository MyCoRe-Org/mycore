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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
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

    private static TextFilterPluginManager PLUGIN_MANAGER = null;

    static Analyzer analyzer = new PerFieldAnalyzerWrapper(new GermanAnalyzer(Version.LUCENE_30));

    File IndexDir;

    private MCRIndexWriteExecutor modifyExecutor;

    private boolean useRamDir = false;

    private RAMDirectory ramDir = null;

    private IndexWriter writerRamDir;

    private int ramDirEntries = 0;

    private MCRSharedLuceneIndexContext sharedIndexContext;

    private FSDirectory indexDir;

    private Vector<MCRFieldDef> addableFields = new Vector<MCRFieldDef>();

    private boolean storeQueryFields;

    private MCRLuceneQueryFieldLogger queryFieldLogger;

    private boolean initializeResult;

    /**
     * @return the sortableSuffix
     */
    static String getSortableSuffix() {
        return SORTABLE_SUFFIX;
    }

    @Override
    public void init(String ID) {
        super.init(ID);

        MCRConfiguration config = MCRConfiguration.instance();
        initializeResult = config.getBoolean(prefix + "initializeResult", false);
        IndexDir = new File(config.getString(prefix + "IndexDir"));
        LOGGER.info(prefix + "indexDir: " + IndexDir);
        if (!IndexDir.exists()) {
            IndexDir.mkdirs();
        }
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
            modifyExecutor = new MCRIndexWriteExecutor(new LinkedBlockingQueue<Runnable>(), indexDir);
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
            if (fd.isAddable()) {
                addableFields.add(fd);
            }
        }
        MCRShutdownHandler.getInstance().addCloseable(this);
        try {
            sharedIndexContext = new MCRSharedLuceneIndexContext(FSDirectory.open(IndexDir), ID);
        } catch (IOException e) {
            throw new MCRException("Cannot initialize IndexReader.", e);
        }
        storeQueryFields = MCRConfiguration.instance().getBoolean(prefix + "StoreQueryFields", false);
        if (storeQueryFields) {
            File queryFieldProperties = getQueryFieldLoggerProperties();
            Properties props = new Properties();
            if (queryFieldProperties.exists()) {
                FileInputStream fileInputStream = null;
                try {
                    try {
                        fileInputStream = new FileInputStream(queryFieldProperties);
                        props.load(fileInputStream);
                    } finally {
                        fileInputStream.close();
                    }
                } catch (IOException e) {
                    throw new MCRException(e);
                }
            }
            queryFieldLogger = new MCRLuceneQueryFieldLogger(props);
        }
    }

    private File getQueryFieldLoggerProperties() {
        return new File(IndexDir.getParentFile(), IndexDir.getName() + "-usage.properties");
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

    @Override
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
     * 
     */
    public void deleteLuceneDocument(String fieldname, String id) throws Exception {
        Term deleteTerm = new Term(fieldname, id);
        MCRIndexWriterAction modifyAction = MCRIndexWriterAction.removeAction(modifyExecutor, deleteTerm);
        modifyIndex(modifyAction);
    }

    /**
     * As opposed to {@link MCRSearcher} the returned MCRResult is read only.
     * @see MCRSearcher#search(MCRCondition, int, List, boolean)
     */
    @Override
    public MCRResults search(MCRCondition condition, int maxResults, List<MCRSortBy> sortBy, boolean addSortData) {
        try {
            List<Element> f = new ArrayList<Element>();
            f.add(condition.toXML());
            Set<String> usedFields = (queryFieldLogger == null) ? null : new HashSet<String>();
            boolean reqf = true;
            // required flag Term with AND (true) or OR (false) combined
            Query luceneQuery = MCRBuildLuceneQuery.buildLuceneQuery(null, reqf, f, analyzer, usedFields);
            LOGGER.debug("Lucene Query: " + luceneQuery.toString());
            MCRResults luceneHits = getLuceneHits(luceneQuery, maxResults, sortBy, addSortData, usedFields);
            if (initializeResult) {
                return MCRResults.union(luceneHits);
            }
            return luceneHits;
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
    private MCRResults getLuceneHits(Query luceneQuery, int maxResults, List<MCRSortBy> sortBy, boolean addSortData, Set<String> usedFields) throws Exception {
        if (maxResults <= 0) {
            maxResults = 1000000;
        }

        long start = System.currentTimeMillis();
        final Sort sortFields = buildSortFields(sortBy);
        if (sortFields.getSort().length == 0) {
            //one sort criteria is needed for TopFieldCollector, using internal document id then
            sortFields.setSort(SortField.FIELD_DOC);
        }
        if (queryFieldLogger != null) {
            //log field usage
            for (String field : usedFields) {
                queryFieldLogger.useField(field);
            }
            for (SortField sortField : sortFields.getSort()) {
                String field = sortField.getField();
                if (field != null) {
                    queryFieldLogger.useField(field);
                }
            }
        }
        MCRLuceneResults results = new MCRLuceneResults(sharedIndexContext, addableFields, sortFields, luceneQuery, maxResults);
        //cannot sort with "score" as the SortField is created on the fly for that search
        for (SortField field : sortFields.getSort()) {
            if (field == SortField.FIELD_SCORE)
                results.setSorted(false);
        }
        LOGGER.info("Number of Objects found: " + results.getNumHits() + " Time for Search: " + (System.currentTimeMillis() - start));
        return results;
    }

    private Sort buildSortFields(List<MCRSortBy> sortBy) {
        ArrayList<SortField> sortList = new ArrayList<SortField>(sortBy.size());
        for (MCRSortBy sortByElement : sortBy) {
            SortField sortField;
            if (sortByElement.getField().getName().equals("score")) {
                sortField = SortField.FIELD_SCORE;
            } else {
                String name = sortByElement.getField().getName();
                //TODO: use dataType to get FieldType (how to handle dates here?)
                int fieldType = getFieldType(sortByElement.getField());
                if (isTokenized(sortByElement.getField())) {
                    name += getSortableSuffix();
                }
                sortField = new SortField(name, fieldType, sortByElement.getSortOrder() == MCRSortBy.DESCENDING);
            }
            sortList.add(sortField);
        }
        if (LOGGER.isDebugEnabled()) {
            for (SortField sortField : sortList) {
                String name = SortField.FIELD_SCORE == sortField ? "score" : sortField.getField();
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
                        field += getSortableSuffix();
                    }
                    String values[] = doc.getValues(field);
                    for (String value : values) {
                        MCRFieldValue fv = new MCRFieldValue(fds, value);
                        hit.addSortData(fv);
                    }
                }
            }
        }
    }

    @Override
    public void addToIndex(String entryID, String returnID, List<MCRFieldValue> fields) {
        LOGGER.info("MCRLuceneSearcher indexing data of " + entryID);

        if (fields == null || fields.size() == 0) {
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
                MCRIndexWriterAction modifyAction = MCRIndexWriterAction.addRamDir(modifyExecutor, ramDir);
                modifyIndex(modifyAction);
                ramDir = new RAMDirectory();
                writerRamDir = new IndexWriter(ramDir, analyzer, true, MaxFieldLength.LIMITED);
                ramDirEntries = 0;
            }
        } else {
            MCRIndexWriterAction modifyAction = MCRIndexWriterAction.addAction(modifyExecutor, doc, analyzer);
            modifyIndex(modifyAction);
        }
    }

    private void modifyIndex(MCRIndexWriterAction modifyAction) {
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
            MCRFieldValue field = fields.get(i);
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

                    doc.add(new Field(name, s, Field.Store.NO, Field.Index.ANALYZED));
                }
            } else {
                Field.Store store = Field.Store.NO;
                if (field.getField().isSortable()) {
                    store = Field.Store.YES;
                }
                if ("date".equals(type) || "time".equals(type) || "timestamp".equals(type)) {
                    doc.add(new NumericField(name, store, true).setLongValue(MCRLuceneTools.getLongValue(content)));
                } else if ("boolean".equals(type)) {
                    content = "true".equals(content) ? "1" : "0";
                    type = "identifier";
                } else if ("decimal".equals(type)) {
                    doc.add(new NumericField(name, store, true).setFloatValue(Float.parseFloat(content)));
                } else if ("integer".equals(type)) {
                    doc.add(new NumericField(name, store, true).setLongValue(Long.parseLong(content)));
                }

                if (type.equals("identifier")) {
                    doc.add(new Field(name, content, Field.Store.YES, Field.Index.NOT_ANALYZED));
                }
                if (type.equals("index")) {
                    doc.add(new Field(name, MCRNormalizer.normalizeString(content, true), Field.Store.YES, Field.Index.NOT_ANALYZED));
                }
                if (type.equals("Text") || type.equals("name") || type.equals("text") && field.getField().isSortable()) {
                    doc.add(new Field(name, content, Field.Store.YES, Field.Index.ANALYZED));
                    if (field.getField().isSortable()) {
                        doc.add(new Field(name + getSortableSuffix(), content, Field.Store.YES, Field.Index.NOT_ANALYZED));
                    }
                } else if (type.equals("text")) {
                    doc.add(new Field(name, content, Field.Store.NO, Field.Index.ANALYZED));
                }
            }
        }

        return doc;
    }

    private boolean isTokenized(MCRFieldDef fieldDef) {
        String type = fieldDef.getDataType();
        if (type.equals("Text") || type.equals("name") || type.equals("text")) {
            return true;
        }
        return false;
    }

    @Override
    public void addSortData(Iterator<MCRHit> hits, List<MCRSortBy> sortBy) {
        sharedIndexContext.getIndexReadLock().lock();
        try {
            IndexSearcher indexSearcher = sharedIndexContext.getSearcher();
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
        } finally {
            sharedIndexContext.getIndexReadLock().unlock();
        }
    }

    @Override
    public void clearIndex() {
        try {
            IndexWriter writer = new IndexWriter(indexDir, analyzer, true, MaxFieldLength.LIMITED);
            writer.close();
        } catch (IOException e) {
            LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
            LOGGER.error(MCRException.getStackTraceAsString(e));
        }
    }

    @Override
    public void clearIndex(String fieldname, String value) {
        try {
            deleteLuceneDocument(fieldname, value);
        } catch (Exception e) {
            LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
            LOGGER.error(MCRException.getStackTraceAsString(e));
        }
    }

    @Override
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
            MCRIndexWriterAction modifyAction = MCRIndexWriterAction.optimizeAction(modifyExecutor);
            modifyIndex(modifyAction);
        } else if (!"finish".equals(mode)) {
            LOGGER.error("invalid mode " + mode);
        }
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
                MCRIndexWriterAction modifyAction = MCRIndexWriterAction.addRamDir(modifyExecutor, ramDir);
                modifyIndex(modifyAction);
            }
        }
    }

    public void prepareClose() {
        //nothing to be done to prepare close()
    }

    public void close() {
        sharedIndexContext.close();
        handleRamDir();
        LOGGER.info("Closing " + toString() + "...");
        modifyExecutor.shutdown();
        try {
            modifyExecutor.awaitTermination(60 * 60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.warn("Error while closing " + toString(), e);
        }
        LOGGER.info("Processed " + modifyExecutor.getCompletedTaskCount() + " modification requests.");
        if (queryFieldLogger != null) {
            Properties properties = queryFieldLogger.getFieldUsageAsProperties();
            FileOutputStream outputStream = null;
            try {
                try {
                    File queryFieldLoggerProperties = getQueryFieldLoggerProperties();
                    outputStream = new FileOutputStream(queryFieldLoggerProperties);
                    properties.store(outputStream, new Date().toString());
                    LOGGER.info("Stored " + properties.size() + " fields in usage statistics to " + queryFieldLoggerProperties);
                } finally {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                }
            } catch (IOException e) {
                throw new MCRException(e);
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + ID;
    }

    @Override
    public boolean isIndexer() {
        return true;
    }

    @Override
    public int getPriority() {
        return MCRShutdownHandler.Closeable.DEFAULT_PRIORITY;
    }
}
