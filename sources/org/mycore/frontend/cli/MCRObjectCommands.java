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

package mycore.commandline;

import java.io.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import mycore.common.*;
import mycore.datamodel.*;

/**
 * Provides static methods that implement commands for the
 * MyCoRe command line interface.
 *
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 **/

public class MCRObjectCommands
{
  private static String SLASH = System.getProperty( "file.separator" );

 /**
  * Deletes an MCRObject from the datastore.
  * 
  * @param ID the ID of the MCRObject that should be deleted
  **/
  public static void delete( String ID )
    throws Exception
  {
    MCRObject mycore_obj = new MCRObject();
    mycore_obj.deleteFromDatastore( ID );
    System.out.println( mycore_obj.getId().getId() + " deleted." );
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
    File dir = new File( directory );

    if( ! dir.isDirectory() )
    {
      System.out.println( directory + " ignored, is not a directory." );
      return;
    }

    String[] list = dir.list();

    if( list.length == 0)
    {
      System.out.println( "No files found in directory " + directory );
      return;
    }

    int numProcessed = 0;
    for( int i = 0; i < list.length; i++ ) 
      {
      if ( ! list[ i ].endsWith(".xml") ) continue;
      if( processFromFile( directory + SLASH + list[ i ], update ) )
        numProcessed++;
      }

    System.out.println( "Processed " + numProcessed + " files." );
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
    if( ! file.endsWith( ".xml" ) )
    {
      System.out.println( file + " ignored, does not end with *.xml" );
      return false;
    }

    if( ! new File( file ).isFile() )
    {
      System.out.println( file + " ignored, is not a file." );
      return false;
    }

    System.out.println( "Reading file " + file + " ...\n" );

    try
    {
      MCRObject mycore_obj = new MCRObject();
      mycore_obj.setFromURI( file );
      System.out.println( "Label --> " + mycore_obj.getLabel() );

      if( update )
      {
        mycore_obj.updateInDatastore();
        System.out.println( mycore_obj.getId().getId() + " updated.\n" );
      }
      else
      {
        mycore_obj.createInDatastore();
        System.out.println( mycore_obj.getId().getId() + " loaded.\n" );
      }
      return true;
    }
    catch( Exception ex )
    {
      System.out.println( ex );
      System.out.println();
      System.out.println( "Exception while loading from file " + file );
      return false;
    }
  }

 /**
  * Shows an MCRObjects.
  *
  * @param ID the ID of the MCRObject to be shown.
  **/
  public static void show( String ID )
  {
    MCRObject mycore_obj = new MCRObject();
    mycore_obj.receiveFromDatastore( ID );
    mycore_obj.debug();
    MCRObjectStructure st = mycore_obj.getStructure();
    if (st != null) { st.debug(); }
    MCRMetaElement me = mycore_obj.getMetadataElement( "titles" );
    if (me != null) {me.debug(); }
    MCRObjectService se = mycore_obj.getService();
    if (se != null) { se.debug(); }
  }

 /**
  * Create a new data base file for the MCRObjectId type.
  *
  * @param mcr_type the MCRObjectId type
  * @return true if all is okay, else false
  **/
  public static boolean createDataBase ( String mcr_type )
    {
    MCRConfiguration conf = MCRConfiguration.instance();
    // Application pathes
    String mycore_appl = "";
    try {
      mycore_appl = conf.getString("MCR.appl_path"); }
    catch ( Exception e ) {
      throw new MCRException("Can't find configuration for MCR.appl_path." ); }
    String SLASH = System.getProperty("file.separator");
    // Read config file
    String conf_home = mycore_appl+SLASH+"config";
    String conf_filename = "";
    try {
      conf_filename = conf.getString("MCR.persistence_config_"+mcr_type); }
    catch ( Exception e ) {
      throw new MCRException("Can't find configuration for "+mcr_type ); }
    if(!conf_filename.endsWith(".xml")) {
      throw new MCRException("Configuration "+mcr_type+" ends not with .xml"); }
    File conf_file = new File(conf_home,conf_filename);
    if(! conf_file.isFile()) {
      throw new MCRException("Can't read configuration for "+mcr_type ); }
    System.out.println( "Reading file " + conf_filename + " ...\n" );
    org.jdom.Document confdoc = null;
    try {
      SAXBuilder builder = new SAXBuilder();
      confdoc = builder.build(conf_file); }
    catch ( Exception e ) {
      throw new MCRException("Can't parse configuration for "+mcr_type ); }
    // create the database
    MCRObject obj = new MCRObject();
    obj.createDataBase(mcr_type,confdoc);
    return true;
    }

 /**
  * Create a new XML Schema file for the MCRObjectId type.
  *
  * @param mcr_type the MCRObjectId type
  * @return true if all is okay, else false
  **/
  public static boolean createXMLSchema ( String mcr_type )
    {
    MCRConfiguration conf = MCRConfiguration.instance();
    // Root pathes
    String mycore_root = "";
    try {
      mycore_root = conf.getString("MCR.root_path"); }
    catch ( Exception e ) {
      throw new MCRException("Can't find configuration for MCR.root_path." ); }
    String mycore_appl = "";
    try {
      mycore_appl = conf.getString("MCR.appl_path"); }
    catch ( Exception e ) {
      throw new MCRException("Can't find configuration for MCR.appl_path." ); }
    String SLASH = System.getProperty("file.separator");
    // Read config file
    String conf_home = mycore_appl+SLASH+"config";
    String conf_filename = "";
    try {
      conf_filename = conf.getString("MCR.persistence_config_"+mcr_type); }
    catch ( Exception e ) {
      throw new MCRException("Can't find configuration for "+mcr_type ); }
    if(!conf_filename.endsWith(".xml")) {
      throw new MCRException("Configuration "+mcr_type+" ends not with .xml"); }
    File conf_file = new File(conf_home,conf_filename);
    if(! conf_file.isFile()) {
      throw new MCRException("Can't read configuration for "+mcr_type ); }
    System.out.println( "Reading file " + conf_filename + " ...\n" );
    // Set schema file
    String schema_home = mycore_appl+SLASH+"schema";
    String schema_filename = conf_filename.substring(0,conf_filename.length()-4)
      +".xsd";
    // Read transformer file
    String xslt_home = mycore_root+SLASH+"stylesheets";
    String xslt_filename = "MCRObjectSchema.xsl";
    File xslt_file = new File(xslt_home,xslt_filename);
    if(! xslt_file.isFile()) {
      throw new MCRException("Can't read schema from MCRSchema.xsl"); }
    System.out.println( "Reading file " + xslt_filename + " ...\n" );
    // Transform
    try {
    TransformerFactory transfakt = TransformerFactory.newInstance();
    Transformer trans = transfakt.newTransformer(new StreamSource(xslt_file));
    trans.setParameter("mycore_home",mycore_root);
    trans.setParameter("mycore_appl",mycore_appl);
    trans.transform(new StreamSource(conf_file),new StreamResult(schema_home+
      System.getProperty("file.separator")+schema_filename));
    System.out.println( "Write file " + schema_filename + " ...\n" ); }
    catch ( Exception e ) {
      throw new MCRException(e.getMessage()); }
    return true;
    }

 /**
  * Shows a list of next MCRObjectIDs.
  */
  public static void getid( String base )
  { 
    MCRObjectID mcr_id = new MCRObjectID();
    mcr_id.setNextId( base );
    mcr_id.debug();
  }
}
