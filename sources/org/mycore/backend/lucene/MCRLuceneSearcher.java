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
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRFieldValue;
import org.mycore.services.fieldquery.MCRQueryParser;
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

    static final private MCRConfiguration CONFIG = MCRConfiguration.instance();

    String IndexDir = "";

    boolean FIRST = true;

    // TODO: read from property file
    static String DATE_FORMAT = "yyyy-MM-dd";
    
    static String TIME_FORMAT = "hh:mm:ss";

    static String TIMESTAMP_FORMAT = "yyyy-MM-dd hh:mm:ss";
    
    static int INT_BEFORE = 10;
    
    static int DEC_BEFORE = 10;
    static int DEC_AFTER  = 4;

    private static TextFilterPluginManager PLUGIN_MANAGER = null;

    public void init(String ID) {
        super.init(ID);
        IndexDir = CONFIG.getString(prefix + "IndexDir");
        LOGGER.info(prefix + "indexDir: " + IndexDir);
        File f = new File( IndexDir );
        if( ! f.exists() ) f.mkdirs();
        if( ! f.isDirectory() )
        {
            String msg = IndexDir + " is not a directory!";
            throw new org.mycore.common.MCRConfigurationException( msg );
        }
        if( ! f.canWrite() )
        {
            String msg = IndexDir + " is not writeable!";
            throw new org.mycore.common.MCRConfigurationException( msg );
        }

        String lockDir = CONFIG.getString("MCR.Lucene.LockDir");
        LOGGER.info("MCR.Lucene.LockDir: " + lockDir);
        f = new File( lockDir );
        if( ! f.exists() ) f.mkdirs();
        if( ! f.isDirectory() )
        {
            String msg = lockDir + " is not a directory!";
            throw new org.mycore.common.MCRConfigurationException( msg );
        }
        if( ! f.canWrite() )
        {
            String msg = lockDir + " is not writeable!";
            throw new org.mycore.common.MCRConfigurationException( msg );
        }
        System.setProperty("org.apache.lucene.lockdir", lockDir);
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
                } else if ("decimal".equals(type))
                {
                  content    = handleNumber(content, "decimal", 0);
                  type       = "identifier";
                } else if ("integer".equals(type))
                {
                  content    = handleNumber(content, "integer", 0);
                  type       = "identifier";
                }

                if (type.equals("identifier")) {
                    doc.add(Field.Keyword(name, content));
                }

                if (type.equals("Text") || type.equals("name") || (type.equals("text") && field.getField().isSortable() )) {
                    doc.add(Field.Text(name, content));
                } else if (type.equals("text")) {
                    doc.add(Field.UnStored(name, content));
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

    public static String handleNumber(String content, String type, long add)
    {
      int before, after;  
      int dez;
      long l;
      if ( "decimal".equals(type))
      {
        before   = DEC_BEFORE;
        after    = DEC_AFTER;
        dez      = before + after;
        double d = Double.parseDouble(content);
        d        = d*Math.pow(10, after) + Math.pow(10, dez);
        l        = (long)d;
      }
      else
      {
        before   = INT_BEFORE;
        dez      = before;
        l        = Long.parseLong(content);
        l        = l +(long)(Math.pow(10, dez) + 0.1);
      }
      
      long m     = l + add;
      String n   = "0000000000000000000";
      String h   = Long.toString(m);
      return n.substring(0,dez+1-h.length()) + h;
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

        writer = getLuceneWriter(IndexDir, FIRST);
        FIRST = false;

        writer.addDocument(doc);
        writer.close();
    }

    /**
     * Get Lucene Writer
     * 
     * @param indexDir
     *            directory where lucene index is store
     *            first  check existance of index directory, if it does nor exist create it
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
        writer.mergeFactor = 200;
        writer.maxMergeDocs = 2000;
        
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
     * @param indexDir
     *      *     the directory where index is stored
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

        if ( hits.length() > 0) {
          IndexReader reader = IndexReader.open(indexDir);
          for (int i = 0; i < hits.length(); i++)
          {
            reader.delete(hits.id(i));
          }
          LOGGER.info("DELETE: " + id);
          reader.close();
        }
        
        searcher.close();
    }

    public MCRResults search(MCRCondition cond, List order, int maxResults) {
        MCRResults results = new MCRResults();

        try {
            MCRLuceneQuery lucenequery = new MCRLuceneQuery(cond, maxResults, IndexDir);
            results = lucenequery.getLuceneHits();
        } catch (Exception e) {
            e.printStackTrace();
        }

        LOGGER.debug("MCRLuceneSearcher results completed");

        return results;
    }
    
    /**
     * Test application, query  lucene index.
     */
    public static void main(String[] args) throws Exception {
      if ( 4 == args.length && "-query".equals(args[0]) &&             // build Query from xml-File 
           "-indexer".equals(args[2]) )                                // use this indexer 
      {
        org.jdom.Element query = MCRURIResolver.instance().resolve("file://" + args[1]);
        MCRLuceneSearcher ls = new MCRLuceneSearcher();
        ls.init( args[3] );

        MCRCondition  cond = new MCRQueryParser().parse((Element) query.getChild("conditions").getChildren().get(0));
        int maxResults = 100;
        MCRResults res = ls.search(cond, null, maxResults);

        org.jdom.Element doc = res.buildXML();
        XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
        System.out.println(out.outputString(doc));

      } else
      {
        System.out.println("usage: query data\n");
        System.out.println("-query     file with query   -indexer  name of indexer");
      }
      
    }

}
