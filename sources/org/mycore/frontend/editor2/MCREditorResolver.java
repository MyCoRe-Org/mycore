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

package org.mycore.frontend.editor2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.StringTokenizer;
import javax.servlet.ServletContext;
import org.jdom.Element;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRConfiguration;
import org.mycore.frontend.servlets.MCRServlet;

class MCREditorResolver
{
  protected static ServletContext context;
  protected static String base;

  protected static MCRCache fileCache;
  
  static void init( ServletContext ctx, String webAppBase )
  {
    context = ctx;
    base    = webAppBase;
    
    MCRConfiguration config = MCRConfiguration.instance();
    String prefix = "MCR.Editor.";
    int cacheSize = config.getInt( prefix + "StaticFiles.CacheSize", 100 );
    fileCache = new MCRCache( cacheSize );
  }
  
  protected static Element readXML( String uri )
  {
    MCREditorServlet.logger.info( "Editor reading xml from uri " + uri );

    String scheme = new StringTokenizer( uri, ":" ).nextToken();
    
    if( "resource".equals( scheme ) )
      return readFromResource( uri );
    else if( "webapp".equals( scheme ) )
      return readFromWebapp( uri );
    else if( "file".equals( scheme ) )
      return readFromFile( uri );
    else if( "http".equals( scheme ) || "https".equals( scheme ) )
      return readFromHTTP( uri );
    else if( "request".equals( scheme ) )
      return readFromRequest( uri );
    else
    {
      String msg = "Unsupported URI type: " + uri;
      throw new MCRUsageException( msg );
    }
  }
  
  // resource:path
  protected static Element readFromResource( String uri )
  {
    String path = uri.substring( uri.indexOf( ":" ) + 1 );

    MCREditorServlet.logger.debug( "Editor reading xml from classpath resource " + path );

    return parseStream( MCREditorResolver.class.getResourceAsStream( path ) );
  }

  // webapp:path
  protected static Element readFromWebapp( String uri )
  {
    String path = uri.substring( uri.indexOf( ":" ) + 1 );
    MCREditorServlet.logger.info( "Reading from webapp " + path );
    uri = "file://" + context.getRealPath( path );
    return readFromFile( uri );
  }
  
  // file:///path
  protected static Element readFromFile( String uri )
  {
    String path = uri.substring( "file://".length() );

    MCREditorServlet.logger.debug( "Editor reading xml from file " + path );

    File file = new File( path );
    Element fromCache = (Element) fileCache.getIfUpToDate( path, file.lastModified() );
    if( fromCache != null ) return fromCache;
    
    try
    {
      Element parsed = parseStream( new FileInputStream( file ) );
      fileCache.put( path, parsed );
      return parsed;
    }
    catch( FileNotFoundException ex )
    {
      String msg = "Could not find file for URI " + uri;
      throw new MCRUsageException( msg, ex ); 
    }
  }
  
  // http:// oder https://
  protected static Element readFromHTTP( String url )
  {
    MCREditorServlet.logger.debug( "Editor reading xml from url " + url );

    try
    { return parseStream( new URL( url ).openStream() ); }
    catch( java.net.MalformedURLException ex )
    {
      String msg = "Malformed http url: " + url;
      throw new MCRUsageException( msg, ex ); 
    }
    catch( IOException ex )
    {
      String msg = "Unable to open input stream at " + url;
      throw new MCRUsageException( msg, ex ); 
    }
  }

  // request:pfad
  protected static Element readFromRequest( String uri )
  {
    String path = uri.substring( uri.indexOf( ":" ) + 1 );

    MCREditorServlet.logger.debug( "Editor reading xml from request " + path );

    StringBuffer url = new StringBuffer( MCRServlet.getBaseURL() );
    url.append( path );
    
    if( path.indexOf( "?" ) != -1 )
      url.append( "&" );
    else
      url.append( "?" );
    
    url.append( "MCRSessionID=" );
    url.append( MCRSessionMgr.getCurrentSession().getID() );
    
    return readFromHTTP( url.toString() );
  }
  
  protected static Element parseStream( InputStream in )
  { 
    try
    { return new org.jdom.input.SAXBuilder().build( in ).getRootElement(); }
    catch( Exception ex )
    {
      String msg = "Exception while reading and parsing XML InputStream";
      throw new MCRUsageException( msg, ex );
    }
  }
}
