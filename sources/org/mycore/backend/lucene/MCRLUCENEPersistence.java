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

import java.text.*;
import java.util.*;
import java.io.*;

import org.apache.log4j.Logger;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectPersistenceInterface;
import org.mycore.datamodel.metadata.MCRTypedContent;

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
 * This class implements all methods for handling searchdata with Lucene
 * based on MCRCM8Persistence.java
 *
 * @author Harald Richter
 *
 * @version $Revision$ $Date$
 **/
public final class MCRLUCENEPersistence implements MCRObjectPersistenceInterface
{
  static final private Logger logger = Logger.getLogger( MCRLUCENEPersistence.class.getName() );
  static String indexDir = "";
  static boolean first   = true; 
  
  /** Reads properties from configuration file when class first used */
  static
  {
    MCRConfiguration config = MCRConfiguration.instance();
    
    indexDir       = config.getString( "MCR.persistence_lucene_searchindexdir" );
    logger.info( "MCR.persistence_lucene_searchindexdir: " + indexDir);
  }

private void item_setChild(int i, String s1, String s2, String s3, String s4, String s5)    
{
//   if ( 0 == i )
//       return;
//    System.out.println(i + " Child: s1:" + s1 + " s2:" + s2 + " s3:" + s3 + " s4:" + s4 + " s5:" + s5);
}


private void item_setAttribute(int i, String s1, String s2, Object s3)    
{
//    if ( 0 == i )
//        return;
//    System.out.println(i + " Attribute: s1" + s1 + " s2:" + s2 + " s3:" + s3);
    logger.debug( "Attribute: " + s1 + s2 + ":" + s3);
}

private void item_create( Document doc )    
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
/*    
    anz++;
    if ( anz > 200 )
    {
     anz = 0;   
     writer.optimize();
    } 
 */
    writer.close();
    writer = null;
   }
    catch(Exception e) { System.out.println( "error adding to lucene" ); }
}

// from configuration

/**
 * The constructor of this class.
 **/
public MCRLUCENEPersistence()
  {
  }

