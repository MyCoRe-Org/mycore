/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  *** 
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package mycore.ifs;

import mycore.common.*;
import org.jdom.*;
import org.jdom.input.*;
import java.util.*;
import java.io.*;

/**
 * Instances of this class represent information about the content type
 * of a file.
 *
 * @author Frank Lützenkirchen
 */
public class MCRFileContentType
{
  /** The unique ID of this file content type */  
  protected String ID;
  /** The label of this file content type */  
  protected String label;
  /** The URL where information such as a plug-in download page can be found */  
  protected String url;
  /** The MIME type used to deliver this file type to a client browser */  
  protected String mimeType;
  
  /** 
   * Constructs a new file content type instance.
   *
   * @param ID the unique content type ID
   * @param label the label of this content type
   * @param url the url where information like a plug-in can be found
   * @param mimeType the MIME type used for this content type
   */
  protected MCRFileContentType( String ID, String label, String url, String mimeType )
  {
    this.ID = ID;
    this.label = label;
    this.url = url;
    this.mimeType = mimeType;
  }
    
  /** 
   * Returns the unique ID of this file content type
   * @return the unique ID of this file content type
   */  
  public String getID() 
  { return this.ID; }

  /** 
   * Returns the label of this file content type
   * @return the label of this file content type
   *
   */  
  public String getLabel() 
  { return this.label; }

  /**
   * Returns the MIME type used to deliver this file type to a client browser
   * @return the MIME type used to deliver this file type to a client browser
   */  
  public String getMimeType() 
  { return this.mimeType; }

  /**
   * Returns the URL where additional information like a plug-in download page can be found
   * @return the URL of additional information, or null
   */  
  public String getURL() 
  { return this.url; }

  /** 
   * Table for looking up all file content types by ID
   */  
  protected static Hashtable typesTable = new Hashtable();
 
  /**
   * Read the file content types from the file FileContentTypes.xml
   * somewhere in the CLASSPATH directories or JAR files and 
   * instantiates the objects.
   *
   * @throws MCRConfigurationException if the file FileContentTypes.xml can not be found or parsed
   */
  static
  {
    InputStream in = MCRFileContentType.class.getResourceAsStream( "/FileContentTypes.xml" );
    if( in == null )
    {
      String msg = "Configuration file FileContentTypes.xml not found in CLASSPATH";
      throw new MCRConfigurationException( msg );
    }
    
    try
    {
      Document xml = new SAXBuilder().build( in );
      // TODO: Validate and provide a DTD/Schema file
    
      List types = xml.getRootElement().getChildren( "type" );
      for( int i = 0; i < types.size(); i++ )
      {
        Element xType = (Element)( types.get( i ) );
        String ID    = xType.getAttributeValue( "ID" );
        String label = xType.getChildTextTrim( "label" );
        String url   = xType.getChildTextTrim( "url"   );
        String mime  = xType.getChildTextTrim( "mime"  );
      
        MCRFileContentType type = new MCRFileContentType( ID, label, url, mime );
        typesTable.put( ID, type );
      }
    }
    catch( Exception exc )
    {
      String msg = "Error processing file FileContentTypes.xml";
      throw new MCRConfigurationException( msg, exc );
    }
  }
  
  /** 
   * Returns the file content type with the given ID
   *
   * @param ID The non-null ID of the content type that should be returned
   * @return The file content type with the given ID
   * @throws MCRConfigurationException if no such file content type is known in the system
   */  
  public static MCRFileContentType getType( String ID )
  {
    MCRArgumentChecker.ensureNotEmpty( ID, "ID" );
    if( typesTable.containsKey( ID ) )
      return (MCRFileContentType)( typesTable.get( ID ) );
    else
    {
      String msg = "There is no file content type with ID = " + ID + " configured";
      throw new MCRConfigurationException( msg );
    }
  }
  
  // TODO: Regeln -> FCT bestimmen (extensions, rules, points)
  // TODO: FCT -> Store (und andere Bestimmungsregeln)
}
