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

import java.io.*;
import java.util.*;
import java.security.*;
import mycore.common.*;

/**
 * This input stream is used by the MyCoRe filesystem classes to
 * read the content of a file and import it into the System. 
 * MCRContentInputStream provides the header of the file that is read 
 * (the first 64k) for content type detection purposes, counts the number 
 * of bytes read and builds an MD5 checksum String while content goes 
 * through this input stream.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRContentInputStream extends FilterInputStream
{
  /** The number of bytes that will be read for content type detection */
  protected final static int headerSize = 65536;

  /** The MD5 checksum of all bytes read through this stream */
  protected MessageDigest digest = null;

  /** The total number of bytes read so far */
  protected long length;

  /** The header of the file read */
  protected byte[] header;

  /** 
   * Constructs a new MCRContentInputStream
   *
   * @param in the InputStream to read from
   * @throws MCRConfigurationException if java classes supporting MD5 checksums are not found
   */
  public MCRContentInputStream( InputStream in )
    throws IOException
  {
    super( null );

    MCRArgumentChecker.ensureNotNull( in, "InputStream" );

    digest = buildMD5Digest();

    DigestInputStream      dis = new DigestInputStream( in, digest );
    MCRBlockingInputStream bis = new MCRBlockingInputStream( dis, headerSize );

    byte[] buffer = new byte[ headerSize ];

    bis.mark( headerSize + 1 );
    int num = bis.read( buffer );
    bis.reset();    

    header = new byte[ Math.max( 0, num ) ];
    if( num > 0 ) System.arraycopy( buffer, 0, header, 0, num ); 

    this.in = bis;
  }

  public int read()
    throws IOException
  {
    int b = super.read();
    if( b != -1 ) length++;
    return b;
  }

  public int read( byte[] buf, int off, int len )
    throws IOException
  {
    len = super.read( buf, off, len );
    if( len != -1 ) length += len;
    return len;
  }

  /**
   * Returns the first 64 k of the underlying input stream. This is used for
   * content type detection during file import into MyCoRe.
   *
   * @return the first 64 k of the input stream
   */
  public byte[] getHeader()
  { return header; }

  /** 
   * Returns the number of bytes read so far 
   *
   * @return the number of bytes read 
   */
  public long getLength()
  { return length; }

  /** 
   * Returns the MD5 message digest that has been built during reading of
   * the underlying input stream.
   *
   * @return the MD5 message digest checksum of all bytes that have been read
   */
  public byte[] getMD5()
  { return digest.digest(); }

  /**
   * Returns the MD5 checksum as a String
   *
   * @return the MD5 checksum as a String of hex digits
   */
  public String getMD5String()
  { return getMD5String( digest ); }
  
  /**
   * Given an MD5 message digest, returns the MD5 checksum as a String
   *
   * @return the MD5 checksum as a String of hex digits
   */
  public static String getMD5String( MessageDigest digest )
  {
    byte[] bytes = digest.digest();
    StringBuffer sb = new StringBuffer();
    for( int i = 0; i < bytes.length; i++ )
    {
      String sValue = "0" + Integer.toHexString( (int) bytes[ i ] );
      sb.append( sValue.substring( sValue.length() - 2 ) );
    }
    return sb.toString();
  }
  
  /**
   * Builds a MessageDigest instance for MD5 checksum computation.
   *
   * @throws MCRConfigurationException if no java classes that support MD5 algorithm could be found
   */
  public static MessageDigest buildMD5Digest()
  {
    try{ return MessageDigest.getInstance( "MD5" ); }
    catch( NoSuchAlgorithmException exc )
    {
      String msg = "Could not find java classes that support MD5 checksum algorithm";
      throw new MCRConfigurationException( msg, exc ); 
    }
  }
}
