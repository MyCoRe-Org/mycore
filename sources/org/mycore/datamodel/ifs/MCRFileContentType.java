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

package org.mycore.datamodel.ifs;

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
   * Constructs a new file content type instance. The list of known file content
   * types is defined in an XML file that is specified in the property
   * MCR.IFS.FileContentTypes.DefinitionFile, and that file is searched in the
   * CLASSPATH directories or JAR files and parsed by MCRFileContentTypeFactory.
   *
   * @see MCRFileContentTypeFactory
   *
   * @param ID the unique content type ID
   * @param label the label of this content type
   * @param url the url where information like a plug-in can be found
   * @param mimeType the MIME type used for this content type
   */
  MCRFileContentType( String ID, String label, String url, String mimeType )
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
   * Returns the MIME type used to deliver this file type to a client browser.
   * If no MIME type is set, the default type "application/octet-stream" for
   * binary content is returned.
   *
   * @return the MIME type used to deliver this file type to a client browser
   */  
  public String getMimeType() 
  { return ( mimeType != null ? mimeType : "application/octet-stream" ); }

  /**
   * Returns the URL where additional information like a plug-in download page can be found
   * @return the URL of additional information, or null
   */  
  public String getURL() 
  { return this.url; }

  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    sb.append( "ID    = " ).append( this.getID()       ).append( "\n" );
    sb.append( "label = " ).append( this.getLabel()    ).append( "\n" );
    sb.append( "mime  = " ).append( this.getMimeType() ).append( "\n" );
    sb.append( "url   = " ).append( this.getURL()      );
    return sb.toString();
  }
}
