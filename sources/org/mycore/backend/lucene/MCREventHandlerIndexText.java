/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.backend.lucene;

import java.io.BufferedReader;
import java.io.File;

import org.apache.log4j.Logger;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.common.events.*;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRPersistenceException;

import org.mycore.services.plugins.FilterPluginTransformException;
import org.mycore.services.plugins.TextFilterPluginManager;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Hits;

/**
 * This class builds fulltext indexes of file content with lucene.
 * TextFilterPluginManager is used. If MRCFile can be converted in text it will be indexed.
 * 
 * @author Harald Richter
 */
public class MCREventHandlerIndexText extends MCREventHandlerBase {
    static boolean first = true;

    private static Logger logger = Logger.getLogger(MCREventHandlerIndexText.class);
    private static final MCRConfiguration CONF = MCRConfiguration.instance();
    private static String indexDir = null;

    private static final TextFilterPluginManager PLUGIN_MANAGER = TextFilterPluginManager
    .getInstance();
    static 
    {
      PLUGIN_MANAGER.loadPlugins();
      
      indexDir = CONF.getString("MCR.IFS.ContentStore.Lucene.IndexDirectory");
      logger.info("MCR.IFS.ContentStore.Lucene.IndexDirectory: " + indexDir);
//TODO use this instead      
//      indexDir = CONF.getString("MCR.content_lucene_searchindexdir");
//      logger.info("MCR.content_lucene_searchindexdir: " + indexDir);
      
      String lockDir = CONF.getString("MCR.content_lucene_lockdir", "");
      logger.info("MCR.content_lucene_lockdir: " + lockDir);
      File file = new File(lockDir);

      if (!file.exists()) {
          logger.info("Lock Directory for Lucene doesn't exist: \"" + lockDir
                  + "\" use " + System.getProperty("java.io.tmpdir"));
      } else if (file.isDirectory()) {
          System.setProperty("org.apache.lucene.lockdir", lockDir);
      }
    }

    /**
     * Handles file created events. This implementation builds fulltext index of file 
     * content with lucene.
     * 
     * @param evt
     *            the event that occured
     * @param file
     *            the MCRFile that caused the event
     */
    protected void handleFileCreated(MCREvent evt, MCRFile file) {
    if (!PLUGIN_MANAGER.isSupported(file.getContentType())) {
      return;
    }
    logger.debug("##### Content type Start Index ####################"
        + file.getContentType().getID());
    Document doc = buildLuceneDocument( file );
    try {
      addDocumentToLucene( doc );
    } catch (Exception e) {
      logger.warn(e.getMessage());
  }
    }
    
    /**
     * Build lucene document MCRFile
     * 
     * @param file
     *            MCRFile Object
     * 
     * @return The lucene document
     *  
     */
    protected Document buildLuceneDocument(MCRFile file)
  {
    Document doc = new Document();
    Field derivateID = new Field("DerivateID", file.getOwnerID(), true, true, false);
    Field fileID = new Field("FileID", file.getID(), true, true, false);
    
    logger.debug("adding fields to document " + file.getID());
    doc.add(derivateID);
    doc.add(fileID);
    try
    {
      BufferedReader in = new BufferedReader(PLUGIN_MANAGER.transform(file.getContentType(),
          file.getContentAsInputStream() ));
      /*
       * since file is stored elsewhere we only index the file and do not store
       */
      Field content = Field.Text("content", in);
      doc.add(content);
    } catch (Exception fe)
    {
      //no transformation was done because of an error
      logger.error("Error while transforming document.", fe);
      return doc;
    }
    logger.debug("returning document");
    return doc;
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
        if (first) {
            first = false;
            File file = new File(indexDir);

            if (!file.exists()) {
                logger.info("The Directory doesn't exist: " + indexDir
                        + " try to build it");
                IndexWriter writer2 = new IndexWriter(indexDir, analyzer, true);
                writer2.close();
            } else if (file.isDirectory()) {
                if (0 == file.list().length) {
                    logger.info("No Entries in Directory, initialize: "
                            + indexDir);
                    IndexWriter writer2 = new IndexWriter(indexDir, analyzer,
                            true);
                    writer2.close();
                }
            }

        } // if ( first

        if (null == writer) {
            writer = new IndexWriter(indexDir, analyzer, false);
            writer.mergeFactor = 200;
            writer.maxMergeDocs = 2000;
        }

        writer.addDocument(doc);
        writer.close();
        writer = null;
    }
    
    /**
     * Handles file updated events. This implementation updates fulltext index.
     * 
     * @param evt
     *          the event that occured
     * @param file
     *          the MCRFile that caused the event
     */
    protected void handleFileUpdated(MCREvent evt, MCRFile file) {
        logger.debug("This default handler implementation does nothing (UPDATE TEXT)");
        handleFileDeleted( evt, file );
        handleFileCreated( evt, file );
    }

    /**
     * Handles file deleted events. This implementation delete entry in index.
     * 
     * @param evt
     *            the event that occured
     * @param file
     *            the MCRFile that caused the event
     */
    protected void handleFileDeleted(MCREvent evt, MCRFile file) {

      try {
        deleteLuceneDocument( file.getID() );
      } catch (Exception e) {
        logger.warn(e.getMessage());
    }
    }
    /**
     * Delete document in Lucene
     * 
     * @param id
     *            string document id
     *  
     */
    private void deleteLuceneDocument(String id) throws Exception {

        IndexSearcher searcher = new IndexSearcher(indexDir);

        if (null == searcher)
            return;
        Term te1 = new Term("FileID", id);

        TermQuery qu = new TermQuery(te1);

        logger.info("Searching for: " + qu.toString(""));

        Hits hits = searcher.search(qu);

        logger.info("Number of documents found : " + hits.length());
        if (1 == hits.length()) {
            logger.info(" id: " + hits.id(0) + " score: " + hits.score(0)
                    + " key: " + hits.doc(0).get("FileID"));
            if (id.equals(hits.doc(0).get("FileID"))) {
                IndexReader reader = IndexReader.open(indexDir);
                reader.delete(hits.id(0));
                reader.close();
                logger.info("DELETE: " + id);
            }
        }

    }
}
