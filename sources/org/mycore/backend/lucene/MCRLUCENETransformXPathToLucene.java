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

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.queryParser.QueryParser;

import org.mycore.common.*;
import org.mycore.common.xml.*;
import org.mycore.datamodel.metadata.*;
import org.mycore.services.query.*;

import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;

/**
 * This is the implementation of the MCRQueryInterface for Lucne
 * based omn MCRXMLDBTransformXPathToeXist.java
 * @author Harald Richter
 * @version $Revision$ $Date$
 **/
public class MCRLUCENETransformXPathToLucene extends MCRQueryBase {

  static String indexDir = "";
  
  /** Reads properties from configuration file when class first used */
  static
  {
    MCRConfiguration config = MCRConfiguration.instance();
    
    indexDir       = config.getString( "MCR.persistence_lucene_searchindexdir" );
    System.out.println( "TextIndexDir: " + indexDir);
  }
  
public static final String DEFAULT_QUERY = "/*";

/**
 * The constructor.
 **/
public MCRLUCENETransformXPathToLucene() {
  super();
  //MCRXMLDBConnectionPool.instance();
  }

/**
 * This method start the Query over one object type and return the 
 * result as MCRXMLContainer.
 *
 * @param type                  the MCRObject type
 * @return                      a result list as MCRXMLContainer
 **/
protected final MCRXMLContainer startQuery( String type ) {
  MCRXMLContainer result = new MCRXMLContainer();
  // Mark all document searches
  for (int i=0;i<subqueries.size();i++) {
    if (((String)subqueries.get(i)).indexOf(XPATH_ATTRIBUTE_DOCTEXT) != -1)
      flags.set(i,Boolean.TRUE);
    }
  
  // prepare the query over the metadata
  String query = handleQueryString(type);
  logger.debug("Transformed query : "+query);
  // do it over the metadata
  try {
      MCRXMLTableManager xmltable = MCRXMLTableManager.instance();
      String objid;
      byte[] xml;
      
      if ( query.startsWith( "ID:" ) )
      {
        objid = query.substring( 3 );  
        System.out.println("xyxy" + objid );
        xml = xmltable.retrieve(type,new MCRObjectID(objid));
        result.add( "local", objid, 0, xml); 
        return result;
      }
      
      long start = System.currentTimeMillis();
      IndexSearcher searcher = new IndexSearcher(indexDir);
      Analyzer analyzer = new GermanAnalyzer();

      Query qu = QueryParser.parse(query, "", analyzer);
      logger.debug("Searching for: " + qu.toString(""));

      Hits hits = searcher.search( qu );
      long qtime = System.currentTimeMillis() - start;
      start = System.currentTimeMillis();

      logger.debug("+++++Number of documents found : " + hits.length());
      for (int i = 0; i < hits.length(); i++)
      {
        objid =  hits.doc(i).get("ID");
        logger.debug("+++++Objid: " + objid + " Type: " + type);
        xml = xmltable.retrieve(type,new MCRObjectID(objid));
        result.add( "local", objid, 0, xml); 
      }
      
      long rtime = System.currentTimeMillis() - start;
      logger.debug("query time:    " + qtime);
      logger.debug("retrieve time: " + rtime);
    }
  catch( Exception e ) {
    throw new MCRPersistenceException( e.getMessage(), e ); }
  finally {
    try {
      }
    catch( Exception e ) {
      throw new MCRPersistenceException( e.getMessage(), e ); }
    }
  // Here you can add other searches and merge the result container with
  // them from the first query.
  return result;
  }

/**
 * Handle query string for Lucene
 **/
private String handleQueryString(String type) {
  if (subqueries.size() == 0) { return DEFAULT_QUERY; }
  StringBuffer qsb = new StringBuffer(1024);
  for (int i=0;i<subqueries.size();i++) {
    if (((Boolean)flags.get(i)).booleanValue()) continue;
    qsb.append(' ').append((String)subqueries.get(i)).append(' ')
      .append((String)andor.get(i));
    flags.set(i,Boolean.TRUE);
    }
  logger.debug("Incomming condition : "+qsb.toString());
  return handleQueryStringExist(qsb.toString().trim(),type);
  }
    
/**
 * Handle query string for exist
 **/
private String handleQueryStringExist( String query, String type ) {
  query = MCRUtils.replaceString(query, "like", "&=");
  query = MCRUtils.replaceString(query, "text()", ".");
  query = MCRUtils.replaceString(query, "ts()", ".");
  query = MCRUtils.replaceString(query, " contains(", ":");
  query = MCRUtils.replaceString(query, ")", "");
  
  query = MCRUtils.replaceString(query, "@", "");
  query = MCRUtils.replaceString(query, "='", ":");
  query = MCRUtils.replaceString(query, "'", "");
  query = MCRUtils.replaceString(query, "\"", "");
  query = MCRUtils.replaceString(query, "metadata/titles/", "");
  // combine the separated queries
//  query = root+ "[" + query +"]";
  return query;
  }
}

