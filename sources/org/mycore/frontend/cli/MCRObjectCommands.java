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

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.*;
import org.mycore.common.xml.*;
import org.mycore.datamodel.metadata.*;

/**
 * Provides static methods that implement commands for the
 * MyCoRe command line interface.
 *
 * @author Jens Kupferschmidt
 * @author Frank L�tzenkirchen
 * @version $Revision$ $Date$
 **/

public class MCRObjectCommands
{
  private static String SLASH = System.getProperty( "file.separator" );
  private static Logger logger =
    Logger.getLogger(MCRClassificationCommands.class.getName());

 /**
  * Initialize common data.
  **/
  private static void init()
    {
    MCRConfiguration config = MCRConfiguration.instance();
    PropertyConfigurator.configure(config.getLoggingProperties());
    }

 /**
  * Deletes an MCRObject from the datastore.
  * 
  * @param ID the ID of the MCRObject that should be deleted
  **/
  public static void delete( String ID )
    throws Exception
    {
    init();
    MCRObject mycore_obj = new MCRObject();
    try {
      mycore_obj.deleteFromDatastore( ID );
      logger.info( mycore_obj.getId().getId() + " deleted." );
      }
    catch ( MCRException ex ) {
      logger.debug( ex.getStackTraceAsString() );
      logger.error( ex.getMessage() );
      logger.error( "Can't deltete " + mycore_obj.getId().getId() + "." );
      logger.error( "" );
      }
    }

 /**
  * Delete MCRObjects form ID to ID from the datastore.
  *
  * @param IDfrom the start ID for deleting the MCRObjects
  * @param IDto   the stop ID for deleting the MCRObjects
  **/
  public static void deleteFromTo( String IDfrom, String IDto )
    throws Exception
    {
    init();
    int from_i = 0;
    int to_i = 0;
    try {
      MCRObjectID from = new MCRObjectID(IDfrom);
      MCRObjectID to = new MCRObjectID(IDto);
      MCRObjectID now = new MCRObjectID(IDfrom);
      from_i = from.getNumberAsInteger();
      to_i = to.getNumberAsInteger();
      if (from_i > to_i) {
        throw new MCRException( "The from-to-interval is false." ); }
      for (int i=from_i;i<to_i+1;i++) {
        now.setNumber(i); delete(now.getId()); }
      }
    catch ( MCRException ex ) {
      logger.debug( ex.getStackTraceAsString() );
      logger.error( ex.getMessage() );
      logger.error( "" );
      }
    }

 /**
  * Loads MCRObjects from all XML files in a directory.
  *
  * @param directory the directory containing the XML files
  **/
  public static void loadFromDirectory( String directory )
    { processFromDirectory( directory, false ); }

 /**
  * Updates MCRObjects from all XML files in a directory.
  *
  * @param directory the directory containing the XML files
  **/
  public static void updateFromDirectory( String directory )
    { processFromDirectory( directory, true ); }

 /**
  * Loads or updates MCRObjects from all XML files in a directory.
  * 
  * @param directory the directory containing the XML files
  * @param update if true, object will be updated, else object is created
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
	if ( ! list[ i ].endsWith(".xml") ) continue;
	if( processFromFile( directory + SLASH + list[ i ], update ) )
	    numProcessed++;
      }
    logger.info( "Processed " + numProcessed + " files." );
    }

 /**
  * Loads an MCRObjects from an XML file.
  *
  * @param filename the location of the xml file
  **/
  public static boolean loadFromFile( String file )
    { return processFromFile( file, false ); }

 /**
  * Updates an MCRObjects from an XML file.
  *
  * @param filename the location of the xml file
  **/
  public static boolean updateFromFile( String file )
    { return processFromFile( file, true ); }

 /**
  * Loads or updates an MCRObjects from an XML file.
  *
  * @param filename the location of the xml file
  * @param update if true, object will be updated, else object is created
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
    logger.info( "Reading file " + file + " ..." );
    try {
      MCRObject mycore_obj = new MCRObject();
      mycore_obj.setFromURI( file );
      logger.info( "Label --> " + mycore_obj.getLabel() );
      if( update ) {
        mycore_obj.updateInDatastore();
        logger.info( mycore_obj.getId().getId() + " updated." );
        logger.info("");
        }
      else {
        mycore_obj.createInDatastore();
        logger.info( mycore_obj.getId().getId() + " loaded." );
        logger.info("");
        }
      return true;
      }
    catch( MCRException ex ) {
      logger.debug( ex.getStackTraceAsString() );
      logger.error( ex.getMessage() );
      logger.error( "Exception while loading from file " + file );
      logger.error("");
      return false;
    }
  }

 /**
  * Shows the next free MCRObjectIDs.
  */
  public static void showNextID( String base )
    { 
    MCRObjectID mcr_id = new MCRObjectID();
    try {
      mcr_id.setNextFreeId( base );
      logger.info("The next free ID  is "+mcr_id.getId());
      }
    catch (MCRException ex) {
      logger.error( ex.getMessage() );
      logger.error("");
      }
    }

 /**
  * Shows the last used MCRObjectIDs.
  */
  public static void showLastID( String base )
    { 
    MCRObjectID mcr_id = new MCRObjectID();
    try {
      mcr_id.setNextFreeId( base );
      mcr_id.setNumber(mcr_id.getNumberAsInteger()-1);
      logger.info("The last used ID  is "+mcr_id.getId());
      }
    catch (MCRException ex) {
      logger.error( ex.getMessage() );
      logger.error("");
      }
    }

 /**
  * Save an MCRObject.
  *
  * @param ID the ID of the MCRObject to be save.
  * @param filename the filename to store the object
  **/
  public static void save( String ID, String filename )
    {
    MCRObject obj = new MCRObject();
    byte[] xml = obj.receiveXMLFromDatastore(ID);
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
    logger.info( "Object "+ID+" stored under "+filename+"." );
    logger.info( "" );
    }

 /**
  * Get the next free MCRObjectID for the given MCRObjectID base.
  *
  * @param base the MCRObjectID base string
  **/
  public static void getNextID (String base)
    {
    MCRObjectID id = new MCRObjectID();
    try {
      id.setNextFreeId(base);
      logger.info(id.getId());
      }
    catch (MCRException ex) {
      logger.error( ex.getMessage() );
      logger.error("");
      }
    }

 /**
  * Get the last used MCRObjectID for the given MCRObjectID base.
  *
  * @param base the MCRObjectID base string
  **/
  public static void getLastID (String base)
    {
    MCRObjectID mcr_id = new MCRObjectID();
    try {
      mcr_id.setNextFreeId(base);
      mcr_id.setNumber(mcr_id.getNumberAsInteger()-1);
      logger.info(mcr_id.getId());
      }
    catch (MCRException ex) {
      logger.error( ex.getMessage() );
      logger.error("");
      }
    }

 /**
  * The method parse and check an XML file.
  *
  * @param filename the location of the xml file
  **/
  public static boolean checkXMLFile( String file )
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
    logger.info( "Reading file " + file + " ..." );
    if (MCRXMLHelper.parseURI(file)!=null)
    	logger.info( "The file has no XML errors." );
    return true;
    }
  }
