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

import org.mycore.common.*;
import org.mycore.datamodel.ifs.*;
import java.text.*;
import java.util.*;
import java.io.*;
import javax.xml.transform.*;
import javax.xml.transform.sax.TransformerHandler;

import org.apache.log4j.Logger;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.*;

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

/**
 * This class implements all methods for handling searchdata of xml-files with Lucene
 *
 * @author Harald Richter
 *
 * @version $Revision$ $Date$
 **/
public class MCRContentIndexerXML extends MCRContentIndexer
{
  /** Reads properties from configuration file when class first used */
  static MCRConfiguration config = MCRConfiguration.instance();

  static final private Logger logger = Logger.getLogger( MCRContentIndexerXML.class.getName() );
  
  String indexDir = "";
  boolean first   = true; 
  Hashtable search = new Hashtable();
  
  /**
   * The constructor of this class.
   **/
  public MCRContentIndexerXML()
  {
  }
  public void init( String indexerID, Hashtable attribute )
  { 
    indexDir         = (String)attribute.get( "dir" );
    logger.info( "Index directory for indexer " + indexerID + ": " + indexDir);
    String indexFile = (String)attribute.get( "index" );
    logger.info( "Index file for indexer " + indexerID + ": " + indexFile);
    try
    {
      InputStream in = this.getClass().getResourceAsStream( "/" + indexFile );
      if( in == null )
      {
        String msg = "Index file for indexer " + indexFile + " not found in CLASSPATH";
        throw new MCRConfigurationException( msg );
      }
    
      org.jdom.input.SAXBuilder builder = new org.jdom.input.SAXBuilder();
      org.jdom.Document jdom            = builder.build( in );
      org.jdom.Element root             = jdom.getRootElement();
      List types                        = root.getChildren( "type" );
      for( int i = 0; i < types.size(); i++ )
      {
        org.jdom.Element xType = (org.jdom.Element)( types.get( i ) );
        String sea             = xType.getAttributeValue( "search" );
        String name            = xType.getAttributeValue( "name" );
        String fieldType       = xType.getAttributeValue( "fieldType" );
        if ( null != sea && null != name && null != fieldType )
          if ( sea.equals( "true" ) )
          {
            logger.debug("Search: " + sea + " Name: " + name + " fieldType:" + fieldType );
            search.put( name, fieldType );
          }
      }
      logger.debug("Search size: " + search.size() );
    }
    catch (Exception e)
    {
      logger.info(e);
    }
  }

/**
 * Adds document to Lucene
 * @param doc lucene document to add to index
 * 
 **/
private void addDocumentToLucene( Document doc )    
{
 try
 {
    IndexWriter writer = null;
    Analyzer analyzer = new GermanAnalyzer();
        
    // does directory for text index exist, if not build it
    if ( first )
    {
      first = false;
      File file = new File( indexDir );
	    
      if ( !file.exists() ) 
      {
        logger.info( "The Directory doesn't exist: " + indexDir + " try to build it" );
        IndexWriter writer2 = new IndexWriter( indexDir, analyzer, true );
        writer2.close();
      }   
    } // if ( first

    if ( null == writer)
    {  
      writer = new IndexWriter( indexDir, analyzer, false );
      writer.mergeFactor  = 200;
      writer.maxMergeDocs = 2000;
    }  
    
    writer.addDocument( doc );
    writer.close();
    writer = null;
   }
    catch(Exception e) { logger.info( "error adding to lucene" ); }
}

/**
 * Delete document in Lucene
 * @param id string document id
 * @param type string name of textindex
 * 
 **/
  private void deleteDocument( String id, String type ) throws Exception {
      
    IndexSearcher searcher = new IndexSearcher(indexDir);
    Analyzer analyzer = new GermanAnalyzer();

    Query qu = QueryParser.parse("key:"+id, "", analyzer);
    logger.info("Searching for: " + qu.toString(""));

    Hits hits = searcher.search( qu );
      
    logger.info("Number of documents found : " + hits.length());
    if ( 1 == hits.length() )
    {    
      logger.info(" id: " + hits.id(0) 
      + " score: " + hits.score(0) + " key: " +  hits.doc(0).get("key") );
      if ( id.equals( hits.doc(0).get("key") ) )
      {    
       IndexReader reader = IndexReader.open( indexDir );
       reader.delete( hits.id(0) ); 
       reader.close();
       logger.info("DELETE: " + id);
      } 
    }  

  }

