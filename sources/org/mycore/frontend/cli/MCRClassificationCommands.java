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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications.MCRClassification;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Provides static methods that implement commands for the
 * MyCoRe command line interface for classifications.
 *
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 **/

public class MCRClassificationCommands extends MCRAbstractCommands
  {

  /** The logger */
  private static Logger LOGGER = Logger.getLogger(MCRClassificationCommands.class.getName());

  /**
   * The empty constructor.
   */
  public MCRClassificationCommands()
  {
    super();
    MCRCommand com = null;

    com = new MCRCommand("delete classification {0}",
      "org.mycore.frontend.cli.MCRClassificationCommands.delete String",
      "The command remove the classification with MCRObjectID {0} from the system."
      );
    command.add(com);

    com = new MCRCommand("load classification from file {0}",
      "org.mycore.frontend.cli.MCRClassificationCommands.loadFromFile String",
      "The command add a new classification form file {0} to the system."
      );
    command.add(com);

    com = new MCRCommand("update classification from file {0}",
      "org.mycore.frontend.cli.MCRClassificationCommands.updateFromFile String",
      "The command update a classification form file {0} in the system."
      );
    command.add(com);

    com = new MCRCommand("load all classifications from directory {0}",
      "org.mycore.frontend.cli.MCRClassificationCommands.loadFromDirectory String",
      "The command add all classifications in the directory {0} to the system."
      );
    command.add(com);

    com = new MCRCommand("update all classifications from directory {0}",
      "org.mycore.frontend.cli.MCRClassificationCommands.updateFromDirectory String",
      "The command update all classifications in the directory {0} to the system."
      );
    command.add(com);

    com = new MCRCommand("save classification {0} to {1}",
      "org.mycore.frontend.cli.MCRClassificationCommands.save String String",
      "The command store the classification with MCRObjectID {0} to the file with name {1}."
      );
    command.add(com);

  }

 /**
  * Deletes an MCRClassification from the datastore.
  * 
  * @param ID the ID of the MCRClassification that should be deleted
  **/
  public static void delete( String ID )
    throws Exception
    {
    MCRObjectID mcr_id = new MCRObjectID(ID);
    MCRClassification cl = new MCRClassification();
    try {
      cl.delete( mcr_id.getId() );
      LOGGER.info( mcr_id.getId() + " deleted." );
      }
    catch ( MCRException ex ) {
      LOGGER.debug( ex.getStackTraceAsString() );
      LOGGER.error( ex.getMessage() );
      LOGGER.error( "Can't deltete " + mcr_id.getId() + "." );
      LOGGER.error( "" );
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
    File dir = new File( directory );
    if( ! dir.isDirectory() ) {
      LOGGER.warn( directory + " ignored, is not a directory." );
      return;
      }
    String[] list = dir.list();
    if( list.length == 0) {
      LOGGER.warn( "No files found in directory " + directory );
      return;
      }
    int numProcessed = 0;
    for( int i = 0; i < list.length; i++ ) {
      if ( ! list[ i ].endsWith(".xml") ) { continue; }
      if( processFromFile( directory + SLASH + list[ i ], update ) )
	    numProcessed++;
      }
    LOGGER.info( "Processed " + numProcessed + " files." );
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
    if( ! file.endsWith( ".xml" ) ) {
      LOGGER.warn( file + " ignored, does not end with *.xml" );
      return false;
      }

    if( ! new File( file ).isFile() ) {
      LOGGER.warn( file + " ignored, is not a file." );
      return false;
      }
    LOGGER.info( "Reading file " + file + " ...\n" );
    try {
      MCRClassification cl = new MCRClassification();
      if( update ) {
        String id = cl.updateFromURI(file);
        LOGGER.info( id + " updated.\n" );
        }
      else {
        String id = cl.createFromURI(file);
        LOGGER.info( id + " loaded.\n" );
        }
      return true;
      }
    catch( MCRException ex ) {
      LOGGER.error( "Exception while loading from file " + file, ex);
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
    MCRObjectID mcr_id = new MCRObjectID(ID);
    MCRClassification cl = new MCRClassification();
    byte[] xml = cl.receiveClassificationAsXML(mcr_id.getId());
    try {
      FileOutputStream out = new FileOutputStream(filename);
      out.write(xml);
      out.flush();
      }
    catch (IOException ex) {
      LOGGER.error( ex.getMessage() );
      LOGGER.error( "Exception while store to file " + filename );
      LOGGER.error("");
      return;
      }
    LOGGER.info( "Classification "+mcr_id.getId()+" stored under "
      +filename+"." );
    LOGGER.info("");
  }

}
