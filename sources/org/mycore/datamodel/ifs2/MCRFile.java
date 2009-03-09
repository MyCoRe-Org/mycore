/*
 * $Revision: 13085 $ 
 * $Date: 2008-02-06 18:27:24 +0100 (Mi, 06 Feb 2008) $
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.RandomAccessContent;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.provider.local.LocalFile;
import org.apache.commons.vfs.util.RandomAccessMode;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRUtils;

/**
 * Represents a file stored in a file collection. This is a file that is imported
 * from outside the system, and may be updated and modified afterwards.
 * 
 * @author Frank Lützenkirchen
 *
 */
public class MCRFile extends MCRStoredNode
{
  /**
   * The md5 checksum of the file's contents.
   */
  protected String md5;
  
  /**
   * Returns a MCRFile object representing an existing file already stored in the
   * store.
   * 
   * @param parent the parent directory containing this file
   * @param fo the file in the local underlying filesystem storing this file
   */
  protected MCRFile( MCRDirectory parent, FileObject fo ) throws Exception
  { 
    super( parent, fo ); 
    parent.readChildData( this );
  }

  /**
   * Creates a new MCRFile and stores it in the underlying local filesystem.
   * 
   * @param parent the parent directory containing this file
   * @param name the name of the file to be created and stored
   */
  public MCRFile( MCRDirectory parent, String name ) throws Exception
  { 
    super( parent, VFS.getManager().resolveFile( parent.fo, name ) );
    md5 = "d41d8cd98f00b204e9800998ecf8427e"; // md5 of empty file
    fo.createFile();
    updateMetadata();
  }

  /**
   * Returns a MCRVirtualNode contained in this file as a child. A file that is
   * a container, like zip or tar, may contain other files as children.
   */
  protected MCRVirtualNode buildChildNode( FileObject fo ) throws Exception
  { return new MCRVirtualNode( this, fo ); }
  
  /**
   * Writes all metadata of this file to the given XML element
   */
  protected void writeChildData( Element entry ) throws Exception
  {
    super.writeChildData( entry );
    entry.setAttribute( "md5", this.getMD5() );
    entry.setAttribute( "size", String.valueOf( this.getSize() ) );
  }
  
  /**
   * Reads metadata of this file from the given XML element and stores it in this
   * object itself.
   */
  protected void readChildData( Element entry ) throws Exception
  { md5 = entry.getAttributeValue( "md5" ); }
  
  /**
   * Repairs the metadata of this file by rebuilding it from the underlying local
   * filesystem. This includes recreation of the md5 checksum. Stores the metadata
   * rebuilt in the parent directory. 
   */
  public void repairMetadata() throws Exception
  {
    InputStream src = getContentInputStream();
    MCRContentInputStream cis = new MCRContentInputStream( src );
    MCRUtils.copyStream( cis, null );
    src.close();
    md5 = cis.getMD5String();
    updateMetadata();
  }

  /**
   * Deletes this file and its content from the store. This object is illegal 
   * afterwards and must not be used any more.
   */
  public void delete() throws Exception
  {
    super.delete();
    fo.delete(); 
  }
  
  /**
   * Sets last modification time of this file to a custom value.
   * 
   * @param time the time to be stored as last modification time
   */
  public void setLastModified( long time ) throws Exception
  { 
    fo.getContent().setLastModifiedTime( time );
    updateMetadata();
  }

  /**
   * Returns the md5 checksum of the file's content.
   * 
   * @return the md5 checksum of the file's content.
   */
  public String getMD5()
  { return md5; } 

  /**
   * Returns the file name extension, which is the part after the last
   * dot in the filename.
   * 
   * @return the file extension, or the empty string if the file name does not have an extension
   */
  public String getExtension()
  {
    String name = this.getName();
    int pos = name.lastIndexOf( "." );
    return( pos == -1 ? "" : name.substring( pos + 1 ) );
  }

