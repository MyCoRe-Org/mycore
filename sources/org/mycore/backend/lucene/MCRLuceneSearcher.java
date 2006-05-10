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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.List;

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
import org.apache.lucene.search.TopDocs;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRFieldValue;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearcher;
import org.mycore.services.plugins.TextFilterPluginManager;

/**
 * This class builds indexes from mycore meta data.
 * 
 * @author Harald Richter
 */
public class MCRLuceneSearcher extends MCRSearcher {
    /** The logger */
    private final static Logger LOGGER = Logger.getLogger(MCRLuceneSearcher.class);

    String IndexDir = "";

    boolean FIRST = true;

    static String LOCK_DIR = "";
    static String DATE_FORMAT = "yyyy-MM-dd";

    static String TIME_FORMAT = "HH:mm:ss";

    static String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";

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

        LOCK_DIR = config.getString("MCR.Lucene.LockDir");
        LOGGER.info("MCR.Lucene.LockDir: " + LOCK_DIR);
        f = new File(LOCK_DIR);
        if (!f.exists())
            f.mkdirs();
        if (!f.isDirectory()) {
            String msg = LOCK_DIR + " is not a directory!";
            throw new org.mycore.common.MCRConfigurationException(msg);
        }
        if (!f.canWrite()) {
            String msg = LOCK_DIR + " is not writeable!";
            throw new org.mycore.common.MCRConfigurationException(msg);
        }
        
        System.setProperty("org.apache.lucene.lockDir", LOCK_DIR);
        deleteLuceneLocks( LOCK_DIR, 0);
        
        try
        {
          IndexWriter writer = getLuceneWriter(IndexDir, FIRST);
          FIRST = false;
          writer.close();
        } catch (IOException e)
        {
          LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
          LOGGER.error(MCRException.getStackTraceAsString(e));
        } catch (Exception e)
        {
          LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
          LOGGER.error(MCRException.getStackTraceAsString(e));
        }
        