/**
 * The methode create an object in the data store. The index class
 * is determinated by the type of the object ID. This <b>must</b>
 * correspond with the lower case configuration name.<br>
 * As example: Document --> MCR.persistence_cm8_document
 *
 * @param mcr_tc      the typed content array
 * @param jdom        the XML stream from the object as JDOM
 * @param mcr_ts_in   the text search string
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void create(MCRTypedContent mcr_tc, org.jdom.Document jdom,
  String mcr_ts_in) throws MCRConfigurationException, MCRPersistenceException
  {
  // get root data
  MCRObjectID mcr_id = null;
  String mcr_label = null;
  int mcr_tc_counter = 0;
  for (int i=0;i<mcr_tc.getSize();i++) {
    if (mcr_tc.getNameElement(i).equals("ID")) {
      mcr_id = new MCRObjectID((String)mcr_tc.getValueElement(i)); 
      mcr_label = (String)mcr_tc.getValueElement(i+1); 
      mcr_tc_counter = i+2; }
    }
  // Read the item type name from the configuration
  StringBuffer sb = new StringBuffer("MCR.persistence_cm8_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  String itemtypename = MCRConfiguration.instance().getString(sb.toString()); 
  String itemtypeprefix = MCRConfiguration.instance().getString(sb+"_prefix");
  itemtypeprefix = "";
  String typeId = mcr_id.getTypeId().toLowerCase();
  // set up data to item
  String connection = "";
  try {
    Document luceneDoc = new Document();
    item_setAttribute( mcr_tc.getFormatElement(mcr_tc_counter-2),typeId+"/",itemtypeprefix+"ID",mcr_id.getId());
    luceneDoc.add( Field.Keyword( "ID", mcr_id.getId() ) ); 
    item_setAttribute( mcr_tc.getFormatElement(mcr_tc_counter-2),typeId+"/",itemtypeprefix+"label",mcr_label);
    logger.debug(mcr_ts_in);
    item_setAttribute(mcr_tc.getFormatElement(0),typeId+"/",itemtypeprefix+"ts",mcr_ts_in);

    String [] xmlpath = new String[MCRTypedContent.TYPE_LASTTAG+1];
    int lastpath = 0;

    // set the metadata children data
    for (int i=mcr_tc_counter;i<mcr_tc.getSize();i++) {
      // tag is 'metadata'
      if ((mcr_tc.getNameElement(i).equals("metadata")) &&
          (mcr_tc.getTypeElement(i) == MCRTypedContent.TYPE_MASTERTAG)) {
        xmlpath[MCRTypedContent.TYPE_MASTERTAG] = itemtypeprefix+"metadata";
        lastpath = MCRTypedContent.TYPE_MASTERTAG;
        item_setChild(mcr_tc.getFormatElement(i),connection,itemtypename,xmlpath[lastpath],"/",
          "/"+xmlpath[lastpath]+"/");
        item_setAttribute(mcr_tc.getFormatElement(i),typeId+"/"+xmlpath[lastpath]+"/",itemtypeprefix+"lang",
          mcr_tc.getValueElement(i+1));
        i++;
        continue; 
        }
      // tag is 'structure'
      if ((mcr_tc.getNameElement(i).equals("structure")) &&
          (mcr_tc.getTypeElement(i) == MCRTypedContent.TYPE_MASTERTAG)) {
        xmlpath[MCRTypedContent.TYPE_MASTERTAG] = itemtypeprefix+"structure";
        lastpath = MCRTypedContent.TYPE_MASTERTAG;
        item_setChild(mcr_tc.getFormatElement(i),connection,itemtypename,xmlpath[lastpath],"/",
          "/"+xmlpath[lastpath]+"/");
        continue; 
        }
      // tag is 'service'
      if ((mcr_tc.getNameElement(i).equals("service")) &&
          (mcr_tc.getTypeElement(i) == MCRTypedContent.TYPE_MASTERTAG)) {
        xmlpath[MCRTypedContent.TYPE_MASTERTAG] = itemtypeprefix+"service";
        lastpath = MCRTypedContent.TYPE_MASTERTAG;
        item_setChild(mcr_tc.getFormatElement(i),connection,itemtypename,xmlpath[lastpath],"/",
          "/"+xmlpath[lastpath]+"/");
        continue; 
        }
      // tag is 'derivate'
      if ((mcr_tc.getNameElement(i).equals("derivate")) &&
          (mcr_tc.getTypeElement(i) == MCRTypedContent.TYPE_MASTERTAG)) {
        xmlpath[MCRTypedContent.TYPE_MASTERTAG] = itemtypeprefix+"derivate";
        lastpath = MCRTypedContent.TYPE_MASTERTAG;
        item_setChild(mcr_tc.getFormatElement(i),connection,itemtypename,xmlpath[lastpath],"/",
          "/"+xmlpath[lastpath]+"/");
        continue; 
        }
      // a path element
      if (mcr_tc.getTypeElement(i) > MCRTypedContent.TYPE_MASTERTAG) {
        xmlpath[mcr_tc.getTypeElement(i)] = new String(itemtypeprefix+
          mcr_tc.getNameElement(i));
        lastpath = mcr_tc.getTypeElement(i);
        sb = new StringBuffer(64);
        sb.append('/');
        for (int j=MCRTypedContent.TYPE_MASTERTAG;j<lastpath;j++) {
          sb.append(xmlpath[j]).append('/'); }
        item_setChild(mcr_tc.getFormatElement(i),connection,xmlpath[lastpath-1],xmlpath[lastpath],
          sb.toString(),
          sb.append(xmlpath[lastpath]).append('/').toString());
        continue; 
        }
      // set an attribute or value
      sb = new StringBuffer(64);
      sb.append('/');
      String elname = xmlpath[lastpath];
      if (mcr_tc.getTypeElement(i) == MCRTypedContent.TYPE_ATTRIBUTE) {
        for (int j=MCRTypedContent.TYPE_MASTERTAG;j<lastpath+1;j++) {
          sb.append(xmlpath[j]).append('/'); }
        elname = itemtypeprefix+mcr_tc.getNameElement(i);
        }
      else {
        for (int j=MCRTypedContent.TYPE_MASTERTAG;j<lastpath;j++) {
          sb.append(xmlpath[j]).append('/'); }
        }
      Object valueobject = null;
      switch (mcr_tc.getFormatElement(i)) {
        case MCRTypedContent.FORMAT_STRING :
          valueobject = mcr_tc.getValueElement(i);
          break;
        case MCRTypedContent.FORMAT_DATE :
          GregorianCalendar cal = (GregorianCalendar)mcr_tc.getValueElement(i);
          int number = 0;
          if (cal.get(Calendar.ERA) == GregorianCalendar.AD) {
            number = (4000+cal.get(Calendar.YEAR))*10000 +
                     cal.get(Calendar.MONTH)*100 +
                     cal.get(Calendar.DAY_OF_MONTH); }
          else {
            number = (4000-cal.get(Calendar.YEAR))*10000 +
                     cal.get(Calendar.MONTH)*100 +
                     cal.get(Calendar.DAY_OF_MONTH); }
          valueobject = new Integer(number);
          // begin for debug
          Calendar calendar = (GregorianCalendar)mcr_tc.getValueElement(i);
          SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd");
          formatter.setCalendar(calendar);
          String datestamp = formatter.format(calendar.getTime());
//          logger.debug("Attribute : "+sb+"  "+elname+"  "+datestamp);
          // end debug
          break;
        case MCRTypedContent.FORMAT_LINK :
          valueobject = mcr_tc.getValueElement(i);
          elname = itemtypeprefix+"xlink"+elname;
          break;
        case MCRTypedContent.FORMAT_CLASSID :
          valueobject = mcr_tc.getValueElement(i);
          break;
        case MCRTypedContent.FORMAT_CATEGID :
          valueobject = mcr_tc.getValueElement(i);
          break;
        case MCRTypedContent.FORMAT_NUMBER :
          valueobject = mcr_tc.getValueElement(i);
          break;
        }
      String help = valueobject.toString();
      help = MCRUtils.replaceString(help, "_", "x");
      luceneDoc.add( Field.UnStored ( typeId+sb.toString()+elname, help ) );  
      item_setAttribute(mcr_tc.getFormatElement(i),typeId+sb.toString(),elname,help);
      }

    // create the item
    item_create( luceneDoc );
    logger.info("Item "+mcr_id.getId()+" was created.");
    }
  catch (Exception e) {
    throw new MCRPersistenceException(
      "Error while creating data in Lucene store.",e); }
/*  
  finally {
    MCRCM8ConnectionPool.instance().releaseConnection(connection); }
 */
  }