  /**
   * Sets the content of this file by reading it from the given uri,
   * which may be a file or http url for example.
   * 
   * @param uri the location of the file's content to be read 
   * @return the MD5 checksum of the stored content
   */
  public String setContentFrom( String uri ) throws Exception 
  {
    FileObject src = VFS.getManager().resolveFile( uri );
    InputStream in = src.getContent().getInputStream();
    String md5 = setContentFrom( in );
    in.close();
    return md5;
  }

  /**
   * Sets the content of this file by reading it from a file in the
   * local filesystem.

   * @param source the local file to read bytes from
   * @return the MD5 checksum of the stored content
   */
  public String setContentFrom( File source ) throws Exception 
  { 
    InputStream in = new FileInputStream( source ); 
    String md5 = setContentFrom( in );
    in.close();
    return md5;
  }

  /**
   * Sets the content of this file from an XML document. The XML is stored
   * using UTF-8 encoding and pretty formatting with indentation.
   * 
   * @param xml the XML to store as content of this file
   * @return the MD5 checksum of the stored content
   */
  public String setContentFrom( Document xml ) throws Exception 
  {
    OutputStream out = fo.getContent().getOutputStream();
    MessageDigest digest = MCRContentInputStream.buildMD5Digest();
    DigestOutputStream dos = new DigestOutputStream( out, digest );
    
    XMLOutputter xout = new XMLOutputter();
    xout.setFormat( Format.getPrettyFormat().setEncoding( "UTF-8" ).setIndent( "  " ) );
    xout.output( xml, dos );

    dos.close();
    md5 = MCRContentInputStream.getMD5String( digest );
    updateMetadata();
    return md5; 
  }

  /**
   * Sets the content of this file by reading bytes from the given InputStream.
   * 
   * @param source the InputStream to read from
   * @return the MD5 checksum of the stored content
   */
  public String setContentFrom( InputStream source ) throws Exception 
  {
    MCRContentInputStream cis = new MCRContentInputStream( source );
    OutputStream out = fo.getContent().getOutputStream();
    MCRUtils.copyStream( cis, out );
    out.close();
    md5 = cis.getMD5String();
    updateMetadata();
    return md5;
  }

  /**
   * Returns an InputStream to read this file's content from. Be sure to close
   * the stream after usage!
   * 
   * @return an InputStream on the file content
   */
  public InputStream getContentInputStream() throws Exception 
  { return fo.getContent().getInputStream(); }

  /**
   * Writes the content of this file to the given OutputStream. The OutputStream
   * is not closed afterwards, this is responsibility of the caller!
   * 
   * @param out the OutputStream to write to.
   */
  public void getContentTo( OutputStream out ) throws Exception 
  { 
    InputStream in = fo.getContent().getInputStream();
    MCRUtils.copyStream( in, out );
    in.close();
  }

  /**
   * Writes the content of this file to a file in the local filesystem.
   *
   * @param target the file to write content to
   */
  public void getContentTo( File target ) throws Exception 
  {
    OutputStream out = new FileOutputStream( target );
    getContentTo( out );
    out.close();
  }

  /**
   * Returns the file content as XML document, assuming the file 
   * contains valid XML.
   * 
   * @return the XML document stored as file content
   */
  public Document getContentAsXML() throws Exception 
  { 
    InputStream in = getContentInputStream();
    Document xml = new SAXBuilder().build( in );
    in.close();
    return xml;
  }
  
  /**
   * Returns the local java.io.File representing this stored file.
   * Be careful to use this only for reading data, do never modify directly!
   * 
   * @return the file in the local filesystem representing this file
   */
  public File getLocalFile() throws Exception
  {
    if( fo instanceof LocalFile )
      return new File( fo.getURL().getPath() );
    else
      return null;  
  }
  
  /**
   * Returns the content of this file for random access read. Be sure not to
   * write to the file using the returned object, use just for reading!
   * 
   * @return the content of this file, for random access
   */
  public RandomAccessContent getRandomAccessContent() throws Exception
  {
    return fo.getContent().getRandomAccessContent( RandomAccessMode.READ );  
  }
}
