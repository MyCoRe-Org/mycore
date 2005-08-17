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

import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRObject;

import org.mycore.services.fieldquery.*;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;

/**
 * This class builds indexes from mycore meta data.
 * MCRMetadata2Fields is used to generate searchfields.
 * 
 * @author Harald Richter
 */
public class MCREventHandlerIndexMeta extends MCREventHandlerBase {
	private static Logger logger = Logger.getLogger(MCREventHandlerIndexMeta.class);

	static final private MCRConfiguration config = MCRConfiguration.instance();

	static String indexDir = "";

	static boolean first = true;

	//TODO: read from property file
	static String dateformat = "yyyy-MM-dd";

	/** Reads properties from configuration file when class first used */
	static {
		MCRConfiguration config = MCRConfiguration.instance();

		indexDir = config.getString("MCR.meta_lucene_searchindexdir");
		logger.info("MCR.meta_lucene_searchindexdir: " + indexDir);
		String lockDir = config.getString("MCR.meta_lucene_lockdir", "");
		logger.info("MCR.meta_lucene_lockdir: " + lockDir);
		File file = new File(lockDir);

		if (!file.exists()) {
			logger.info("Lock Directory for Lucene doesn't exist: \"" + lockDir
					+ "\" use " + System.getProperty("java.io.tmpdir"));
		} else if (file.isDirectory()) {
			System.setProperty("org.apache.lucene.lockdir", lockDir);
		}
	}

	/**
   * This class builds indexes of meta data objects.
	 * MCRMetadata2Fields is used to generate searchfields.
	 * 
	 * @param evt
	 *            the event that occured
	 * @param obj
	 *            the MCRObject that caused the event
	 */
	protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
		List fields =  MCRMetadata2Fields.buildFields(obj);
		try
    {
      Document doc = buildLuceneDocument(fields);
      logger.debug("####### lucene document build ");
      addDocumentToLucene(doc);
    } catch (Exception e)
    {
      e.printStackTrace();
      logger.warn( "xxxxx " + e.getMessage());
    }
	}
	
  /**
   * Build lucene document from transformed xml list 
   * @param fields corresponding to lucene fields
   * 
   * @return The lucene document
   * 
   **/
  private Document buildLuceneDocument( List fields )
  throws Exception {
    Document doc = new Document();
    
    for (int i = 0; i < fields.size(); i++)
    {
      org.jdom.Element xType = (org.jdom.Element) (fields.get(i));
      String name = xType.getAttributeValue("name");
      String type = xType.getAttributeValue("type");
      String content = xType.getAttributeValue("value");
      if (null != name && null != type && null != content)
      {
        if ("date".equals(type))
        {
          DateFormat f1 = new SimpleDateFormat(dateformat);
          DateFormat f2 = new SimpleDateFormat("yyyyMMdd");
          Date d;
          d = f1.parse(content);
          content = f2.format(d);
          type = "Text";
        }

        logger.debug("####### Name: " + name + " Type: " + type + " Content: " + content);
        if (type.equals("identifier"))
          doc.add(Field.Keyword(name, content));
        if (type.equals("Text") || type.equals("name"))
          doc.add(Field.Text(name, content));
        if (type.equals("text"))
          doc.add(Field.UnStored(name, content));
      }
    }
      
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
				String names[] = file.list();
				
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
   * Updates Object in lucene index.
   * 
   * @param evt
   *          the event that occured
   * @param obj
   *          the MCRObject that caused the event
   */
	protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
		handleObjectDeleted(evt, obj);
		handleObjectCreated(evt, obj);
	}

	/**
	 * Deletes Object in lucene index.
	 * 
	 * @param evt
	 *            the event that occured
	 * @param obj
	 *            the MCRObject that caused the event
	 */
	protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
		try {
			deleteLuceneDocument(obj.getId().getId());
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
		Term te1 = new Term("id", id);

		TermQuery qu = new TermQuery(te1);

		logger.info("Searching for: " + qu.toString(""));

		Hits hits = searcher.search(qu);

		logger.info("Number of documents found : " + hits.length());
		if (1 == hits.length()) {
			logger.info(" id: " + hits.id(0) + " score: " + hits.score(0)
					+ " key: " + hits.doc(0).get("id"));
			if (id.equals(hits.doc(0).get("id"))) {
				IndexReader reader = IndexReader.open(indexDir);
				reader.delete(hits.id(0));
				reader.close();
				logger.info("DELETE: " + id);
			}
		}

	}

}