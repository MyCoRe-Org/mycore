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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Enumeration;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
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
 * Based on MCRLucenesearcher Version 1.43 for miless-software 
 * 
 * @author Harald Richter
 * @deprecated use MCRLuceneSearcher
 */
public class MCROldLuceneSearcher extends MCRSearcher {
    /** The logger */
    private final static Logger LOGGER = Logger.getLogger(MCROldLuceneSearcher.class);

    String IndexDir = "";

    boolean FIRST = true;

    static String LOCK_DIR = "";

    static int INT_BEFORE = 10;

    static int DEC_BEFORE = 10;

    static int DEC_AFTER = 4;

    private static TextFilterPluginManager PLUGIN_MANAGER = null;

    static Analyzer analyzer = new PerFieldAnalyzerWrapper(new GermanAnalyzer());

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
            throw new org.mycore.common.MCRConfigurationException(msg);
        }
        if (!f.canWrite()) {
            String msg = IndexDir + " is not writeable!";
            throw new org.mycore.common.MCRConfigurationException(msg);
        }

        try {
            IndexWriter writer = getLuceneWriter(IndexDir, FIRST);
            FIRST = false;
            writer.close();
        } catch (IOException e) {
            LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
            LOGGER.error(MCRException.getStackTraceAsString(e));
        } catch (Exception e) {
            LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
            LOGGER.error(MCRException.getStackTraceAsString(e));
        }

        StandardAnalyzer standardAnalyzer = new StandardAnalyzer(); // should
                                                                    // work like
                                                                    // GermanAnalyzer
                                                                    // without
                                                                    // stemming
        List fds = MCRFieldDef.getFieldDefs(getIndex());
        for (int i = 0; i < fds.size(); i++) {
            MCRFieldDef fd = (MCRFieldDef) (fds.get(i));
            if ("name".equals(fd.getDataType())) {
                ((PerFieldAnalyzerWrapper) analyzer).addAnalyzer(fd.getName(), standardAnalyzer);
            }
        }

    }

    public void addToIndex(String entryID, String returnID, List fields) {
        LOGGER.info("MCRoldLuceneSearcher indexing data of " + entryID);

        if ((fields == null) || (fields.size() == 0)) {
            return;
        }

        try {
            Document doc = buildLuceneDocument(fields);
            doc.add(new Field("mcrid", entryID, Field.Store.YES, Field.Index.UN_TOKENIZED));
            doc.add(new Field("returnid", returnID, Field.Store.YES, Field.Index.UN_TOKENIZED));
            LOGGER.debug("######+#+# lucene document build with entryID: " + entryID + " and returnID: " + returnID);
            addDocumentToLucene(doc, analyzer, IndexDir, FIRST);
            FIRST = false;
        } catch (Exception e) {
            LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
            LOGGER.error(MCRException.getStackTraceAsString(e));
        }
    }

    /**
     * @param sortBy
     * @param doc
     *          lucene document to get sortdata from 
     * @param hit
     *          sortdata are added 
     * @param score
     *          of hit 
     */
    private void addSortDataToHit(List<MCRSortBy> sortBy, org.apache.lucene.document.Document doc, MCRHit hit, String score)
    {
      for (int j = 0; j < sortBy.size(); j++) {
          MCRSortBy sb = sortBy.get(j);
          MCRFieldDef fds = sb.getField();
          if (null != fds) {
              String field = fds.getName();
              String values[] = doc.getValues(field);
              if (null != values) {
                  for (int i=0; i < values.length; i++)
                  {
                    MCRFieldDef fd = MCRFieldDef.getDef(field);
                    MCRFieldValue fv = new MCRFieldValue(fd, values[i]);
                    hit.addSortData(fv);
                  }
              }
              else if ("score".equals(field) && null != score)
              {
                MCRFieldDef fd = MCRFieldDef.getDef(field);
                MCRFieldValue fv = new MCRFieldValue(fd, score);
                hit.addSortData(fv);
              }
          }
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
    public static Document buildLuceneDocument(List fields) throws Exception {
        return MCRLuceneSearcher.buildLuceneDocument(fields);
    }

    /**
     * Adds document to Lucene
     * 
     * @param doc
     *            lucene document to add to index
     * 
     */
    private static synchronized void addDocumentToLucene(Document doc, Analyzer analyzer, String indexDir, boolean first) throws Exception {
        IndexWriter writer = getLuceneWriter(indexDir, first);
        try
        {
          writer.addDocument(doc, analyzer);
        } finally 
        {
          writer.close();
        }
    }

    /**
     * Get Lucene Writer
     * 
     * @param indexDir
     *            directory where lucene index is store first check existance of
     *            index directory, if it does nor exist create it
     * 
     * @return the lucene writer, calling programm must close writer
     */
    public static IndexWriter getLuceneWriter(String indexDir, boolean first) throws Exception {
        IndexWriter writer;
        Analyzer analyzer = new GermanAnalyzer();

        // does directory for text index exist, if not build it
        if (first) {
            File file = new File(indexDir);

            if (!file.exists()) {
                LOGGER.info("The Directory doesn't exist: " + indexDir + " try to build it");

                IndexWriter writer2 = new IndexWriter(indexDir, analyzer, true);
                writer2.close();
            } else if (file.isDirectory()) {
                if (0 == file.list().length) {
                    LOGGER.info("No Entries in Directory, initialize: " + indexDir);

                    IndexWriter writer2 = new IndexWriter(indexDir, analyzer, true);
                    writer2.close();
                }
            }
        } // if ( first

        writer = new IndexWriter(indexDir, analyzer, false);
        // writer.mergeFactor = 200;
        writer.setMergeFactor(200);
        // writer.maxMergeDocs = 2000;
        writer.setMaxMergeDocs(2000);

        return writer;
    }

    public void removeFromIndex(String entryID) {
        LOGGER.info("MCRoldLuceneSearcher removing indexed data of " + entryID);

        try {
            deleteLuceneDocument("mcrid", entryID, IndexDir);
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
    public static synchronized void deleteLuceneDocument(String fieldname, String id, String indexDir) throws Exception {
        IndexSearcher searcher = new IndexSearcher(indexDir);

        if (null == searcher) {
            return;
        }

        Term te1 = new Term(fieldname, id);

        TermQuery qu = new TermQuery(te1);

        LOGGER.info("Searching for: " + qu.toString(""));

        Hits hits = searcher.search(qu);

        LOGGER.info("Number of documents found : " + hits.length());

        if (hits.length() > 0) {
            IndexReader reader = IndexReader.open(indexDir);
            for (int i = 0; i < hits.length(); i++) {
                // reader.delete(hits.id(i));
                reader.deleteDocument(hits.id(i));
            }
            LOGGER.info("DELETE: " + id);
            reader.close();
        }

        searcher.close();
    }

    public MCRResults search(MCRCondition condition, int maxResults, List<MCRSortBy> sortBy, boolean addSortData) {
        MCRResults results = new MCRResults();

        try {
            List<Element> f = new ArrayList<Element>();
            f.add(condition.toXML());

            boolean reqf = true;
            // required flag Term with AND (true) or OR (false) combined
            Query luceneQuery = MCRBuildLuceneQuery.buildLuceneQuery(null, reqf, f, analyzer);
            LOGGER.debug("Lucene Query: " + luceneQuery.toString());
            results = getLuceneHits(luceneQuery, maxResults, sortBy, addSortData);
        } catch (Exception e) {
            LOGGER.error("Exception in MCRoldLuceneSearcher", e);
        }

        return results;
    }

    /**
     * method does lucene query
     * 
     * @return result set
     */
    private MCRResults getLuceneHits(Query luceneQuery, int maxResults, List<MCRSortBy> sortBy, boolean addSortData) throws Exception {
        if (maxResults <= 0)
            maxResults = 1000000;

        IndexSearcher searcher = null;
        try {
            searcher = new IndexSearcher(IndexDir);
        } catch (IOException e) {
            LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
            LOGGER.error(MCRException.getStackTraceAsString(e));
        }

        Hits hits = searcher.search( luceneQuery );
        int found = hits.length();
//        TopDocs hits = searcher.search(luceneQuery, null, maxResults);
//        int found = hits.scoreDocs.length;

        LOGGER.info("Number of Objects found : " + found);

        MCRResults result = new MCRResults();

        for (int i = 0; i < found; i++) {
            org.apache.lucene.document.Document doc = hits.doc(i);
            //org.apache.lucene.document.Document doc = searcher.doc(hits.scoreDocs[i].doc);

            String id = doc.get("returnid");
            if (null == id)    // für miless
              id = doc.get("mcrid");
            MCRHit hit = new MCRHit(id);
            
            Enumeration fields = doc.fields();
            while (fields.hasMoreElements()) {
                Field field = (Field) fields.nextElement();
                if (field.isStored() ) { // mainly for scorm search
                    MCRFieldDef fd = MCRFieldDef.getDef(field.name());
                    if ( null != fd )
                    {
                      MCRFieldValue fv = new MCRFieldValue(fd, field.stringValue());
                      hit.addMetaData(fv);
                    }
                }
            }
            
            
            String score = Float.toString(hits.score(i));
            addSortDataToHit(sortBy, doc, hit, score);
            result.addHit(hit);
        }

        searcher.close();

        return result;
    }
    
    public void addSortData(Iterator hits, List<MCRSortBy> sortBy)
    {
      try
      {
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
              addSortDataToHit(sortBy, doc, hit, null);
          }
      }
        
        searcher.close();
      } catch (IOException e)
      {
        LOGGER.error("Exception in MCROldLuceneSearcher (addSortData)", e);
      }
    }
    public void clearIndex() {
      try
      {
        IndexWriter writer = new IndexWriter(IndexDir, analyzer, true);
        writer.close();
      } catch (IOException e)
      {
        LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
        LOGGER.error(MCRException.getStackTraceAsString(e));
      }
    }
    
}

