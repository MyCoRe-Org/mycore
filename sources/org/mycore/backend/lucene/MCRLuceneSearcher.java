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
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexModifier;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
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
    /** The logger */
    private final static Logger LOGGER = Logger.getLogger(MCRLuceneSearcher.class);

    static File LOCK_DIR;

    static int INT_BEFORE = 10;

    static int DEC_BEFORE = 10;

    static int DEC_AFTER = 4;

    private static TextFilterPluginManager PLUGIN_MANAGER = null;

    static Analyzer analyzer = new PerFieldAnalyzerWrapper(new GermanAnalyzer());

    File IndexDir;

    private IndexModifyExecutor modifyExecutor;

    public void init(String ID) {
        super.init(ID);

        MCRConfiguration config = MCRConfiguration.instance();
        IndexDir = new File(config.getString(prefix + "IndexDir"));
        LOGGER.info(prefix + "indexDir: " + IndexDir);
        if (!IndexDir.exists())
            IndexDir.mkdirs();
        if (!IndexDir.isDirectory()) {
            String msg = IndexDir + " is not a directory!";
            throw new MCRConfigurationException(msg);
        }
        if (!IndexDir.canWrite()) {
            String msg = IndexDir + " is not writeable!";
            throw new MCRConfigurationException(msg);
        }

        LOCK_DIR = new File(config.getString("MCR.Lucene.LockDir"));
        LOGGER.info("MCR.Lucene.LockDir: " + LOCK_DIR);
        if (!LOCK_DIR.exists())
            LOCK_DIR.mkdirs();
        if (!LOCK_DIR.isDirectory()) {
            String msg = LOCK_DIR + " is not a directory!";
            throw new MCRConfigurationException(msg);
        }
        if (!LOCK_DIR.canWrite()) {
            String msg = LOCK_DIR + " is not writeable!";
            throw new MCRConfigurationException(msg);
        }

        System.setProperty("org.apache.lucene.lockDir", LOCK_DIR.getAbsolutePath());
        deleteLuceneLocks(LOCK_DIR, 0);
        
        // is index directory initialized, .....?
        try {
          IndexWriter writer = MCRLuceneTools.getLuceneWriter(config.getString(prefix + "IndexDir"), true);
          writer.close();
      } catch (IOException e) {
          LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
          LOGGER.error(MCRException.getStackTraceAsString(e));
      } catch (Exception e) {
          LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
          LOGGER.error(MCRException.getStackTraceAsString(e));
      }


        try {
            modifyExecutor = new IndexModifyExecutor(new LinkedBlockingQueue<Runnable>(),IndexDir);
        } catch (Exception e) {
            throw new MCRException("Cannot start IndexModifier thread.", e);
        }
        // should work like GermanAnalyzer without stemming and removing of stopwords
        SimpleAnalyzer simpleAnalyzer = new SimpleAnalyzer();
        List fds = MCRFieldDef.getFieldDefs(getIndex());
        for (int i = 0; i < fds.size(); i++) {
            MCRFieldDef fd = (MCRFieldDef) (fds.get(i));
            if ("name".equals(fd.getDataType())) {
                ((PerFieldAnalyzerWrapper) analyzer).addAnalyzer(fd.getName(), simpleAnalyzer);
            }
        }
        MCRShutdownHandler.getInstance().addCloseable(this);
    }

    private static void deleteLuceneLocks(File lockDir, long age) {
        GregorianCalendar cal = new GregorianCalendar();

        File f[] = lockDir.listFiles();
        for (int i = 0; i < f.length; i++) {
            if (!f[i].isDirectory()) {
                String n = f[i].getName().toLowerCase();
                if (n.startsWith("lucene") && n.endsWith(".lock")) {
                    // age of file in seconds
                    long l = (cal.getTimeInMillis() - f[i].lastModified()) / 1000;
                    if (l > age) {
                        LOGGER.info("Delete lucene lock file " + f[i].getAbsolutePath() + " Age " + l);
                        f[i].delete();
                    }
                }
            }
        }
    }

    public static String handleNumber(String content, String type, long add) {
        int before, after;
        int dez;
        long l;
        try {
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
	            if ( content.indexOf('.')>0)
	            	content = content.substring(content.lastIndexOf('.')+1);
	            l = Long.parseLong(content);
	            l = l + (long) (Math.pow(10, dez) + 0.1);
	        }
	        long m = l + add;
	        String n = "0000000000000000000";
	        String h = Long.toString(m);
	        return n.substring(0, dez + 1 - h.length()) + h;
        } catch ( Exception all) {
        	LOGGER.info("MCRLuceneSearcher can't format this Number, ignore this content: " + content);
        	return "0";
        }
    }

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
     * @param indexDir *
     *            the directory where index is stored
     * 
     */
    public void deleteLuceneDocument(String fieldname, String id) throws Exception {
        Term deleteTerm = new Term(fieldname, id);
        IndexModifierAction modifyAction = IndexModifierAction.removeAction(modifyExecutor, deleteTerm);
        modifyIndex(modifyAction);
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
            LOGGER.error("Exception in MCRLuceneSearcher", e);
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
            searcher = new IndexSearcher(IndexDir.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
            LOGGER.error(MCRException.getStackTraceAsString(e));
            deleteLuceneLocks(LOCK_DIR, 100);
        }

        Hits hits = searcher.search( luceneQuery );
        int found = hits.length();
        //TopDocs hits = searcher.search(luceneQuery, null, maxResults);
        //int found = hits.scoreDocs.length;

        LOGGER.info("Number of Objects found : " + found);

        MCRResults result = new MCRResults();

        for (int i = 0; i < found; i++) {
            org.apache.lucene.document.Document doc = hits.doc(i);
            //org.apache.lucene.document.Document doc = searcher.doc(hits.scoreDocs[i].doc);

            String id = doc.get("returnid");
            MCRHit hit = new MCRHit(id);
            String score = Float.toString(hits.score(i));
            //String score = Float.toString(hits.scoreDocs[i].score);
            addSortDataToHit(sortBy, doc, hit, score);
            result.addHit(hit);
        }

        searcher.close();

        return result;
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

    public void addToIndex(String entryID, String returnID, List fields) {
        LOGGER.info("MCRLuceneSearcher indexing data of " + entryID);

        if ((fields == null) || (fields.size() == 0)) {
            return;
        }

        try {
            Document doc = buildLuceneDocument(fields);
            doc.add(new Field("mcrid", entryID, Field.Store.YES, Field.Index.UN_TOKENIZED));
            doc.add(new Field("returnid", returnID, Field.Store.YES, Field.Index.UN_TOKENIZED));
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
        IndexModifierAction modifyAction = IndexModifierAction.addAction(modifyExecutor, doc, analyzer);
        modifyIndex(modifyAction);
    }

    private void modifyIndex(IndexModifierAction modifyAction) {
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
                    String s;
                    StringBuffer text = new StringBuffer();
                    while ((s = in.readLine()) != null) {
                        text.append(s).append(" ");
                    }

                    s = text.toString();
                    s = MCRNormalizer.normalizeString(s);

                    doc.add(new Field(name, s, Field.Store.NO, Field.Index.TOKENIZED));
                }
            } else {
                if ("date".equals(type) || "time".equals(type) || "timestamp".equals(type)) {
                    type = "identifier";
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
                    doc.add(new Field(name, content, Field.Store.YES, Field.Index.UN_TOKENIZED));
                }

                if (type.equals("Text") || type.equals("name") || (type.equals("text") && field.getField().isSortable())) {
                    doc.add(new Field(name, content, Field.Store.YES, Field.Index.TOKENIZED));
                } else if (type.equals("text")) {
                    doc.add(new Field(name, content, Field.Store.NO, Field.Index.TOKENIZED));
                }
            }
        }

        return doc;
    }

    public void addSortData(Iterator hits, List<MCRSortBy> sortBy) {
        try {
            IndexSearcher searcher = new IndexSearcher(IndexDir.getAbsolutePath());
    
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
        } catch (IOException e) {
            LOGGER.error("Exception in MCRLuceneSearcher (addSortData)", e);
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
    
    public void clearIndex(String fieldname, String value) {
      try
      {
          deleteLuceneDocument(fieldname, value);
      } catch (Exception e)
      {
        LOGGER.error(e.getClass().getName() + ": " + e.getMessage());
        LOGGER.error(MCRException.getStackTraceAsString(e));
      }
    }
    
    public void close() {
        LOGGER.info("Closing "+toString()+"...");
        modifyExecutor.shutdown();
        try {
            modifyExecutor.awaitTermination(60*60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.warn("Error while closing "+toString(),e);
        }
        LOGGER.info("Processed "+modifyExecutor.getCompletedTaskCount()+" modification requests.");
    }
    
    public String toString() {
        return getClass().getSimpleName()+":"+ID;
    }

    private static class IndexModifyExecutor extends ThreadPoolExecutor {
        boolean modifierClosed, firstJob, closeModifierEarly;

        private IndexModifier indexModifier;

        private File indexDir;

        public IndexModifyExecutor(BlockingQueue<Runnable> workQueue, File indexDir) {
            //single thread mode
            super(1, 1, 0, TimeUnit.SECONDS, workQueue);
            this.indexDir = indexDir;
            modifierClosed=true;
            firstJob=true;
            closeModifierEarly=MCRConfiguration.instance().getBoolean("MCR.Lucene.closeModifierEarly", false);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            if (firstJob)
                firstJob=false;
            if (getQueue().isEmpty() || closeModifierEarly)
                closeIndexModifier();
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
            if (modifierClosed)
                openIndexModifier();
        }

        private void openIndexModifier() {
            try {
                LOGGER.debug("Opening Lucene index for writing.");
                indexModifier = getLuceneModifier(indexDir, firstJob);
            } catch (Exception e) {
                LOGGER.warn("Error while reopening IndexModifier.", e);
            }
            modifierClosed = false;
        }

        private void closeIndexModifier() {
            try {
                LOGGER.debug("Writing Lucene index changes to disk.");
                indexModifier.close();
            } catch (IOException e) {
                LOGGER.warn("Error while closing IndexModifier.", e);
            } catch (IllegalStateException e) {
                LOGGER.debug("IndexModifier was allready closed.");
            }
            modifierClosed = true;
        }

        private static IndexModifier getLuceneModifier(File indexDir, boolean first) throws Exception {
            IndexModifier modifier;
            Analyzer analyzer = new GermanAnalyzer();
            boolean create = false;
            //check if indexDir is empty before creating a new index
            if (first && (indexDir.list().length == 0)) {
                LOGGER.info("No Entries in Directory, initialize: " + indexDir);
                create = true;
            }
            modifier = new IndexModifier(indexDir, analyzer, create);
            modifier.setMergeFactor(200);
            modifier.setMaxBufferedDocs(2000);
            return modifier;
        }

        public IndexModifier getIndexModifier() {
            return indexModifier;
        }
    }

    private static class IndexModifierAction implements Runnable {
        private IndexModifyExecutor executor;

        private Document doc;

        private Analyzer analyzer;

        private boolean delete = false;

        private Term deleteTerm;

        private IndexModifierAction(IndexModifyExecutor executor) {
            this.executor = executor;
        }

        public static IndexModifierAction addAction(IndexModifyExecutor executor, Document doc, Analyzer analyzer) {
            IndexModifierAction e = new IndexModifierAction(executor);
            e.doc = doc;
            e.analyzer = analyzer;
            return e;
        }

        public static IndexModifierAction removeAction(IndexModifyExecutor executor, Term deleteTerm) {
            IndexModifierAction e = new IndexModifierAction(executor);
            e.delete = true;
            e.deleteTerm = deleteTerm;
            return e;
        }

        public void run() {
            try {
                if (delete) {
                    deleteDocument();
                } else {
                    addDocument();
                }
            } catch (IOException e) {
                LOGGER.error("Error while writing Lucene Document: " + doc.toString(), e);
            }
        }

        private void addDocument() throws IOException {
            LOGGER.debug("add Document:" + toString());
            executor.getIndexModifier().addDocument(doc, analyzer);
            LOGGER.debug("adding done.");
        }

        private void deleteDocument() throws IOException {
            LOGGER.debug("delete Document:" + toString());
            executor.getIndexModifier().deleteDocuments(deleteTerm);
        }

        public String toString() {
            if (doc != null)
                return doc.toString();
            if (deleteTerm != null)
                return deleteTerm.toString();
            return "empty IndexModifierAction";
        }
    }
}