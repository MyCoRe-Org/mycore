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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;

import org.mycore.backend.filesystem.MCRCStoreLocalFilesystem;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRInputStreamCloner;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileContentType;
import org.mycore.datamodel.ifs.MCRFileReader;
import org.mycore.services.plugins.TextFilterPluginManager;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * Need to insert some things here
 *
 */
public class MCRCStoreLucene extends MCRCStoreLocalFilesystem {
	private static final TextFilterPluginManager pMan =
		TextFilterPluginManager.getInstance();
	private static final Logger logger =
		Logger.getLogger(MCRCStoreLucene.class);
	private static final MCRConfiguration conf = MCRConfiguration.instance();
	private static File indexDir = null;
	private static IndexWriter indexWriter = null;
	private static final int optimizeIntervall = 10;
	private static int docCount;

	private static IndexReader indexReader;
	/* (non-Javadoc)
	 * @see org.mycore.datamodel.ifs.MCRContentStore#doDeleteContent(java.lang.String)
	 */
	protected void doDeleteContent(String storageID) throws Exception {
		//remove from index
		Term term = new Term("StorageID", storageID);
		int deleted = indexReader.delete(term);
		logger.debug("deleted " + deleted + " documents containing " + term);
		//remove file
		super.doDeleteContent(storageID);
	}

	/* (non-Javadoc)
	 * @see org.mycore.datamodel.ifs.MCRContentStore#doStoreContent(org.mycore.datamodel.ifs.MCRFileReader, org.mycore.datamodel.ifs.MCRContentInputStream)
	 */
	protected String doStoreContent(
		MCRFileReader file,
		MCRContentInputStream source)
		throws Exception {
		Document doc = null;
		MCRInputStreamCloner isc = new MCRInputStreamCloner(source);
		source = new MCRContentInputStream(isc.getNewInputStream());
		InputStream sourceStream = isc.getNewInputStream();
		String returns = super.doStoreContent(file, source);
		if (returns == null || returns.length() == 0)
			throw new MCRPersistenceException(
				"Failed to store file "
					+ file.getID()
					+ " to local file system!");
		doc = getDocument(file, isc.getNewInputStream());
		Field storageID = new Field("StorageID", returns, true, true, false);
		doc.add(storageID);
		try {
			indexDocument(doc);
		} catch (IOException io) {
			//Document was not added
			//remove file from local FileStore
			super.deleteContent(returns);
			//send Exception
			throw new MCRPersistenceException(
				"Failed to store file "
					+ file.getID()
					+ " to local file system!\n"
					+ "Cannot index file content!",
				io);
		}
		return returns;
	}

	/* (non-Javadoc)
	 * @see org.mycore.datamodel.ifs.MCRContentStore#init(java.lang.String)
	 */
	public void init(String storeID) {
		super.init(storeID);
		pMan.loadPlugins();
		indexDir = new File(conf.getString("MCR.store_lucene_searchindexdir"));
		logger.debug("TextIndexDir: " + indexDir);
		if (indexWriter == null) {
			boolean create = true;
			if (IndexReader.indexExists(indexDir)) {
				//reuse Index
				create = false;
			}
			try {
				indexWriter = new IndexWriter(indexDir, getAnalyzer(), create);
			} catch (IOException e) {
				throw new MCRPersistenceException(
					"Cannot create index in "
						+ indexDir.getAbsolutePath()
						+ File.pathSeparatorChar
						+ indexDir.getName(),
					e);
			}
			indexWriter.mergeFactor = optimizeIntervall;
			docCount = indexWriter.docCount();
		}
		if (indexReader == null) {
			try {
				indexReader = IndexReader.open(indexDir);
			} catch (IOException e) {
				throw new MCRPersistenceException(
					"Cannot read index in "
						+ indexDir.getAbsolutePath()
						+ File.pathSeparatorChar
						+ indexDir.getName(),
					e);
			}
		}

	}

