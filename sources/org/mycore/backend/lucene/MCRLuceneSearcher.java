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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearchField;
import org.mycore.services.fieldquery.MCRSearcherBase;
import org.mycore.services.plugins.TextFilterPluginManager;

/**
 * This class builds indexes from mycore meta data.
 * 
 * @author Harald Richter
 */
public class MCRLuceneSearcher extends MCRSearcherBase {
    /** The logger */
    private final static Logger LOGGER = Logger.getLogger(MCRLuceneSearcher.class);

    static final private MCRConfiguration CONFIG = MCRConfiguration.instance();

    String IndexDir = "";

    boolean FIRST = true;

    // TODO: read from property file
    static String DATE_FORMAT = "yyyy-MM-dd";

    static String TIME_FORMAT = "hh:mm:ss";

    static String TIMESTAMP_FORMAT = "yyyy-MM-dd hh:mm:ss";

    private static TextFilterPluginManager PLUGIN_MANAGER = null;

    public void init(String ID) {
        super.init(ID);
        IndexDir = CONFIG.getString(prefix + "IndexDir");
        LOGGER.info(prefix + "indexDir: " + IndexDir);

        String lockDir = CONFIG.getString("MCR.Lucene.LockDir", "");
        LOGGER.info("MCR.Lucene.LockDir: " + lockDir);

        File file = new File(lockDir);

        if (!file.exists()) {
            LOGGER.info("Lock Directory for Lucene doesn't exist: \"" + lockDir + "\" use " + System.getProperty("java.io.tmpdir"));
        } else if (file.isDirectory()) {
            System.setProperty("org.apache.lucene.lockdir", lockDir);
        }
    }

    protected void addToIndex(String entryID, List fields) {
        LOGGER.info("MCRLuceneSearcher indexing data of " + entryID);

        if ((fields == null) || (fields.size() == 0)) {
            return;
        }

        try {
            Document doc = buildLuceneDocument(fields);
            doc.add(Field.Keyword("mcrid", entryID));
            LOGGER.debug("lucene document build " + entryID);
            addDocumentToLucene(doc);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.warn("xxxxx " + e.getMessage());
        }
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
    private Document buildLuceneDocument(List fields) throws Exception {
        Document doc = new Document();

        for (int i = 0; i < fields.size(); i++) {
            MCRSearchField field = (MCRSearchField) (fields.get(i));
            String name = field.getName();
            String type = field.getDataType();
            String content = field.getValue();
            MCRFile mcrfile = field.getFile();

            LOGGER.debug("####### Name: " + name + " Type: " + type + " Content: " + content);

            if (null != mcrfile) {
                if (PLUGIN_MANAGER == null) {
                    PLUGIN_MANAGER = TextFilterPluginManager.getInstance();
                }
                if (PLUGIN_MANAGER.isSupported(mcrfile.getContentType())) {
                    LOGGER.debug("####### Index MCRFile: " + mcrfile.getPath());

                    BufferedReader in = new BufferedReader(PLUGIN_MANAGER.transform(mcrfile.getContentType(), mcrfile.getContentAsInputStream()));
                    Field f = Field.Text(name, in);
                    doc.add(f);
                }
            } else if ((null != name) && (null != type) && (null != content) && (content.length() > 0)) {
                if ("date".equals(type)) {
                    content = handleDate(content, DATE_FORMAT, "yyyyMMdd");
                    type = "Text";
                } else if ("time".equals(type)) {
                    content = handleDate(content, TIME_FORMAT, "HHmmss");
                    type = "Text";
                } else if ("timestamp".equals(type)) {
                    content = handleDate(content, TIMESTAMP_FORMAT, "yyyyMMddHHmmss");
                    type = "Text";
                } else if ("boolean".equals(type)) {
                    content = "true".equals(content) ? "1" : "0";
                    type = "indentifier";
                }

                // TODO handle decimal and integer
                if (type.equals("identifier")) {
                    doc.add(Field.Keyword(name, content));
                }

                if (type.equals("Text") || type.equals("name")) {
                    doc.add(Field.Text(name, content));
                }

                if (type.equals("text")) {
                    doc.add(Field.UnStored(name, content));
                }
            }
        }

        return doc;
    }

    private String handleDate(String content, String informat, String outformat) throws Exception {
        DateFormat f1 = new SimpleDateFormat(informat);
        DateFormat f2 = new SimpleDateFormat(outformat);
        Date d;
        d = f1.parse(content);

        return f2.format(d);
    }

    /**
     * Adds document to Lucene
     * 
     * @param doc
     *            lucene document to add to index
     * 
     */
    private void addDocumentToLucene(Document doc) throws Exception {
        IndexWriter writer = null;
        Analyzer analyzer = new GermanAnalyzer();

        // does directory for text index exist, if not build it
        if (FIRST) {
            FIRST = false;

            File file = new File(IndexDir);

            if (!file.exists()) {
                LOGGER.info("The Directory doesn't exist: " + IndexDir + " try to build it");

                IndexWriter writer2 = new IndexWriter(IndexDir, analyzer, true);
                writer2.close();
            } else if (file.isDirectory()) {
                if (0 == file.list().length) {
                    LOGGER.info("No Entries in Directory, initialize: " + IndexDir);

                    IndexWriter writer2 = new IndexWriter(IndexDir, analyzer, true);
                    writer2.close();
                }
            }
        } // if ( first

        if (null == writer) {
            writer = new IndexWriter(IndexDir, analyzer, false);
            writer.mergeFactor = 200;
            writer.maxMergeDocs = 2000;
        }

        writer.addDocument(doc);
        writer.close();
        writer = null;
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
     * Delete document in Lucene
     * 
     * @param fieldname
     *            string name of lucene field to store id
     * @param id
     *            string document id
     * 
     */
    private void deleteLuceneDocument(String fieldname, String id) throws Exception {
        IndexSearcher searcher = new IndexSearcher(IndexDir);

        if (null == searcher) {
            return;
        }

        Term te1 = new Term(fieldname, id);

        TermQuery qu = new TermQuery(te1);

        LOGGER.info("Searching for: " + qu.toString(""));

        Hits hits = searcher.search(qu);

        LOGGER.info("Number of documents found : " + hits.length());

        if (1 == hits.length()) {
            LOGGER.info(fieldname + ": " + hits.id(0) + " score: " + hits.score(0) + " key: " + hits.doc(0).get(fieldname));

            if (id.equals(hits.doc(0).get(fieldname))) {
                IndexReader reader = IndexReader.open(IndexDir);
                reader.delete(hits.id(0));
                reader.close();
                LOGGER.info("DELETE: " + id);
            }
        }
    }

    public MCRResults search(MCRCondition cond, List order, int maxResults) {
        MCRResults results = new MCRResults();

        try {
            MCRLuceneQuery lucenequery = new MCRLuceneQuery(cond, maxResults, IndexDir);
            results = lucenequery.getLuceneHits();
            results.setComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }

        LOGGER.debug("MCRMemorySearcher results completed");

        return results;
    }

}
