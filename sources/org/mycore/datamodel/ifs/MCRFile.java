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
  }
  
  public MCRFile( String name, MCRDirectory parent )
  { 
    super( name, parent );
    initContentFields();
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
    String md5_old = this.md5;
    long size_old = this.size;
    
    if( storageID.length() != 0 ) // delete old content of file
    {
      getContentStore().deleteContent( this );
      initContentFields();
    }  
    
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

    if( ( size != size_old ) || ( ! md5.equals( md5_old ) ) )
    {
      lastModified = new GregorianCalendar();
      if( parent != null ) parent.sizeOfChildChanged( size_old, size );
    }
  }

  /**
   * Deletes this file and its content stored in the system
   **/
  public void delete()
    throws MCRPersistenceException
  {
    ensureNotDeleted();
    
    if( storageID.length() != 0 ) getContentStore().deleteContent( this );
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
}