  /**
   * Builds an index of the content of an MCRFile object
   *
   * @param file the MCRFile thats content is to be indexed
   *
   **/
  protected void doIndexContent( MCRFile file )
    throws MCRException
  {
    logger.info( "++++ doIndexContent: " + file.getID() + " Store: " + file.getStoreID( ) +
                          " ContentTypeID: " + file.getContentTypeID() );
    List list = trans( file ); 
    if ( null != list)
    {
      Document doc = buildLuceneDocument( file.getID(), list );
      if ( null != doc )
        addDocumentToLucene( doc );
    }
  }

  /**
   * Deletes the index of an MCRFile object
   *
   * @param the MCRFile object
   */
  protected void doDeleteIndex( MCRFile file )
    throws MCRException
  {
    logger.info( "++++ doDeleteIndex: " + file.getID() + " Store: " + file.getStoreID( ) +
                          " ContentTypeID: " + file.getContentTypeID() );
    try
    {
      deleteDocument( file.getID(), "String type" );
    }
    catch( Exception e){ logger.info( "error deleting from lucene" ); } 
  }
  
/**
 * Transforms xml data with stylesheet mcr_make_types.xsl 
 * @param file the MCRFile to be transformed
 * 
 * @return list of childen (typed content)
 * 
 **/
private List trans( MCRFile file )    
{
  try
  {
  ByteArrayOutputStream out = new ByteArrayOutputStream();
  MCRXSLTransformation transformer = MCRXSLTransformation.getInstance();
  java.net.URL url      = MCRContentIndexerXML.class.getResource( "/mcr_make_types.xsl" );
  Templates xsl = transformer.getStylesheet( url.getFile() );
  TransformerHandler handler = transformer.getTransformerHandler( xsl );
  transformer.transform( file.getContentAsJDOM(), handler, out );
  out.close();
  byte[] output = out.toByteArray();
  org.jdom.input.SAXBuilder builder = new org.jdom.input.SAXBuilder();
  org.jdom.Document jdom = builder.build( new ByteArrayInputStream( output ) );
  return jdom.getRootElement().getChildren( "type" );
  }   
  catch (Exception e)
  {
    logger.info(e);
    return null;
  }
}

/**
 * Build lucene document from transformed xml list 
 * @param key MCRFile ID           
 * @param types xml data as list (typed content)
 * 
 * @return The lucene document
 * 
 **/
private Document buildLuceneDocument( String key, List types )    
{
  Document doc = new Document();
  doc.add( Field.Keyword( "key", key ) );
  
  for( int i = 0; i < types.size(); i++ )
  {
    org.jdom.Element xType = (org.jdom.Element)( types.get( i ) );
    String name      = xType.getAttributeValue( "name" );
    String value     = xType.getAttributeValue( "value" );
    if ( null != name && null != value )
    {
      if ( search.containsKey( name ) )
      {
        String field = (String)search.get( name );
        logger.debug( "Name: " + name + " Value: " + value + " Field: " + field );
        if ( field.equals( "Keyword" ) )
          doc.add( Field.Keyword( name, value ) );
        if ( field.equals( "Text" ) )
          doc.add( Field.Text( name, value ) );
        if ( field.equals( "UnStored" ) )
          doc.add( Field.UnStored( name, value ) );
      }
    }
  }
  return doc;
}

  /**
   * Search in Index with query
   *
   * @param the query
   *
   * @return the hits of the query (ifs IDs)
   *
   */
  public String[] doSearchIndex( String query )
    throws MCRException
  {
    String result[] = null; 
    try
    {
      IndexSearcher searcher = new IndexSearcher(indexDir);
      Analyzer analyzer = new GermanAnalyzer();

      Query qu = QueryParser.parse( query, "", analyzer);
      logger.info("Searching for: " + qu.toString(""));

      Hits hits = searcher.search( qu );
      
      int anz = hits.length();
      result = new String[anz];
      logger.info("Number of documents found : " + hits.length());
      for (int i=0; i<anz; i++)
      {    
        result[i] = hits.doc(0).get("key");
//        logger.info(" id: " + hits.id(0) + " score: " + hits.score(0) + " key: " +  hits.doc(0).get("key") );
      }  
      return result;

    }
    catch( Exception e){ logger.info( "error search with lucene: " + query); } 
    return result;
  }
 
}