	protected Document getDocument(MCRFileReader reader, InputStream stream)
		throws IOException {
		Document returns = new Document();
		PipedInputStream pin = new PipedInputStream();
		PipedOutputStream pout = new PipedOutputStream(pin);
		PrintStream out = new PrintStream(pout);
		BufferedReader in = new BufferedReader(new InputStreamReader(pin));
		//filter here
		pMan.transform(reader.getContentType(), stream, out);
		//reader is instance of MCRFile
		//ownerID is derivate ID for all mycore files
		if (reader instanceof MCRFile) {
			MCRFile file = (MCRFile) reader;
			Field derivateID =
				new Field("DerivateID", file.getOwnerID(), true, true, false);
			Field fileID = new Field("FileID", file.getID(), true, true, false);
			/* since file is stored elsewhere 
			 * we only index the file and do not store
			 */
			Field content = Field.Text("content", in);
			returns.add(derivateID);
			returns.add(fileID);
			returns.add(content);
			return returns;
		} else
			return null;
	}
	protected String[] getDerivateID(String docTextQuery) {
		String[] returns = null;
		//maybe transform query here
		String queryText = docTextQuery;
		try {
			HashSet derivateIDs = getUniqueFieldValues("DerivateID");
			Iterator it = derivateIDs.iterator();
			Hits hits;
			Document doc;
			String derivateID;
			HashSet collector = new HashSet();
			int i = 0;
			while (it.hasNext()) {
				hits = getHitsForDerivate((String) it.next(), queryText);
				if (hits.length() > 0) {
					doc = hits.doc(0);
					derivateID = doc.get("DerivateID");
					if (derivateID != null) {
						logger.debug(++i + ". " + derivateID);
						collector.add(derivateID);
					} else {
						logger.warn(
							"Found Document containes no Field \"DerivateID\":"
								+ doc);
					}
				}
			}
			returns = MCRUtils.getStringArray(collector.toArray());
		} catch (IOException e) {
			throw new MCRPersistenceException(
				"IOException while query:" + docTextQuery,
				e);
		}
		return returns;
	}

	private void indexDocument(Document doc) throws IOException {
		logger.debug("Create index for storageID=" + doc.getField("StorageID"));
		indexWriter.addDocument(doc);
		docCount++;
		if (docCount % optimizeIntervall == 0) {
			logger.debug("Optimize index for searching...");
			indexWriter.optimize();
		}
	}
	private static Analyzer getAnalyzer() {
		//TODO: have to replace GermanAnalyzer by more generic
		return new GermanAnalyzer();
	}

	private HashSet getUniqueFieldValues(String fieldName) {
		HashSet collector = new HashSet();
		if (fieldName == null || fieldName.length() == 0)
			return collector;
		TermEnum enum = null;
		try {
			try {
				enum = indexReader.terms(new Term(fieldName, ""));
				while (fieldName.equals(enum.term().field())) {
					//... collect enum.term().text() ...
					collector.add(enum.term().text());
					if (!enum.next())
						break;
				}
			} finally {
				enum.close();
			}
		} catch (IOException e) {
			StringBuffer msg =
				new StringBuffer("Error while fetching unique values of field ")
					.append(fieldName)
					.append("!");
			throw new MCRPersistenceException(msg.toString(), e);
		}
		return collector;
	}
	private Hits getHitsForDerivate(String derivateID, String queryText) {
		Hits hits = null;
		Searcher searcher;
		Analyzer analyzer = getAnalyzer();

		BufferedReader in =
			new BufferedReader(new InputStreamReader(System.in));
		logger.debug("Query: ");
		QueryParser parser = new QueryParser("content", analyzer);
		//combine to a query over a specific DerivateID
		StringBuffer queryStr =
			new StringBuffer("DerivateID:\"").append(derivateID).append(
				"\" ").append(
				queryText);
		try {
			searcher = new IndexSearcher(indexReader);
			try {
				Query query = parser.parse(queryStr.toString());
				logger.debug("Searching for: " + query.toString("content"));
				hits = searcher.search(query);
				logger.debug(hits.length() + " total matching documents");
			} catch (ParseException e) {
				StringBuffer msg =
					new StringBuffer("Error while querying (")
						.append(queryText)
						.append(") over Files matching DerivateID=")
						.append(derivateID)
						.append("!");
				throw new MCRPersistenceException(msg.toString(), e);
			} finally {
				searcher.close();
			}
		} catch (IOException e) {
			StringBuffer msg =
				new StringBuffer("Error while querying (")
					.append(queryText)
					.append(") over Files matching DerivateID=")
					.append(derivateID)
					.append("!");
			throw new MCRPersistenceException(msg.toString(), e);
		}
		return hits;
	}

}