        StandardAnalyzer standardAnalyzer = new StandardAnalyzer(); // should work like GermanAnalyzer without stemming
        List fds = MCRFieldDef.getFieldDefs( getIndex() );
        for( int i = 0; i < fds.size(); i++ )
        {
          MCRFieldDef fd = (MCRFieldDef)( fds.get( i ) );
          if ("name".equals(fd.getDataType()))                     
          {
            ((PerFieldAnalyzerWrapper)analyzer).addAnalyzer(fd.getName(), standardAnalyzer);
          }
        }
        
    }
    
    private static void deleteLuceneLocks(String lockDir, long age)
    {
      File file = new File(lockDir);

      GregorianCalendar cal = new GregorianCalendar();
      
      File f[] = file.listFiles();
      for (int i=0; i<f.length;i++)
      {
        if (!f[i].isDirectory())
        {
          String n = f[i].getName().toLowerCase();
          if (n.startsWith("lucene") && n.endsWith(".lock"))
          {
            long l = (cal.getTimeInMillis() - f[i].lastModified())/1000;   // age of file in seconds
            if ( l > age )
            {
              LOGGER.info("Delete lucene lock file " + f[i].getAbsolutePath() + " Age " + l  );
              f[i].delete();
            }
          }
        }
      }
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
            addDocumentToLucene(doc);
        } catch (Exception e) {
            LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
            LOGGER.error(MCRException.getStackTraceAsString(e));
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
                    // Field f = Field.Text(name, in);
                    Field f = new Field(name, in);
                    doc.add(f);
                }
            } else {
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
                    type = "identifier";
                } else if ("decimal".equals(type)) {
                    content = handleNumber(content, "decimal", 0);
                    type = "identifier";
                } else if ("integer".equals(type)) {
                    content = handleNumber(content, "integer", 0);
                    type = "identifier";
                }

                if (type.equals("identifier")) {
                    // doc.add(Field.Keyword(name, content));
                    doc.add(new Field(name, content, Field.Store.YES, Field.Index.UN_TOKENIZED));
                }

                if (type.equals("Text") || type.equals("name") || (type.equals("text") && field.getField().isSortable())) {
                    // doc.add(Field.Text(name, content));
                    doc.add(new Field(name, content, Field.Store.YES, Field.Index.TOKENIZED));
                } else if (type.equals("text")) {
                    // doc.add(Field.UnStored(name, content));
                    doc.add(new Field(name, content, Field.Store.NO, Field.Index.TOKENIZED));
                }
            }
        }

        return doc;
    }

    private static String handleDate(String content, String informat, String outformat) throws Exception {
        DateFormat f1 = new SimpleDateFormat(informat);
        DateFormat f2 = new SimpleDateFormat(outformat);
        Date d;
        d = f1.parse(content);

        return f2.format(d);
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

    /**
     * Adds document to Lucene
     * 
     * @param doc
     *            lucene document to add to index
     * 
     */
    private void addDocumentToLucene(Document doc) throws Exception {
        IndexWriter writer = getLuceneWriter(IndexDir, FIRST);
        FIRST = false;

        writer.addDocument(doc, analyzer);
        writer.close();
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

    protected void removeFromIndex(String entryID) {
        LOGGER.info("MCRLuceneSearcher removing indexed data of " + entryID);

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
    public static void deleteLuceneDocument(String fieldname, String id, String indexDir) throws Exception {
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

    public MCRResults search(MCRCondition cond, List order, int maxResults) {
        long start = System.currentTimeMillis();
        MCRResults results = new MCRResults();

        try {
            List f = new ArrayList();
            f.add(cond.toXML());

            boolean reqf = true; // required flag Term with AND (true) or OR
            // (false) combined
            Query luceneQuery = MCRBuildLuceneQuery.buildLuceneQuery(null, reqf, f, analyzer);
            LOGGER.debug("Lucene Query: " + luceneQuery.toString());
            results = getLuceneHits(luceneQuery, maxResults);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Exception in MCRLuceneSearcher", e);
        }

        long qtime = System.currentTimeMillis() - start;
        LOGGER.debug("total query time in MCRLuceneSearcher:    " + qtime);

        return results;
    }

    /**
     * method does lucene query
     * 
     * @return result set
     */
    private MCRResults getLuceneHits(Query luceneQuery, int maxResults) throws Exception {
        if (maxResults <= 0)
            maxResults = 100000;

        IndexSearcher searcher = null;
        try
        {
          searcher = new IndexSearcher(IndexDir);
        } catch (IOException e)
        {
          LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
          LOGGER.error(MCRException.getStackTraceAsString(e));
          deleteLuceneLocks( LOCK_DIR, 100);
        }
        
        TopDocs hits = searcher.search(luceneQuery, null, maxResults);
        int found = hits.scoreDocs.length;

        LOGGER.info("Number of Objects found : " + found);

        MCRResults result = new MCRResults();

        for (int i = 0; i < found; i++) {
            // org.apache.lucene.document.Document doc = hits.doc(i);
            org.apache.lucene.document.Document doc = searcher.doc(hits.scoreDocs[i].doc);

            String id = doc.get("returnid");
            if (null == id) // TODO: temporary used, field name of aleph index
                            // is id and NOT mcrid
                id = doc.get("id");
            /*
             * TODO if (MCRAccessManager.checkReadAccess( id,
             * MCRSessionMgr.getCurrentSession()))
             */{
                MCRHit hit = new MCRHit(id);

                Enumeration fields = doc.fields();
                while (fields.hasMoreElements()) {
                    Field field = (Field) fields.nextElement();
                    if (field.isStored() && !("mcrid".equals(field.name()) || "returnid".equals(field.name()))) {
                        MCRFieldDef fd = MCRFieldDef.getDef(field.name());
                        MCRFieldValue fv = new MCRFieldValue(fd, field.stringValue());
                        hit.addMetaData(fv);
                        if (fd.isSortable())
                            hit.addSortData(fv);
                    }
                }

                result.addHit(hit);
            } // MCRAccessManager
        }

        searcher.close();

        return result;
    }
}
