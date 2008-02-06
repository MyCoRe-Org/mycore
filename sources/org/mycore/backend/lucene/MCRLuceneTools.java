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
import java.io.StringReader;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.mycore.common.MCRConfiguration;
import java.util.Map;
import java.util.HashMap;

/**
 * Use Lucene Analyzer to normalize strings
 * 
 * @author Harald Richter
 * 
 * @version $Revision$ $Date$
 * 
 */
public class MCRLuceneTools {
    MCRConfiguration config = MCRConfiguration.instance();
    private static Map<String,Analyzer> analyzerMap = new HashMap<String,Analyzer>();
    /** The logger */
    private final static Logger LOGGER = Logger.getLogger(MCRLuceneTools.class);

    /**
     * Use Lucene Analyzer to normalize strings
     * 
     * @param value
     *            string to convert
     * @param ID
     *            The classes that do the normalization come from the lucene package
     *            and are configured by the property
     *            <tt>MCR.Lucene.Analyzer.<ID>.Class</tt> in mycore.properties.
     * 
     * @return the normalized string
     */
    public static String luceneNormalize(String value, String ID) throws Exception {
      Analyzer analyzer = analyzerMap.get( ID );
      if (null == analyzer)
      {
        analyzer = (Analyzer)MCRConfiguration.instance().getInstanceOf("MCR.Lucene.Analyzer." + ID + ".Class");
        analyzerMap.put(ID, analyzer);
      }
      
      StringBuffer sb = new StringBuffer();

      TokenStream ts = analyzer.tokenStream(null, new StringReader(value));
      Token to;

      while ((to = ts.next()) != null) 
      {
        if ( sb.length() > 0)
          sb.append(" ");
        sb.append(to.termText());
      }

      return sb.toString();
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
  
}
