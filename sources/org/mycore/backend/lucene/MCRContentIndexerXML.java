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
  static final private Logger logger = Logger.getLogger( MCRContentIndexerXML.class.getName() );
  static String indexDir = "";
  static boolean first   = true; 
  static Hashtable search = new Hashtable();
  
  /** Reads properties from configuration file when class first used */
  static
  {
    MCRConfiguration config = MCRConfiguration.instance();
    
    indexDir       = config.getString( "MCR.IFS.ContentIndexer.tablename.indexdir" );
    logger.info( "MCR.IFS.ContentIndexer.tablename.indexdir: " + indexDir);
try
{
    org.jdom.input.SAXBuilder builder = new org.jdom.input.SAXBuilder();
    org.jdom.Document jdom = builder.build(new FileInputStream( "D:/Lernen/xsl/typed/bibentry-index.xml" ) );
    org.jdom.Element root = jdom.getRootElement();
    List types = root.getChildren( "type" );
    for( int i = 0; i < types.size(); i++ )
    {
      // Build file content type from XML element
      org.jdom.Element xType = (org.jdom.Element)( types.get( i ) );
      String sea       = xType.getAttributeValue( "search" );
      String name      = xType.getAttributeValue( "name" );
      String fieldType = xType.getAttributeValue( "fieldType" );
      if ( null != sea && null != name && null != fieldType )
        if ( sea.equals( "true" ) )
        {
          System.out.println("Search: " + sea + " Name: " + name + " fieldType:" + fieldType );
          search.put( name, fieldType );
        }
    }
    System.out.println("Search size: " + search.size() );
  }
  catch (Exception e)
  {
    System.out.println(e);
  }
  }

  /**
   * The constructor of this class.
   **/
  public MCRContentIndexerXML()
  {
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
        logger.info( "The Directory doesn't exist: " + indexDir );
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
    catch(Exception e) { System.out.println( "error adding to lucene" ); }
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
    System.out.println("Searching for: " + qu.toString(""));

    Hits hits = searcher.search( qu );
      
    System.out.println("Number of documents found : " + hits.length());
    if ( 1 == hits.length() )
    {    
//      System.out.println("NAME: " + hits.doc(0).get("path") + " id: " + hits.id(0) 
      System.out.println(" id: " + hits.id(0) 
      + " score: " + hits.score(0) + " key: " +  hits.doc(0).get("key") );
      if ( id.equals( hits.doc(0).get("key") ) )
      {    
       IndexReader reader = IndexReader.open( indexDir );
       reader.delete( hits.id(0) ); 
       reader.close();
       System.out.println("DELETE: " + id);
      } 
    }  

  }

  /**
   * Builds an index of the content of an MCRFile by reading from an MCRContentInputStream.
   *
   * @param file the MCRFile thats content is to be indexed
   * @param source the ContentInputStream where the file content is read from
   **/
  protected void doIndexContent( MCRFile file )
    throws MCRException
  {
    System.out.println( "++++ doIndexContent: " + file.getID() + " Store: " + file.getStoreID( ) +
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
   * Deletes the index of an MCRFile object that is indexed under the given
   * Storage ID in this indexer instance.
   *
   * @param storageID the storage ID of the MCRFile object
   */
  protected void doDeleteIndex( MCRFile file )
    throws MCRException
  {
    System.out.println( "++++ doDeleteIndex: " + file.getID() + " Store: " + file.getStoreID( ) +
                          " ContentTypeID: " + file.getContentTypeID() );
    try
    {
      deleteDocument( file.getID(), "String type" );
    }
    catch( Exception e){ System.out.println( "error deleting from lucene" ); } 
  }
  
/**
 * Transforms xml data with stylesheet mcr_make_types.xsl 
 * @param header contains xml data ( size is limited to 64 k!)
 * 
 **/
private List trans( MCRFile file )    
{
  try
  {
  ByteArrayOutputStream out = new ByteArrayOutputStream();
  MCRXSLTransformation transformer = MCRXSLTransformation.getInstance();
  Templates xsl = transformer.getStylesheet( "D:/wd_sample/mycore-sample-application/stylesheets/mcr_make_types.xsl" );
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
    System.out.println(e);
    return null;
  }
}

/**
 * Build lucene document from transformed xml list 
 * @param key MCRFile ID           
 * @param types xml data as list
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
        System.out.println( "Name: " + name + " Value: " + value + " Field: " + field );
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
}

