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

package org.mycore.frontend.cli;

import java.io.*;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.mycore.common.*;
import org.mycore.datamodel.metadata.*;
import org.mycore.datamodel.classifications.MCRClassification;

/**
 * Provides static methods that implement commands for the
 * MyCoRe command line interface for classifications.
 *
 * @author Jens Kupferschmidt
 * @author Frank L�tzenkirchen
 * @version $Revision$ $Date$
 **/

public class MCRClassificationCommands
  {
  private static Logger logger = 
    Logger.getLogger(MCRClassificationCommands.class.getName());
  private static String SLASH = System.getProperty( "file.separator" );
  

 /**
  * Initialize common data.
  **/
  private static void init()
    {
    MCRConfiguration config = MCRConfiguration.instance();
    PropertyConfigurator.configure(config.getLoggingProperties());
    }

 /**
  * Deletes an MCRClassification from the datastore.
  * 
  * @param ID the ID of the MCRClassification that should be deleted
  **/
  public static void delete( String ID )
    throws Exception
    {
    init();
    MCRObjectID mcr_id = new MCRObjectID(ID);
    MCRClassification cl = new MCRClassification();
    try {
      cl.delete( mcr_id.getId() );
      logger.info( mcr_id.getId() + " deleted." );
      }
    catch ( MCRException ex ) {
      logger.debug( ex.getStackTraceAsString() );
      logger.error( ex.getMessage() );
      logger.error( "Can't deltete " + mcr_id.getId() + "." );
      logger.error( "" );
      }
    }

 /**
  * Loads MCRClassification from all XML files in a directory.
  *
  * @param directory the directory containing the XML files
  **/
  public static void loadFromDirectory( String directory )
    { processFromDirectory( directory, false ); }

 /**
  * Updates MCRClassification from all XML files in a directory.
  *
  * @param directory the directory containing the XML files
  **/
  public static void updateFromDirectory( String directory )
    { processFromDirectory( directory, true ); }

 /**
  * Loads or updates MCRClassification from all XML files in a directory.
  * 
  * @param directory the directory containing the XML files
  * @param update if true, classification will be updated, else Classification
  * is created
  **/
  private static void processFromDirectory( String directory, boolean update )
    {
    init();
    File dir = new File( directory );
    if( ! dir.isDirectory() ) {
      logger.warn( directory + " ignored, is not a directory." );
      return;
      }
    String[] list = dir.list();
    if( list.length == 0) {
      logger.warn( "No files found in directory " + directory );
      return;
      }
    int numProcessed = 0;
    for( int i = 0; i < list.length; i++ ) {
      if ( ! list[ i ].endsWith(".xml") ) { continue; }
      if( processFromFile( directory + SLASH + list[ i ], update ) )
	    numProcessed++;
      }
    logger.info( "Processed " + numProcessed + " files." );
    }

 /**
  * Loads an MCRClassification from an XML file.
  *
  * @param filename the location of the xml file
  **/
  public static boolean loadFromFile( String file )
    { return processFromFile( file, false ); }

 /**
  * Updates an MCRClassification from an XML file.
  *
  * @param filename the location of the xml file
  **/
  public static boolean updateFromFile( String file )
    { return processFromFile( file, true ); }

 /**
  * Loads or updates an MCRClassification from an XML file.
  *
  * @param filename the location of the xml file
  * @param update if true, classification will be updated, else classification
  * is created
  **/
  private static boolean processFromFile( String file, boolean update )
    {
    init();
    if( ! file.endsWith( ".xml" ) ) {
      logger.warn( file + " ignored, does not end with *.xml" );
      return false;
      }

    if( ! new File( file ).isFile() ) {
      logger.warn( file + " ignored, is not a file." );
      return false;
      }
    logger.info( "Reading file " + file + " ...\n" );
    try {
      MCRClassification cl = new MCRClassification();
      if( update ) {
        String id = cl.updateFromURI(file);
        logger.info( id + " updated.\n" );
        }
      else {
        String id = cl.createFromURI(file);
        logger.info( id + " loaded.\n" );
        }
      return true;
      }
    catch( MCRException ex ) {
      logger.error( "Exception while loading from file " + file, ex);
      return false;
    }
  }

 /**
  * Save an MCRClassification.
  *
  * @param ID the ID of the MCRClassification to be save.
  * @param filename the filename to store the classification
  **/
  public static void save( String ID, String filename )
  {
    init();
    MCRObjectID mcr_id = new MCRObjectID(ID);
    MCRClassification cl = new MCRClassification();
    byte[] xml = cl.receiveClassificationAsXML(mcr_id.getId());
    try {
      FileOutputStream out = new FileOutputStream(filename);
      out.write(xml);
      out.flush();
      }
    catch (IOException ex) {
      logger.error( ex.getMessage() );
      logger.error( "Exception while store to file " + filename );
      logger.error("");
      return;
      }
    logger.info( "Classification "+mcr_id.getId()+" stored under "
      +filename+"." );
    logger.info("");
  }

}
