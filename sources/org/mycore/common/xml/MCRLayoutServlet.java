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

package mycore.xml;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.sax.*;
import mycore.common.*;

/**
 * Does the layout for other MyCoRe servlets by transforming XML 
 * input to various output formats, using XSL stylesheets.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRLayoutServlet extends HttpServlet 
{
  protected SAXTransformerFactory factory;  
  protected MCRCache stylesheetCache;
  protected MCRCache staticFileCache;

  public void init()
  {
    // Get SAX transformer factory
    TransformerFactory tf = TransformerFactory.newInstance();
      
    if( ! tf.getFeature( SAXTransformerFactory.FEATURE ) )
      throw new MCRConfigurationException
      ( "Could not load a SAXTransformerFactory for use with XSLT" );
      
    factory = (SAXTransformerFactory)( tf );
    
    // Create caches
    stylesheetCache = new MCRCache( 50 );
  }
    
 /**
  * Reads the input XML from a file or from the invoking servlet,
  * chooses a stylesheet and does the layout by applying the XSL
  * stylesheet.
  */
  public void doGet( HttpServletRequest  request, 
                     HttpServletResponse response ) 
    throws IOException, ServletException
  {
    Properties parameters = buildXSLParameters( request );
    
    org.jdom.Document xml;  
      
    if( invokedByServlet( request ) )  
      xml = getXMLInputFromServlet( request );
    else
      xml = getXMLInputFromFile( request, parameters );
    
    String style = parameters.getProperty( "Style", "default" );

    if( "xml".equals( style ) )
    {
      renderAsXML( xml, response );
    }
    else
    {
      String documentType = getDocumentType( xml );
      String styleName    = buildStylesheetName( style, documentType );
      String styleDir     = "/WEB-INF/stylesheets/";
      
      File styleFile = getStylesheetFile( styleDir, styleName );
      Templates stylesheet = getCompiledStylesheet( factory, styleFile );
      TransformerHandler handler = getHandler( stylesheet );
      setXSLParameters( handler, parameters );
      transform( xml, stylesheet, handler, response );
    }
  }

 /**
  * Returns true, if LayoutServlet was invoked by another servlet, or false
  * otherwise, meaning it was invoked by a direct static "*.xml" mapping.
  */
  protected boolean invokedByServlet( HttpServletRequest request )
  { 
    return ( request.getAttribute( "MCRLayoutServlet.Input.JDOM" ) != null ) ||
           ( request.getAttribute( "MCRLayoutServlet.Input.DOM"  ) != null );
  }
  
 /**
  * Assuming that LayoutServlet was invoked by another servlet by 
  * request dispatching, gets XML input as JDOM or DOM document from
  * the request attributesc that the invoking servlet has set.
  */
  protected org.jdom.Document getXMLInputFromServlet( HttpServletRequest request )
  {
    org.jdom.Document jdom = (org.jdom.Document)
      request.getAttribute( "MCRLayoutServlet.Input.JDOM" );
    if( jdom != null) return jdom;
    
    org.w3c.dom.Document dom = (org.w3c.dom.Document)
      request.getAttribute( "MCRLayoutServlet.Input.DOM" );

    try{ return new org.jdom.input.DOMBuilder().build( dom ); }
    catch( Exception exc )
    {
      String msg = "LayoutServlet could not transform DOM input to JDOM";
      throw new MCRException( msg, exc );
    }
  }
  
  /**
   * Assuming that LayoutServlet was invoked by a static "*.xml" url mapping,
   * reads a static xml file from disk and parses it to a JDOM document as
   * input.
   */
  protected org.jdom.Document getXMLInputFromFile( HttpServletRequest request,
                                                   Properties parameters )
  {
    String requestedPath = request.getServletPath();
    URL url = null;
    
    try{ url = getServletContext().getResource( requestedPath ); }
    catch( MalformedURLException willNeverBeThrown ){}
    
    if( url == null )
    {
      String msg = "LayoutServlet could not find file " + requestedPath;
      throw new MCRException( msg );
    }

    String realPath = getServletContext().getRealPath( requestedPath );
    String documentBaseURL = new File( realPath ).getParent() + File.separator;

    parameters.put( "DocumentBaseURL", documentBaseURL );
    
    try{ return new org.jdom.input.SAXBuilder().build( url ); }
    catch( Exception exc )
    {
      String msg = "LayoutServlet could not parse XML input from " + realPath;
      throw new MCRException( msg, exc );
    }
  }
  
  protected Properties buildXSLParameters( HttpServletRequest request )
  {
    Properties parameters = new Properties();
    
    // Read all *.xsl attributes that are stored in the browser session
    HttpSession session = request.getSession();
    for( Enumeration e = session.getAttributeNames(); e.hasMoreElements(); )
    {
      String name = (String)( e.nextElement() );
      if( name.startsWith( "XSL." ) )
      {
        String value = (String)( session.getAttribute( name ) );
        parameters.put( name.substring( 4 ), value );
      }
    }
    
    // Read all *.xsl attributes provided by the invoking servlet
    for( Enumeration e = request.getAttributeNames(); e.hasMoreElements(); )
    {
      String name = (String)( e.nextElement() );
      if( name.startsWith( "XSL." ) )
      {
        String value = (String)( request.getAttribute( name ) );
        parameters.put( name.substring( 4 ), value );
      }
    }
      
    // Read all *.xsl attributes from the client HTTP request parameters
    for( Enumeration e = request.getParameterNames(); e.hasMoreElements(); )
    {
      String name = (String)( e.nextElement() );
      if( name.startsWith( "XSL." ) )
      {
        String value = request.getParameter( name );
        parameters.put( name.substring( 4 ), value );
      }
    }

    // Set some predefined XSL parameters:

    String user = (String)( session.getAttribute( "XSL.CurrentUser" ) );
    if( user == null ) user = "gast";
    
    String contextPath = request.getContextPath() + "/";
    String requestURL  = getCompleteURL( request );
    
    int pos = requestURL.indexOf( contextPath, 9 );
    String applicationBaseURL = requestURL.substring( 0, pos ) + contextPath;

    String servletsBaseURL = applicationBaseURL + "servlets/";

    parameters.put( "CurrentUser",           user               );
    parameters.put( "RequestURL",            requestURL         );
    parameters.put( "WebApplicationBaseURL", applicationBaseURL );
    parameters.put( "ServletsBaseURL",       servletsBaseURL    );
    
    return parameters;
  }

  protected String getCompleteURL( HttpServletRequest request )
  {
    StringBuffer buffer = HttpUtils.getRequestURL( request );

    String queryString = request.getQueryString();

    if( queryString != null )
    {
      buffer.append( "?" );
      StringTokenizer tokenizer = new StringTokenizer( queryString, "&" );
      while( tokenizer.hasMoreTokens() )
      {
        String token = tokenizer.nextToken();
        String encoded =  URLEncoder.encode( token );
        buffer.append( encoded );
        if( tokenizer.hasMoreTokens() ) buffer.append( "&" );
      }
    }
    return buffer.toString();
  }
  
  protected void renderAsXML( org.jdom.Document xml,
                              HttpServletResponse response )
    throws IOException
  {
    response.setContentType( "text/xml" );
    OutputStream out = response.getOutputStream();
    new org.jdom.output.XMLOutputter( "  ", true ).output( xml, out );
    out.close();
  }
  
  /**
   * Returns the XML document type name as declared in the XML input document,
   * or otherwise returns the name of the root element instead.
   */
  protected String getDocumentType( org.jdom.Document xml )
  {
    if( xml.getDocType() != null ) 
      return xml.getDocType().getElementName();
    else
      return xml.getRootElement().getName();
  }

 /**
  * Builds the filename of the stylesheet to use, e. g. "playlist-simple.xsl"
  */
  protected String buildStylesheetName( String style, String docType )
  {
    StringBuffer filename = new StringBuffer( docType );
    if( ! "default".equals( style ) )
    {
      filename.append( "-"   );
      filename.append( style );
    }
    filename.append( ".xsl" );
    
    return filename.toString();
  }
  
  protected File getStylesheetFile( String dir, String name )
  {
    String path = getServletContext().getRealPath( dir + name );
    File file = new File( path );
    
    if( ! file.exists() )
    {
      String msg = "XSL stylesheet " + path + " does not exist";
      throw new MCRConfigurationException( msg );
    }  
    if( ! file.canRead() )
    {
      String msg = "XSL stylesheet " + path + " not readable";
      throw new MCRConfigurationException( msg );
    }  
    
    return file;
  }
  
  class MCRLayoutCacheEntry
  {
    Object value;
    long   lastModified;
  }
  
  protected Templates getCompiledStylesheet( TransformerFactory factory,
                                             File file )
  {
    String path = file.getPath();
    long   time = file.lastModified();
    
    Templates stylesheet = null;
    
    MCRLayoutCacheEntry entry = (MCRLayoutCacheEntry)( stylesheetCache.get( path ) );
    if( entry != null )
    {
      if( time > entry.lastModified )
        stylesheetCache.remove( path );
      else
        stylesheet = (Templates)( entry.value );
    }
    
    if( stylesheet == null )
    {
      try
      { stylesheet = factory.newTemplates( new StreamSource( file ) ); }
      catch( TransformerConfigurationException exc )
      {
        String msg = "Error while compiling XSL stylesheet " + file.getName();
        throw new MCRConfigurationException( msg, exc );
      }
      
      entry = new MCRLayoutCacheEntry();
      entry.value = stylesheet;
      entry.lastModified = time;
      stylesheetCache.put( path, entry );
    }
    
    return stylesheet;
  }

  protected TransformerHandler getHandler( Templates stylesheet )
  {
    try
    { return factory.newTransformerHandler( stylesheet ); }
    catch( TransformerConfigurationException exc )
    {
      String msg = "Error while compiling XSL stylesheet";
      throw new MCRConfigurationException( msg, exc );
    }
  }  
    
  protected void setXSLParameters( TransformerHandler handler,
                                   Properties         parameters )
  {
    Transformer transformer = handler.getTransformer();
    Enumeration names       = parameters.propertyNames();
    
    while( names.hasMoreElements() )
    {
      String name  = (String)( names.nextElement() );
      String value = parameters.getProperty( name );

      transformer.setParameter( name, value );
    }
  }
  
  protected void transform( org.jdom.Document   xml,
                            Templates           xsl,
                            TransformerHandler  handler,
                            HttpServletResponse response )
    throws IOException
  {
    // Set content type  from "<xsl:output media-type = "...">
    // Set char encoding from "<xsl:output encoding   = "...">

    String ct  = xsl.getOutputProperties().getProperty( "media-type" );
    String enc = xsl.getOutputProperties().getProperty( "encoding"   );
    response.setContentType( ct + "; charset=" + enc );

    OutputStream out = response.getOutputStream();
    handler.setResult( new StreamResult( out ) );
    
    try
    { new org.jdom.output.SAXOutputter( handler ).output( xml ); }
    catch( org.jdom.JDOMException ex )
    {
      String msg = "Error while transforming XML using XSL stylesheet";
      throw new MCRException( msg, ex );
    }
    finally
    { out.close(); }
  }
}
