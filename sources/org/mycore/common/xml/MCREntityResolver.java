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

package org.mycore.common.xml;

import org.mycore.common.*;

import java.io.*;
import java.util.*;
import org.apache.log4j.*;
import org.xml.sax.*;

/**
 * Implements a SAX EntityResolver that looks for XSL schema files and DTDs
 * in the CLASSPATH.
 *
 * @author Frank Lützenkirchen
 *
 * @version $Revision$ $Date$
 **/
public class MCREntityResolver implements EntityResolver
{
  /** A cache of resolved entities as byte[] */
  private static MCRCache cache = new MCRCache( 50 );

  /** The logger */
  private static Logger logger = Logger.getLogger( MCREntityResolver.class );

  /** Implements the SAX EntityResolver interface */
  public InputSource resolveEntity( String publicId, String systemId ) 
    throws org.xml.sax.SAXException, java.io.IOException
  {
    logger.debug( "MCREntityResolver publicID = " + publicId );
    logger.debug( "MCREntityResolver systemID = " + systemId );

    if( systemId == null ) return null;

    StringTokenizer st = new StringTokenizer( systemId, "/\\" );
    String filename = st.nextToken();
    while( st.hasMoreTokens() ) filename = st.nextToken();

    logger.debug( "MCREntityResolver filename = " + filename );

    byte[] bytes = (byte[])( cache.get( filename ) );
    if( bytes != null )
    {
      logger.debug( "MCREntityResolver file " + filename + " found in cache." );
      return new InputSource( new ByteArrayInputStream( bytes ) );
    }

    InputStream in = MCREntityResolver.class.getResourceAsStream( "/" + filename );
    if( in == null )
    {
      logger.debug( "MCREntityResolver file " + filename + " not found, falling back to default behavior." );
      return null;
    }
    else
    {
      logger.debug( "MCREntityResolver file " + filename + " found as CLASSPATH resource." );
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      MCRUtils.copyStream( in, baos );
      baos.close();
      bytes = baos.toByteArray();
      cache.put( filename, bytes );
      return new InputSource( new ByteArrayInputStream( bytes ) );
    }
  }
}
