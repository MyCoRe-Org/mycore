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

package org.mycore.datamodel.ifs;

import org.mycore.common.*;
import org.mycore.common.xml.*;
import org.jdom.*;
import org.jdom.input.*;
import java.util.*;
import java.io.*;

/**
 * This class manages instances of MCRContentIndexer
 * and provides methods to get these for a given Indexer ID or MCRFile instance.
 * The class is responsible for looking up, loading, instantiating and
 * remembering the implementations of MCRContentIndexer
 * that are used in the system.
 *
 * @author Harald Richter
 * @version $Revision$ $Date$
 */
public class MCRContentIndexerFactory
{
  /** Hashtable IndexerID to MCRContentIndexer instance */  
  protected static Hashtable indexers = new Hashtable();

  /** The MCRContentIndexerDetector implementation that will be used */
  protected static MCRContentIndexerDetector indexerDetector;
  
  static
  {
    MCRConfiguration config = MCRConfiguration.instance();
    
    String file = config.getString( "MCR.IFS.FileContentHandler.DefinitionFile", "FileContentHandler.xml" );
        
    InputStream in = MCRFileContentType.class.getResourceAsStream( "/" + file );
    if( in == null )
    {
      String msg = "Configuration file " + file + " not found in CLASSPATH";
      throw new MCRConfigurationException( msg );
    }
    
    try
    {
      SAXBuilder builder = new SAXBuilder();
      builder.setEntityResolver( new MCREntityResolver() );

      Document xml = builder.build( in );
      // TODO: Validate and provide a DTD/Schema file
      
      List types = xml.getRootElement().getChildren( "fcttype" );
      for( int i = 0; i < types.size(); i++ )              // handle all fcttypes
      {
        Element xType    = (Element)( types.get( i ) );
        String fcttype   = xType.getAttributeValue( "type" );
      
        List handler = xType.getChildren( "handler" );
        for( int j = 0; j < handler.size(); j++ )          // handle all handlers
        {
          org.jdom.Element xAttribute = (org.jdom.Element)handler.get(j);
          List attribute = xAttribute.getChildren( "attribute" );
          for( int k = 0; k < attribute.size(); k++ )      // handle all attributes
          {
            org.jdom.Element xValue = (org.jdom.Element)attribute.get(k);
            System.out.println("FCTTYPE: " + fcttype + " " + 
                                xAttribute.getAttributeValue( "ID" ) + " " + 
                                xAttribute.getAttributeValue( "type" ) + " " +
                                xValue.getTextTrim() );
          }
        } 
        
      }
      
    }
    catch( Exception exc )
    {
      String msg = "Error processing list of defined file content handlers";
      throw new MCRConfigurationException( msg, exc );
    }
  }
  
  /**
   * Returns the MCRContentIndexer instance that is configured for this
   * IndexerID. The instance that is returned is configured by the property
   * <tt>MCR.IFS.ContentIndexer.<IndexerID>.Class</tt> in mycore.properties.
   * 
   * @param indexerID the non-null ID of the MCRContentIndexer implementation
   * @return the MCRContentIndexer instance that uses this indexerID
   */
  public static MCRContentIndexer getIndexer( String indexerID )
  {
    MCRArgumentChecker.ensureNotEmpty( indexerID, "Indexer ID" );
    if( ! indexers.containsKey( indexerID ) )
    {
      try
      { 
        Class cl   = Class.forName( "org.mycore.backend.lucene.MCRContentIndexerXML" );
        Object obj = cl.newInstance();
//        String indexerClass = "MCR.IFS.ContentIndexer." + indexerID + ".Class";
//        Object obj = MCRConfiguration.instance().getInstanceOf( indexerClass );
        MCRContentIndexer s = (MCRContentIndexer)( obj );
        Hashtable attribute = new Hashtable();
        attribute.put( "dir", "d:/_indexer" );
        attribute.put( "index", "bibentry-index.xml" );
        s.init( indexerID, attribute );
        indexers.put( indexerID, s );
      }
      catch( Exception ex )
      { 
        String msg = "Could not load MCRContentIndexer with indexer ID = " + indexerID;
        throw new MCRConfigurationException( msg, ex ); 
      }
    }
    return (MCRContentIndexer)( indexers.get( indexerID ) );
  }

  /**
   * Returns the MCRContentIndexer instance that should be used
   * to indexer the content of the given file. The configured
   * MCRContentIndexerDetector is used to make this decision.
   *
   * @see MCRContentIndexerDetector
   * @see MCRContentIndexer
   **/
  public static MCRContentIndexer getIndexerFromFCT( String fct )
  {
    if( indexerDetector == null )
    {
      String property = "MCR.IFS.ContentIndexerDetector.Class";
      Object obj = MCRConfiguration.instance().getInstanceOf( property );
      indexerDetector = (MCRContentIndexerDetector)obj;
    }
    String indexerID = indexerDetector.getIndexer( fct );
    if ( null != indexerID)
    {
      System.out.println("++++ Indexer gefunden: " + indexerID );
      return getIndexer( indexerID );
    }
    else
      return null;
  }
  
}
