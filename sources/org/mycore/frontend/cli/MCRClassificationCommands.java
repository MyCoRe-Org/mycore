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
import mycore.classifications.MCRClassification;

/**
 * Provides static methods that implement commands for the
 * MyCoRe command line interface for classifications.
 *
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 **/

public class MCRClassificationCommands
{
  private static String SLASH = System.getProperty( "file.separator" );

 /**
  * Deletes an MCRClassification from the datastore.
  * 
  * @param ID the ID of the MCRClassification that should be deleted
  **/
  public static void delete( String ID )
    throws Exception
  {
    MCRClassification cl = new MCRClassification();
    cl.delete( ID );
    System.out.println( ID + " deleted." );
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
    for( int i = 0; i < list.length; i++ ) {
	if ( ! list[ i ].endsWith(".xml") ) continue;
	if( processFromFile( directory + SLASH + list[ i ], update ) )
	    numProcessed++;
    }

    System.out.println( "Processed " + numProcessed + " files." );
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
    MCRClassification cl = new MCRClassification();
      if( update )
      {
        String id = cl.updateFromURI(file);
        System.out.println( id + " updated.\n" );
      }
      else
      {
        String id = cl.createFromURI(file);
        System.out.println( id + " loaded.\n" );
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
  * Save an MCRClassification.
  *
  * @param ID the ID of the MCRClassification to be save.
  * @param filename the filename to store the classification
  **/
  public static void save( String ID, String filename )
  {
    MCRClassification cl = new MCRClassification();
    byte[] xml = cl.receiveClassificationAsXML(ID);
    try {
      FileOutputStream out = new FileOutputStream(filename);
      out.write(xml);
      out.flush();
      }
    catch (IOException ex) {
      System.out.println( ex );
      System.out.println();
      System.out.println( "Exception while store to file " + filename );
      }
    System.out.println( "Classification "+ID+" stored under"+filename+".\n" );
  }

}
