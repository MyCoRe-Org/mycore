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

import java.util.HashSet;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.query.MCRMetaSearchInterface;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.queryParser.QueryParser;

import org.apache.lucene.search.BooleanQuery;

/**
 * This is the implementation of the MCRMetaSearchInterface with Lucene
 *
 * @author Harald Richter
 * @version $Revision$ $Date$
 **/
public class MCRLUCENETransformXPathToLucene implements MCRMetaSearchInterface {

/** The default query **/
public static final String DEFAULT_QUERY = "/*";

// the logger
protected static Logger logger =
  Logger.getLogger(MCRLUCENETransformXPathToLucene.class.getName());
private MCRConfiguration config = null;

static String indexDir = "";

/**
 * The constructor.
 **/
  public MCRLUCENETransformXPathToLucene()
  {
    config = MCRConfiguration.instance();
    indexDir = config.getString("MCR.persistence_lucene_searchindexdir");
    logger.debug("TextIndexDir: " + indexDir);
  }

  /**
   * This method starts the Query over the Lucene persistence layer for one
   * object type and and returns the query result as a HashSet of MCRObjectIDs.
   * 
   * @param root
   *          the query root
   * @param query
   *          the metadata queries
   * @param type
   *          the MCRObject type
   * @return a result list as MCRXMLContainer
   */
	public final HashSet getResultIDs(String root, String query, String type)
  {
    // prepare the query over the rest of the metadata
    HashSet idmeta = new HashSet();
    logger.debug("Incomming condition : " + query);
    String newquery = "";
    if ((root == null) && (query.length() == 0))
    {
      newquery = DEFAULT_QUERY;
    }
    newquery = handleQueryStringExist(root, query, type);
    logger.debug("Transformed query for Lucene: " + newquery);

    // do it over the metadata
    if (newquery.length() != 0)
    {
      try
      {
        long start = System.currentTimeMillis();
        IndexSearcher searcher = new IndexSearcher(indexDir);
        Analyzer analyzer = new GermanAnalyzer();

        BooleanQuery bq = new BooleanQuery();
        Query qu = QueryParser.parse(newquery, "", analyzer);
        bq.add(qu, true, false);
        
        Term  te = new Term("typeId", type);
        
        TermQuery tq = new TermQuery(te);
        bq.add(tq, true, false);
        
        logger.debug("Searching for: " + bq.toString(""));

        Hits hits = searcher.search(bq);
        long qtime = System.currentTimeMillis() - start;
        start = System.currentTimeMillis();

        logger.debug("+++++Number of objects found : " + hits.length());
        for (int i = 0; i < hits.length(); i++)
        {
          String objid = hits.doc(i).get("key");
          logger.debug("+++++Objid: " + objid + " Type: " + type);
          idmeta.add(new MCRObjectID(objid));
        }

        logger.debug("query time:    " + qtime);
      } catch (Exception e)
      {
        throw new MCRPersistenceException(e.getMessage(), e);
      }
    }
    return idmeta;
  }

/**
 * Handle query string for Lucene
 **/
private String handleQueryStringExist(String root, String query, String type) {
	query = MCRUtils.replaceString(query, "#####metadata/titles/title contains(\"", "+(title:");
	query = MCRUtils.replaceString(query, "\")#####", ")");
	
	/*
	query = MCRUtils.replaceString(query, "like", "&=");
	query = MCRUtils.replaceString(query, "text()", ".");
	query = MCRUtils.replaceString(query, "ts()", ".");
	query = MCRUtils.replaceString(query, "contains(", "&=");
	query = MCRUtils.replaceString(query, "contains (", "&=");
	query = MCRUtils.replaceString(query, ")", "");
	*/
	return query;
}
  
}
