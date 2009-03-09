/*
 * $Revision$ 
 * $Date$
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
 */

package org.mycore.datamodel.ifs2;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.vfs.FileObject;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

/**
 * Stores XML metadata documents in a persistent filesystem structure
 * 
 * For each metadata type, a store must be defined as follows:
 * 
 * MCR.IFS2.MetadataStore.DocPortal_document.BaseDir=c:\\store
 * MCR.IFS2.MetadataStore.DocPortal_document.SlotLayout=4-2-2
 * MCR.IFS2.MetadataStore.DocPortal_document.CacheSize=100
 * 
 * @author Frank Lützenkirchen
 */
public class MCRMetadataStore extends MCRStore 
{
  /**
   * Map of defined metadata stores. Key is the document type, 
   * value is the store storing documents of that type.
   */
  private static HashMap<String,MCRMetadataStore> stores;
  
  /**
   * Reads configuration and initializes defined stores
   */
  static
  {
    stores = new HashMap<String,MCRMetadataStore>();
   
    // MCR.IFS2.MetadataStore.DocPortal_document.BaseDir=c:\\store
    // MCR.IFS2.MetadataStore.DocPortal_document.SlotLayout=4-2-2
    // MCR.IFS2.MetadataStore.DocPortal_document.CacheSize=100
    
    String prefix = "MCR.IFS2.MetadataStore.";
    MCRConfiguration config = MCRConfiguration.instance();
    Properties prop = config.getProperties( prefix );
    for( Enumeration keys = prop.keys(); keys.hasMoreElements(); )
    {
      String key = (String)(keys.nextElement());
      if( ! key.endsWith( "BaseDir" ) ) continue;
      String baseDir = prop.getProperty( key );
      String type = key.substring( prefix.length(), key.indexOf( ".BaseDir" ) );
      String slotLayout = config.getString( prefix + type + ".SlotLayout" );
      int cacheSize = config.getInt( prefix + type + ".CacheSize", 0 );
      new MCRMetadataStore( type, baseDir, slotLayout, cacheSize );
    }
  }
  
  /**
   * Returns the store storing metadata of the given´type
   * 
   * @param type the document type
   * @return the store defined for the given metadata type
   */
  public static MCRMetadataStore getStore( String type )
  { return stores.get( type ); }
  
  /**
   * A cache of retrieved metadata objects. Cache size can be configured 
   * by property MCR.IFS2.MetadataStore.<TYPE>.CacheSize, and may be 0 for 
   * disabling the cache.
   */
  protected MCRCache cache;
  
  /**
   * Creates a new metadata store instance. 
   * 
   * @param type the document type that is stored in this store
   * @param baseDir the base directory in the local filesystem storing the data
   * @param slotLayout the layout of slot subdirectories
   * @param the cache size, to disable cache use a size of 0. 
   */
  protected MCRMetadataStore( String type, String baseDir, String slotLayout, int cacheSize )
  { 
    super( type, baseDir, slotLayout, type + "_", ".xml" ); 
    stores.put( type, this );
    cache = new MCRCache( cacheSize, "Metadata cache for " + type );
  }
  
  /**
   * Stores a newly created document, using the next free ID.
   * 
   * @param xml the XML document to be stored
   * @return the ID under which the document has been stored
   */
  public int create( Document xml ) throws Exception
  {
    int id = getNextFreeID();
    create( xml, id );
    return id; 
  }

  /**
   * Stores a newly created document under the given ID.
   * 
   * @param xml the XML document to be stored
   * @param id the ID under which the document should be stored
   * @throws Exception when the given ID is already used for another stored document
   */
  public void create( Document xml, int id ) throws Exception
  {
    FileObject fo = getSlot( id );
    if( fo.exists() )
    {
      String msg = "Metadata object with ID " + id + " already exists in store";
      throw new MCRException( msg );
    }
    fo.createFile();
    write( xml, getSlot( id ) );
  }
  
  /**
   * Updates the document stored under the given ID
   * 
   * @param xml the XML document to be stored
   * @param id the ID that should be replaced
   * @throws Exception when the given ID is not used by any existing document
   */
  public void update( Document xml, int id ) throws Exception
  {
    FileObject fo = getSlot( id );
    if( ! fo.exists() )
    {
      String msg = "Metadata object with ID " + id + " does not exist in store";
      throw new MCRException( msg );
    }
    write( xml, getSlot( id ) );
  }
  
  /**
   * Writes an XML document to the given local file. The XML is written using
   * UTF-8 encoding and pretty formatting including indentation.
   * 
   * @param xml the document to be written to file
   * @param fo the file the document should be written to
   */
  protected void write( Document xml, FileObject fo ) throws Exception
  {
    OutputStream out = fo.getContent().getOutputStream();
    XMLOutputter xout = new XMLOutputter();
    xout.setFormat( Format.getPrettyFormat().setEncoding( "UTF-8" ).setIndent( "  " ) );
    xout.output( xml, out );
    out.close();
  }
  
  /**
   * Returns the XML document stored under the given ID. The result eventually comes
   * from a cache, so you must not modify it, unless you request a copy of the cached version
   * 
   * @param id the ID of the XML document
   * @param copy if true, make a copy of potentially cached version
   * @return the XML document stored under that ID, or null when there is no such document
   */
  public Document retrieve( int id, boolean copy ) throws Exception
  {
    FileObject fo = getSlot( id );
    if( ! fo.exists() ) return null;
    
    if( cache.getCapacity() == 0 )  return readXML( fo );

    Integer key = new Integer( id );
    long time = fo.getContent().getLastModifiedTime();
    Document xml = (Document)( cache.getIfUpToDate( key, time ) );
      
    if( xml == null )
    {
      xml = readXML( fo );
      cache.put( key, xml );
    }
    return ( copy ? (Document)(xml.clone()) : xml );
  }
  
  /**
   * Reads the content of the given FileObject and returns it as XML document
   * 
   * @param fo the FileObject to read
   * @return the parsed XML document
   */
  protected Document readXML( FileObject fo ) throws Exception
  {
    InputStream in = fo.getContent().getInputStream();
    Document xml = new SAXBuilder().build( in );
    in.close();
    return xml;
  }
  
  /**
   * Deletes the XML document with the given ID from the store 
   *
   * @param id the ID of the document to be deleted
   */
  public void delete( int id ) throws Exception
  {
    FileObject fo = getSlot( id );
    fo.delete();
    cache.remove( new Integer( id ) );
  }
  
  /**
   * Returns the time of last modification of the XML document
   * with the given ID.
   *  
   * @param id the ID of the stored XML document
   * @return the time that document was last modified in the store, or null when there is no such document
   */
  public Date getLastModified( int id ) throws Exception
  {
    FileObject fo = getSlot( id );
    if( ! fo.exists() ) return null;
    long time = fo.getContent().getLastModifiedTime();
    return new Date( time );
  }
}
