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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;

import org.mycore.backend.filesystem.MCRCStoreLocalFilesystem;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRInputStreamCloner;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileReader;
import org.mycore.services.plugins.TextFilterPluginManager;
import org.mycore.services.query.MCRTextSearchInterface;

/**
 * This class provides a content store based on lucene and the local filesystem.
 * 
 * It uses lucene for indexing know file formats and filesystem for storage.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRCStoreLucene
	extends MCRCStoreLocalFilesystem
	implements MCRTextSearchInterface {
	private static final MCRConfiguration conf = MCRConfiguration.instance();
	private static final String DERIVATE_FIELD = "DerivateID";
	private static final String STORAGE_FIELD = "StorageID";
	private static final Logger logger = Logger.getLogger(MCRCStoreLucene.class);
	private static final int optimizeIntervall = 10;
	private static final TextFilterPluginManager pMan =
		TextFilterPluginManager.getInstance();
	private static int docCount;
	private static File indexDir = null;

	private static IndexReader indexReader;
	private static Searcher indexSearcher;
	private static IndexWriter indexWriter = null;
	/**
	 * searches on the index and delivers derivate ids matching the search
	 * 
	 * Syntax:
	 * <pre>
	 * foo bar   : search for foo AND bar anywhere across the files of the derivate
	 * foo -bar  : search for foo and no file of the derivate may contain bar
	 * "foo bar" : any file of the derivate must contain the phrase foo bar.
	 * </pre>
	 * 
	 * @param doctext query
	 * @return Array of DerivateIDs
	 */
	public String[] getDerivateIDs(String docTextQuery) {
		String[] returns = null;
		//maybe transform query here
		String queryText = parseQuery(docTextQuery);
		logger.debug("TS transformed query:" + queryText);
		if (queryText.length() == 0)
			return new String[0];
		try {
			HashSet derivateIDs = getUniqueFieldValues(DERIVATE_FIELD);
			Iterator it = derivateIDs.iterator();
			Hits[] hits;
			Document doc;
			String derivateID;
			HashSet collector = new HashSet();
			int i = 0;
			while (it.hasNext()) {
				hits = getHitsForDerivate((String) it.next(), queryText);
				//we have an array of hits each should contain only
				//documents belonging to a single derivateID
				boolean ok = true;
				for (int j = 0; j < hits.length; j++)
					if (hits[j] == null || hits[j].length() == 0)
						ok = false;
				if (ok) {
					doc = hits[0].doc(0);
					derivateID = doc.get(DERIVATE_FIELD);
					if (derivateID != null) {
						logger.debug(++i + ". " + derivateID);
						collector.add(derivateID);
					} else {
						logger.warn(
							"Found Document containes no Field \"DerivateID\":" + doc);
					}
				}
				//				else{
				//					logger.error("At least one hit returned was empty!");
				//				}
			}
			returns = MCRUtils.getStringArray(collector.toArray());
		} catch (IOException e) {
			throw new MCRPersistenceException(
				"IOException while query:" + docTextQuery,
				e);
		}
		return returns;
	}

	/* (non-Javadoc)
	 * @see org.mycore.datamodel.ifs.MCRContentStore#init(java.lang.String)
	 */
	public void init(String storeID) {
		super.init(storeID);
		pMan.loadPlugins();
		indexDir = new File(conf.getString(prefix + "IndexDirectory"));
		logger.debug("TextIndexDir: " + indexDir);
		if (indexWriter == null) {
			logger.debug("creating IndexWriter...");
			try {
				if (indexDir.exists()) {
					//do some hardcore...
					Directory index = FSDirectory.getDirectory(indexDir, false);
					if (IndexReader.isLocked(indexDir.getAbsolutePath()))
						IndexReader.unlock(index);
				}
				loadIndexWriter();
				logger.debug("IndexWriter created...");
				docCount = indexWriter.docCount();
				indexWriter.close();
			} catch (IOException e) {
				logger.error("Setting indexWriter=null");
				indexWriter = null;
			}
		}
		if (indexReader == null) {
			loadIndexReader();
		}
		if (indexSearcher == null) {
			loadIndexSearcher();
		}
	}

	protected static String parseQuery(String query) {
		logger.debug("TS incoming query: " + query);
		int i = query.indexOf('\"');
		i++;
		if (i == 0)
			return "";
		int j = query.lastIndexOf('\"');
		if (j == -1)
			return "";
		return query.substring(i, j);
	}

	/* (non-Javadoc)
	 * @see org.mycore.datamodel.ifs.MCRContentStore#doDeleteContent(java.lang.String)
	 */
	protected void doDeleteContent(String storageID) throws Exception {
		//remove from index
		Term term = new Term(STORAGE_FIELD, storageID);
		int deleted = indexReader.delete(term);
		indexSearcher.close();
		indexReader.close();
		indexReader = null;
		loadIndexReader();
		loadIndexSearcher();
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
				"Failed to store file " + file.getID() + " to local file system!");
		doc = getDocument(file, isc.getNewInputStream());
		Field storageID = new Field(STORAGE_FIELD, returns, true, true, false);
		doc.add(storageID);
		try {
			indexDocument(doc);
			doc=null;
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

	protected void finalize() throws Throwable {
		logger.debug("finalize() called on Lucenestore: shutting down...");
		synchronized (indexReader) {
			indexReader.close();
			indexReader = null;
		}
		synchronized (indexWriter) {
			indexWriter.close();
			indexWriter = null;
		}
		logger.debug("shutting down... completed");
	}

	protected Document getDocument(MCRFileReader reader, InputStream stream)
		throws IOException {
		Document returns = new Document();
		ByteArrayOutputStream out=new ByteArrayOutputStream();
		//filter here
		pMan.transform(reader.getContentType(), stream, out);
		out.flush();
		byte[] temp=out.toByteArray();
		out.close();
		ByteArrayInputStream bin=new ByteArrayInputStream(temp);
		BufferedReader in = new BufferedReader(new InputStreamReader(bin));
		//reader is instance of MCRFile
		//ownerID is derivate ID for all mycore files
		if (reader instanceof MCRFile) {
			MCRFile file = (MCRFile) reader;
			Field derivateID =
				new Field(DERIVATE_FIELD, file.getOwnerID(), true, true, false);
			Field fileID = new Field("FileID", file.getID(), true, true, false);
			/* since file is stored elsewhere 
			 * we only index the file and do not store
			 */
			Field content = Field.Text("content", in);
			logger.debug("adding fields to document");
			returns.add(derivateID);
			returns.add(fileID);
			returns.add(content);
			//in.close();fin.close();tmp.delete();
			logger.debug("returning document");
			return returns;
		} else
			return null;
	}

	private final boolean containsExclusiveClause(BooleanQuery query) {
		BooleanClause[] clauses = query.getClauses();
		if (clauses.length == 1) {
			clauses = ((BooleanQuery) clauses[0].query).getClauses();
			if (clauses.length == 2)
				return clauses[1].prohibited;
		}
		return false;
	}

	private static Analyzer getAnalyzer() {
		//TODO: have to replace GermanAnalyzer by more generic
		return new GermanAnalyzer();
	}

	private Hits[] getHitsForDerivate(String derivateID, String queryText) {
		Hits[] hits = null;
		Analyzer analyzer = getAnalyzer();

		logger.debug("Query: " + derivateID + "-->" + queryText);
		LuceneCStoreQueryParser parser =
			new LuceneCStoreQueryParser("content", analyzer);
		parser.setGroupingValue(derivateID);
		//combine to a query over a specific DerivateID
		//		StringBuffer queryStr =
		//			new StringBuffer("DerivateID:\"").append(derivateID).append(
		//				"\" AND ").append(
		//				queryText);
		try {
			BooleanQuery[] queries = parser.getBooleanQueries(queryText);
			hits = new Hits[queries.length];
			for (int i = 0; i < queries.length; i++) {
				logger.debug("  -Searching for: " + queries[i].toString("content"));
				hits[i] = indexSearcher.search(queries[i]);
				if (containsExclusiveClause(queries[i])) {
					//check that all documents meets negative clause
					Hits test =
						indexSearcher.search(
							QueryParser.parse(
								derivateID,
								DERIVATE_FIELD,
								new WhitespaceAnalyzer()));
					if (test.length() != hits[i].length())
						hits[i] = null;
				}
				//logger.debug(hits[i].length() + " total matching documents");
			}
		} catch (ParseException e) {
			StringBuffer msg =
				new StringBuffer("Error while querying (")
					.append(queryText)
					.append(") over Files matching DerivateID=")
					.append(derivateID)
					.append("!");
			throw new MCRPersistenceException(msg.toString(), e);
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

	private void indexDocument(Document doc) throws IOException {
		indexSearcher.close();
		loadIndexWriter();
		logger.debug(
			"Create index for storageID="
				+ doc.getField(STORAGE_FIELD).stringValue());
		indexWriter.addDocument(doc);
		docCount++;
		if (docCount % optimizeIntervall == 0) {
			logger.debug("Optimize index for searching...");
			indexWriter.optimize();
		}
		indexWriter.close();
		loadIndexSearcher();
	}

	private synchronized void loadIndexReader() {
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
	private synchronized void loadIndexSearcher() {
		if (indexReader == null) {
			loadIndexReader();
		}
		indexSearcher = new IndexSearcher(indexReader);
	}
	private synchronized void loadIndexWriter() {
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
		indexWriter.minMergeDocs = 1; //always write to local dir
	}

}