/**
 * The methode create a new datastore based of given configuration. It create
 * a new data table for storing MCRObjects with the same MCRObjectID type.
 *
 * @param mcr_type    the MCRObjectID type as string
 * @param mcr_conf    the configuration XML stream as JDOM tree
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public void createDataBase(String mcr_type, org.jdom.Document mcr_conf)
  throws MCRConfigurationException, MCRPersistenceException
  { /*MCRCM8ItemType.create(mcr_type,mcr_conf);*/ }

/**
 * The method deletes an object from the searchindex.
 *
 * @param mcr_id      the object id
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void delete(MCRObjectID mcr_id)
  throws MCRConfigurationException, MCRPersistenceException
  {
  String id = mcr_id.getId();
  
  try
  {
    // does directory for text index exist, if not build it
    if ( first )
    {
      first = false;
      File file = new File( indexDir );
	    
      if ( !file.exists() ) 
      {
        Analyzer analyzer = new GermanAnalyzer();
        logger.info( "Delete: The Directory doesn't exist: " + indexDir );
        IndexWriter writer2 = new IndexWriter( indexDir, analyzer, true );
        writer2.close();
      }   
    } // if ( first
    
    IndexSearcher searcher = new IndexSearcher(indexDir);

    Term  t  = new Term( "ID", id );
    Query qu = new TermQuery( t );
    logger.debug( "Delete: Searching for: " + qu.toString("") );

    Hits hits = searcher.search( qu );
      
    logger.debug( "Delete: Number of documents found : " + hits.length() );
    if ( 1 == hits.length() )
    {    
      logger.debug( "Delete: ID: " +  hits.doc(0).get("ID") );
      if ( id.equals( hits.doc(0).get("ID") ) )
      {    
        IndexReader reader = IndexReader.open( indexDir );
        reader.delete( hits.id(0) ); 
        reader.close();
      } 
      else {
        throw new MCRPersistenceException("Delete: An object with ID " + id + " does not exist."); }
    }
    else {
      throw new MCRPersistenceException("Delete: An object with ID " + id + " does not exist."); }
  }  
  catch( Exception e )
  { 
    throw new MCRPersistenceException( e.getMessage(), e ); 
  }
}

/**
 * The method updates an object in the searchindex.
 *
 * @param mcr_tc      the typed content array
 * @param mcr_ts_in   the text search string
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void update(MCRTypedContent mcr_tc, org.jdom.Document jdom,
  String mcr_ts_in) throws MCRConfigurationException, MCRPersistenceException
  {
  // get MCRObjectID
  MCRObjectID mcr_id = null;
  for (int i=0;i<mcr_tc.getSize();i++) {
    if (mcr_tc.getNameElement(i).equals("ID")) {
      mcr_id = new MCRObjectID((String)mcr_tc.getValueElement(i)); 
      }
    }
  // delete the item with the MCRObjectID
  try
  {
    delete(mcr_id);
  } 
  catch( Exception e ){}
  // create the item with the MCRObjectID
  create(mcr_tc,jdom,mcr_ts_in);
  }
}

