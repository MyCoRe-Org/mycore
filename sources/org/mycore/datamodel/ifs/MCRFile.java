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

package mycore.ifs;

import mycore.common.*;
import java.util.*;
import java.io.*;
import java.security.*;
import javax.servlet.http.*;

/**
 * Represents a stored file with its metadata and content.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRFile extends MCRFilesystemNode
{
  /** The ID of the store that holds this file's content */
  protected String storeID;  
  
  /** The ID that identifies the place where the store holds the content */
  protected String storageID;  

  /** The ID of the content type of this file */
  protected String contentTypeID;
  
  /** The md5 checksum that was built when content was read for this file */
  protected String md5;
  
  /** The optional extender for streaming audio/video files */
  protected MCRAudioVideoExtender avExtender;

  public MCRFile( String name, String ownerID )
  {
    super( name, ownerID );
    initContentFields();
    storeNew();
  }
  
  public MCRFile( String name, MCRDirectory parent )
  { 
    super( name, parent );
    initContentFields();
    storeNew();
  }
  
  MCRFile( String ID, String parentID, String ownerID, String name, long size, GregorianCalendar date, String storeID, String storageID, String fctID, String md5 )
  {
    super( ID, parentID, ownerID, name, size, date );
    
    this.storageID     = storageID;
    this.storeID       = storeID;
    this.contentTypeID = fctID;
    this.md5           = md5;
  }
  
  private void initContentFields()
  {
    storageID     = "";
    storeID       = "";
    contentTypeID = MCRFileContentTypeFactory.getDefaultType().getID();
    md5           = "d41d8cd98f00b204e9800998ecf8427e"; // md5 of empty file
    size          = 0;
    avExtender    = null;
  }
  
  /**
   * Returns the file extension of this file, or an empty string if
   * the file has no extension
   **/
  public String getExtension()
  {
    ensureNotDeleted();

    if( name.endsWith( "." ) ) return "";
    
    int pos = name.lastIndexOf( "." );
    return( pos == -1 ? "" : name.substring( pos + 1 ) );
  }

  /**
   * Returns the MD5 checksum for this file
   **/
  public String getMD5()
  { 
    ensureNotDeleted();
    return md5; 
  }

  /**
   * Returns the ID of the MCRContentStore implementation that holds the
   * content of this file
   **/
  public String getStoreID()
  { 
    ensureNotDeleted();
    return storeID; 
  }
  
  /**
   * Returns the storage ID that identifies the place where the MCRContentStore 
   * has stored the content of this file
   **/
  public String getStorageID()
  { 
    ensureNotDeleted();
    return storageID; 
  }
  
  /**
   * Returns the MCRContentStore instance that holds the content of this file
   **/
  protected MCRContentStore getContentStore()
  {
    if( storeID.length() == 0 )
      return null;
    else
      return MCRContentStoreFactory.getStore( storeID );
  }

  /**
   * Reads the content of this file from a java.lang.String and
   * stores its text as bytes, encoded in the default encoding of the
   * platform where this is running.
   **/
  public void setContentFrom( String source )
    throws MCRPersistenceException, IOException
  { 
    MCRArgumentChecker.ensureNotNull( source, "source string" );
    byte[] bytes = source.getBytes();
    
    setContentFrom( bytes );
  }
  
  /**
   * Reads the content of this file from a java.lang.String and
   * stores its text as bytes, encoded in the encoding given, 
   * in an MCRContentStore.
   **/
  public void setContentFrom( String source, String encoding )
    throws MCRPersistenceException, IOException, UnsupportedEncodingException
  { 
    MCRArgumentChecker.ensureNotNull( source, "source string"          );
    MCRArgumentChecker.ensureNotNull( source, "source string encoding" );
    byte[] bytes = source.getBytes( encoding );
    
    setContentFrom( bytes );
  }
  
  /**
   * Reads the content of this file from a source file in the local
   * filesystem and stores it in an MCRContentStore.
   **/
  public void setContentFrom( File source )
    throws MCRPersistenceException, IOException
  { 
    MCRArgumentChecker.ensureNotNull( source, "source file" );
    MCRArgumentChecker.ensureIsTrue( source.exists(),  
      "source file does not exist:" + source.getPath() );
    MCRArgumentChecker.ensureIsTrue( source.canRead(), 
      "source file not readable:" + source.getPath() );
    
    setContentFrom( new FileInputStream( source ) );
  }
  
  /**
   * Reads the content of this file from a byte array and
   * stores it in an MCRContentStore.
   **/
  public void setContentFrom( byte[] source )
    throws MCRPersistenceException, IOException
  { 
    MCRArgumentChecker.ensureNotNull( source, "source byte array" );
    
    setContentFrom( new ByteArrayInputStream( source ) );
  }
  
  /**
   * Reads the content of this file from the source InputStream and
   * stores it in an MCRContentStore.
   **/
  public void setContentFrom( InputStream source )
    throws MCRPersistenceException, IOException
  { 
    ensureNotDeleted();
    MCRArgumentChecker.ensureNotNull( source, "source input stream" );
    
    String          old_md5       = this.md5;
    long            old_size      = this.size;
    String          old_storageID = this.storageID;
    MCRContentStore old_store     = getContentStore(); 
    
    initContentFields();
    
    MCRContentInputStream cis = new MCRContentInputStream( source );
    byte[] header = cis.getHeader();
    
    contentTypeID = 
      MCRFileContentTypeFactory.detectType( this.getName(), header ).getID();

    if( header.length > 0 ) // Do not store empty file content
    {
      MCRContentStore store = MCRContentStoreFactory.selectStore( this );
      
      storageID = store.storeContent( this, cis ); 
      storeID   = store.getID();
    }
    
    size = cis.getLength();
    md5  = cis.getMD5String();
    
    boolean changed = ( ( size != old_size ) || ( ! md5.equals( old_md5 ) ) );
    if( changed ) lastModified = new GregorianCalendar();
    
    manager.storeNode( this );
    
    if( changed && hasParent() ) getParent().sizeOfChildChanged( old_size, size );
    
    if( old_storageID.length() != 0 ) old_store.deleteContent( old_storageID );
  }

  /**
   * Deletes this file and its content stored in the system
   **/
  public void delete()
    throws MCRPersistenceException
  {
    ensureNotDeleted();
    
    if( storageID.length() != 0 ) getContentStore().deleteContent( storageID );
    super.delete();
    
    this.contentTypeID = null;
    this.md5           = null;
    this.storageID     = null;
    this.storeID       = null;
    this.avExtender    = null;
  }

  /**
   * Writes the content of this file to a target output stream
   **/
  public void getContentTo( OutputStream target )
    throws MCRPersistenceException
  { 
    ensureNotDeleted();
    
    if( storageID.length() != 0 ) 
    {
      MessageDigest digest = MCRContentInputStream.buildMD5Digest();

      DigestOutputStream dos = new DigestOutputStream( target, digest );
      getContentStore().retrieveContent( this, target ); 
      
      String md5_new = MCRContentInputStream.getMD5String( digest );
      if( ! this.md5.equals( md5_new ) )
      {
        String msg = "MD5 Checksum failure while retrieving file content";
        throw new MCRPersistenceException( msg );
      }
    }
  }
  
  /**
   * Writes the content of this file to a file on the local filesystem
   **/
  public void getContentTo( File target )
    throws MCRPersistenceException, IOException
  { getContentTo( new FileOutputStream( target ) ); }

  /**
   * Gets the content of this file as a byte array
   **/
  public byte[] getContentAsByteArray()
    throws MCRPersistenceException
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try
    {
      getContentTo( baos );
      baos.close();
    }
    catch( IOException willNotBeThrown ){}
    return baos.toByteArray();
  }
  
  /**
   * Gets the content of this file as a string, using the default encoding
   * of the system environment
   **/
  public String getContentAsString()
    throws MCRPersistenceException
  { return new String( getContentAsByteArray() ); }
  
  /**
   * Gets the content of this file as a string, using the given encoding
   **/
  public String getContentAsString( String encoding )
    throws MCRPersistenceException, UnsupportedEncodingException
  { return new String( getContentAsByteArray(), encoding ); }
  
  /**
   * Returns true, if this file is stored in a content store that provides
   * an MCRAudioVideoExtender for audio/video streaming and additional metadata
   **/
  public boolean hasAudioVideoExtender()
  {
    ensureNotDeleted();

    if( storeID.length() == 0 ) 
      return false;
    else
      return MCRContentStoreFactory.providesAudioVideoExtender( storeID );
  }
  
  /**
   * Returns the AudioVideoExtender in case this file is streaming audio/video
   * and stored in a ContentStore that supports this
   **/
  public MCRAudioVideoExtender getAudioVideoExtender()
  {
    ensureNotDeleted();

    if( hasAudioVideoExtender() && ( avExtender == null ) )  
      avExtender = MCRContentStoreFactory.buildExtender( this );

    return avExtender;
  }

  /**
   * Gets the ID of the content type of this file
   **/
  public String getContentTypeID()
  { 
    ensureNotDeleted();
    return contentTypeID; 
  }
  
  /**
   * Gets the content type of this file
   **/
  public MCRFileContentType getContentType()
  { 
    ensureNotDeleted();
    return MCRFileContentTypeFactory.getType( contentTypeID ); 
  }
  
  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    sb.append( super.toString() );
    sb.append( "ContentType = " ).append( this.contentTypeID ).append( "\n" );
    sb.append( "MD5         = " ).append( this.md5           ).append( "\n" );
    sb.append( "StoreID     = " ).append( this.storeID       ).append( "\n" );
    sb.append( "StorageID   = " ).append( this.storageID     );
    return sb.toString();
  }
}
