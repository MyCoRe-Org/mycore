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
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.mycore.backend.filesystem.MCRCStoreLocalFilesystem;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileReader;
import org.mycore.services.plugins.FilterPluginTransformException;
import org.mycore.services.plugins.TextFilterPluginManager;
import org.mycore.services.query.MCRTextSearchInterface;

/**
 * This class provides a content store based on lucene and the local filesystem.
 * 
 * It uses lucene for indexing know file formats and filesystem for storage.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRCStoreLucene extends MCRCStoreLocalFilesystem implements MCRTextSearchInterface {
    private static final MCRConfiguration CONF = MCRConfiguration.instance();

    private static final String DERIVATE_FIELD = "DerivateID";

    private static final String STORAGE_FIELD = "StorageID";

    private static final Logger LOGGER = Logger.getLogger(MCRCStoreLucene.class);

    private static final int OPTIMIZE_INTERVALL = 10;

    private static final TextFilterPluginManager PLUGIN_MANAGER = TextFilterPluginManager.getInstance();

    private static int DOC_COUNT;

    private static File INDEX_DIR = null;

    private static IndexReader INDEX_READER;

    private static Searcher INDEX_SEARCHER;

    private static IndexWriter INDEX_WRITER = null;

    /**
     * searches on the index and delivers derivate ids matching the search
     * 
     * Syntax:
     * 
     * <pre>
     * 
     * 
     *    foo bar   : search for foo AND bar anywhere across the files of the derivate
     *    foo -bar  : search for foo and no file of the derivate may contain bar
     *    &quot;foo bar&quot; : any file of the derivate must contain the phrase foo bar.
     * 
     * 
     * </pre>
     * 
     * @param docTextQuery
     *            query
     * @return Array of DerivateIDs
     */
    public String[] getDerivateIDs(String docTextQuery) {
        String[] returns = null;

        // transform query here
        String queryText = parseQuery(docTextQuery);
        LOGGER.debug("TS transformed query:" + queryText);

        if (queryText.length() == 0) {
            return new String[0];
        }

        // Start a filtering query for the largest word in the query
        HashSet derivateIDs = getUniqueFieldValues(DERIVATE_FIELD, queryText);

        // if it's a simple query we have the results already
        if (queryText.indexOf(" ") == -1) {
            // only one word in the query!
            // results are in derivateIDs
            return MCRUtils.getStringArray(derivateIDs.toArray());
        }

        // for all derivates matching the filtering query
        // we apply the complete query now
        Iterator it = derivateIDs.iterator();
        Hits[] hits;
        Document doc;
        String derivateID;
        HashSet collector = new HashSet();
        int i = 0;

        try {
            while (it.hasNext()) {
                hits = getHitsForDerivate((String) it.next(), queryText);

                // we have an array of hits each should contain only
                // documents belonging to a single derivateID
                boolean ok = true;

                for (int j = 0; j < hits.length; j++)
                    if ((hits[j] == null) || (hits[j].length() == 0)) {
                        ok = false;
                    }

                if (ok) {
                    doc = hits[0].doc(0);
                    derivateID = doc.get(DERIVATE_FIELD);

                    if (derivateID != null) {
                        LOGGER.debug(++i + ". " + derivateID);
                        collector.add(derivateID);
                    } else {
                        LOGGER.warn("Found Document containes no Field \"DerivateID\":" + doc);
                    }
                }

                // else{
                // logger.error("At least one hit returned was empty!");
                // }
            }

            returns = MCRUtils.getStringArray(collector.toArray());
        } catch (IOException e) {
            throw new MCRPersistenceException("IOException while query:" + docTextQuery, e);
        }

        return returns;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.ifs.MCRContentStore#init(java.lang.String)
     */
    public void init(String storeID) {
        super.init(storeID);
        PLUGIN_MANAGER.loadPlugins();
        INDEX_DIR = new File(CONF.getString(prefix + "IndexDirectory"));
        LOGGER.debug("TextIndexDir: " + INDEX_DIR);

        if (INDEX_WRITER == null) {
            LOGGER.debug("creating IndexWriter...");

            try {
                if (INDEX_DIR.exists()) {
                    // do some hardcore...
                    Directory index = FSDirectory.getDirectory(INDEX_DIR, false);

                    if (IndexReader.isLocked(INDEX_DIR.getAbsolutePath())) {
                        IndexReader.unlock(index);
                    }
                }

                loadIndexWriter();
                LOGGER.debug("IndexWriter created...");
                DOC_COUNT = INDEX_WRITER.docCount();
                INDEX_WRITER.close();
            } catch (IOException e) {
                LOGGER.error("Setting indexWriter=null");
                INDEX_WRITER = null;
            }
        }

        if (INDEX_READER == null) {
            loadIndexReader();
        }

        if (INDEX_SEARCHER == null) {
            loadIndexSearcher();
        }

        try {
            IndexReader.getCurrentVersion(INDEX_DIR);
        } catch (IOException e) {
            LOGGER.error("Error while getting last modified info from IndexDir", e);
        }
    }

    protected static String parseQuery(String query) {
        LOGGER.debug("TS incoming query: " + query);

        int i = query.indexOf('\"');
        i++;

        if (i == 0) {
            return "";
        }

        int j = query.lastIndexOf('\"');

        if (j == -1) {
            return "";
        }

        return query.substring(i, j).trim();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.ifs.MCRContentStore#doDeleteContent(java.lang.String)
     */
    protected void doDeleteContent(String storageID) throws Exception {
        // remove from index
        Term term = new Term(STORAGE_FIELD, storageID);
        int deleted = INDEX_READER.delete(term);
        INDEX_SEARCHER.close();
        INDEX_READER.close();
        INDEX_READER = null;
        DOC_COUNT--;
        LOGGER.debug("deleted " + deleted + " documents containing " + term);

        if ((DOC_COUNT % OPTIMIZE_INTERVALL) == 0) {
            loadIndexWriter();
            LOGGER.debug("Optimize index for searching...");
            INDEX_WRITER.optimize();
            INDEX_WRITER.close();
        }

        loadIndexReader();
        loadIndexSearcher();

        // remove file
        super.doDeleteContent(storageID);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.ifs.MCRContentStore#doStoreContent(org.mycore.datamodel.ifs.MCRFileReader,
     *      org.mycore.datamodel.ifs.MCRContentInputStream)
     */
    protected String doStoreContent(MCRFileReader file, MCRContentInputStream source) throws Exception {
        if (!PLUGIN_MANAGER.isSupported(file.getContentType())) {
            throw new MCRPersistenceException(new StringBuffer(file.getContentTypeID()).append(" is not supported by any TextFilterPlugin detected").append(" by the TextFilterPluginManager.\n").append("Make sure you have a Plugin installed in the proper directory:").append(CONF.getString("MCR.PluginDirectory", "(not configured yet)")).append(
                    "\nIf you don't have the right Plugin ready, reasign \"").append(file.getContentTypeID()).append("\" to another ContentStore.\n").append("Read the manual on how to do this!").toString());
        }

        String returns = super.doStoreContent(file, source);
        if ((returns == null) || (returns.length() == 0)) {
            throw new MCRPersistenceException("Failed to store file " + file.getID() + " to local file system!");
        }

        Document doc = getDocument(file);

        Field storageID = new Field(STORAGE_FIELD, returns, true, true, false);
        doc.add(storageID);

        try {
            indexDocument(doc);
            doc = null;
        } catch (IOException io) {
            // Document was not added
            // remove file from local FileStore
            super.deleteContent(returns);

            // send Exception
            throw new MCRPersistenceException("Failed to store file " + file.getID() + " to local file system!\n" + "Cannot index file content!", io);
        }

        return returns;
    }

    protected void finalize() throws Throwable {
        LOGGER.debug("finalize() called on Lucenestore: shutting down...");

        synchronized (INDEX_READER) {
            INDEX_READER.close();
            INDEX_READER = null;
        }

        synchronized (INDEX_WRITER) {
            INDEX_WRITER.optimize();
            INDEX_WRITER.close();
            INDEX_WRITER = null;
        }

        LOGGER.debug("shutting down... completed");
    }

    protected Document getDocument(MCRFileReader reader) throws IOException {
        Document returns = new Document();

        // reader is instance of MCRFile
        // ownerID is derivate ID for all mycore files
            MCRFile file = (MCRFile) reader;
            Field derivateID = new Field(DERIVATE_FIELD, file.getOwnerID(), true, true, false);
            Field fileID = new Field("FileID", file.getID(), true, true, false);
            LOGGER.debug("adding fields to document");
            returns.add(derivateID);
            returns.add(fileID);

        try {
            InputStream stream = file.getContentAsInputStream();
            BufferedReader in = new BufferedReader(PLUGIN_MANAGER.transform(reader.getContentType(), stream));

            /*
             * since file is stored elsewhere we only index the file and do not
             * store
             */
            Field content = Field.Text("content", in);
            returns.add(content);
        } catch (FilterPluginTransformException fe) {
            // no transformation was done because of an error
            LOGGER.error("Error while transforming document.", fe);

            return returns;
        } catch (NullPointerException ne) {
            // maybe ContentType is unsupported?
            LOGGER.error("Error while transforming document.", ne);

            return returns;
        }

        LOGGER.debug("returning document");

        return returns;
    }

    private final boolean containsExclusiveClause(BooleanQuery query) {
        BooleanClause[] clauses = query.getClauses();

        if (clauses.length == 1) {
            clauses = ((BooleanQuery) clauses[0].query).getClauses();

            if (clauses.length == 2) {
                return clauses[1].prohibited;
            }
        }

        return false;
    }

    private static Analyzer getAnalyzer() {
        // TODO: have to replace GermanAnalyzer by more generic
        return new GermanAnalyzer();
    }

    private Hits[] getHitsForDerivate(String derivateID, String queryText) {
        Hits[] hits = null;
        Analyzer analyzer = getAnalyzer();

        LOGGER.debug("Query: " + derivateID + "-->" + queryText);

        LuceneCStoreQueryParser parser = new LuceneCStoreQueryParser("content", analyzer);
        parser.setGroupingValue(derivateID);

        // combine to a query over a specific DerivateID
        // StringBuffer queryStr =
        // new StringBuffer("DerivateID:\"").append(derivateID).append(
        // "\" AND ").append(
        // queryText);
        try {
            BooleanQuery[] queries = parser.getBooleanQueries(queryText);
            hits = new Hits[queries.length];

            for (int i = 0; i < queries.length; i++) {
                LOGGER.debug("  -Searching for: " + queries[i].toString("content"));
                hits[i] = INDEX_SEARCHER.search(queries[i]);

                if (containsExclusiveClause(queries[i])) {
                    // check that all documents meets negative clause
                    Hits test = INDEX_SEARCHER.search(QueryParser.parse(derivateID, DERIVATE_FIELD, new WhitespaceAnalyzer()));

                    if (test.length() != hits[i].length()) {
                        hits[i] = null;
                    }
                }

                // logger.debug(hits[i].length() + " total matching documents");
            }
        } catch (ParseException e) {
            StringBuffer msg = new StringBuffer("Error while querying (").append(queryText).append(") over Files matching DerivateID=").append(derivateID).append("!");
            throw new MCRPersistenceException(msg.toString(), e);
        } catch (IOException e) {
            StringBuffer msg = new StringBuffer("Error while querying (").append(queryText).append(") over Files matching DerivateID=").append(derivateID).append("!");
            throw new MCRPersistenceException(msg.toString(), e);
        }

        return hits;
    }

    /**
     * returns all Field values matching the biggest subquery of query
     * 
     * @param fieldName
     * @param query
     * @return a set of search results that matches the biggest subquery of query
     */
    private HashSet getUniqueFieldValues(String fieldName, String query) {
        HashSet collector = new HashSet();

        if ((fieldName == null) || (query == null) || (fieldName.length() == 0) || (query.length() == 0)) {
            return collector;
        }

        int size = 0;
        String biggestSub = null;
        String temp;
        StringTokenizer tok = new StringTokenizer(query, " ");

        while (tok.hasMoreTokens()) {
            temp = tok.nextToken();

            if (biggestSub == null) {
                // remove leading quote
                if (temp.charAt(0) == '\"') {
                    temp = temp.substring(1);
                }

                // remove leading quote of a negative clause
                if ((temp.charAt(0) == '-') && (temp.charAt(1) == '\"')) {
                    temp = '-' + temp.substring(2);
                }

                biggestSub = temp;
                size = temp.length();
            } else {
                if ((temp.length() > size) && !(temp.charAt(0) == '-')) {
                    // Subquery is not negative and bigger then current
                    if (temp.charAt(temp.length() - 1) == '\"') {
                        temp = temp.substring(0, temp.length() - 1);
                    }

                    // removed double quotes
                    biggestSub = temp;
                    size = temp.length();
                }
            }
        }

        LOGGER.debug("Start a presearch for subquery:" + biggestSub);

        try {
            Hits hits = INDEX_SEARCHER.search(QueryParser.parse(biggestSub, "content", getAnalyzer()));
            String[] values;

            for (int i = 0; i < hits.length(); i++) {
                values = hits.doc(i).getValues(fieldName);

                if (values == null) {
                    LOGGER.warn("Found a document but " + fieldName + " was not stored in!");
                } else {
                    for (int j = 0; j < values.length; j++) {
                        // Store field value in collector
                        collector.add(values[j]);
                    }
                }
            }
        } catch (ParseException e) {
            StringBuffer msg = new StringBuffer("Error while fetching unique values of field ").append(fieldName).append("!");
            throw new MCRPersistenceException(msg.toString(), e);
        } catch (IOException e) {
            StringBuffer msg = new StringBuffer("Error while fetching unique values of field ").append(fieldName).append("!");
            throw new MCRPersistenceException(msg.toString(), e);
        }

        return collector;
    }

    private void indexDocument(Document doc) throws IOException {
        INDEX_SEARCHER.close();
        loadIndexWriter();
        LOGGER.debug("Create index for storageID=" + doc.getField(STORAGE_FIELD).stringValue());
        INDEX_WRITER.addDocument(doc);
        DOC_COUNT++;

        if ((DOC_COUNT % OPTIMIZE_INTERVALL) == 0) {
            LOGGER.debug("Optimize index for searching...");
            INDEX_WRITER.optimize();
        }

        INDEX_WRITER.close();
        loadIndexReader();
        loadIndexSearcher();
    }

    private synchronized void loadIndexReader() {
        try {
            INDEX_READER = IndexReader.open(INDEX_DIR);
        } catch (IOException e) {
            throw new MCRPersistenceException("Cannot read index in " + INDEX_DIR.getAbsolutePath() + File.pathSeparatorChar + INDEX_DIR.getName(), e);
        }
    }

    private synchronized void loadIndexSearcher() {
        if (INDEX_READER == null) {
            loadIndexReader();
        }

        try {
            IndexReader.getCurrentVersion(INDEX_DIR);
        } catch (IOException e) {
            LOGGER.warn("Cannot get current Version of IndexDir", e);
        }

        INDEX_SEARCHER = new IndexSearcher(INDEX_READER);
    }

    private synchronized void loadIndexWriter() {
        boolean create = true;

        if (IndexReader.indexExists(INDEX_DIR)) {
            // reuse Index
            create = false;
        }

        LOGGER.debug("No previous index exists:(" + create + ")");

        try {
            INDEX_WRITER = new IndexWriter(INDEX_DIR, getAnalyzer(), create);
        } catch (IOException e) {
            throw new MCRPersistenceException("Cannot create index in " + INDEX_DIR.getAbsolutePath(), e);
        }

        INDEX_WRITER.mergeFactor = OPTIMIZE_INTERVALL;
        INDEX_WRITER.minMergeDocs = 1; // always write to local dir
    }
}
