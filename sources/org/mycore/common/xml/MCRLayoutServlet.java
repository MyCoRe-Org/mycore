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
import java.net.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.sax.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Does the layout for other MyCoRe servlets by transforming XML 
 * input to various output formats, using XSL stylesheets.
 *
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler
 *
 * @version $Revision$ $Date$
 */
public class MCRLayoutServlet extends HttpServlet 
{
  private static MCRCache cache;
  
  private static Logger logger = Logger.getLogger( MCRLayoutServlet.class );
  
  public static final String DOM_ATTR    = "MCRLayoutServlet.Input.DOM";
  public static final String JDOM_ATTR   = "MCRLayoutServlet.Input.JDOM";
  public static final String BYTE_ATTR   = "MCRLayoutServlet.Input.BYTES";
  public static final String FILE_ATTR   = "MCRLayoutServlet.Input.FILE";
  public static final String STREAM_ATTR = "MCRLayoutServlet.Input.STREAM";
  
  public void init()
  {
    MCRConfiguration config = MCRConfiguration.instance();
   	PropertyConfigurator.configure( config.getLoggingProperties() );
    buildTransformerFactory();
    cache = new MCRCache( 100 );
  }
  
  protected String parseDocumentType( InputStream in )
  {
    SAXParser parser = null;
    try{ parser = SAXParserFactory.newInstance().newSAXParser(); }
    catch( Exception ex )
    {
      String msg = "Could not build a SAX Parser for processing XML input";
      throw new MCRConfigurationException( msg, ex );
    }
    
    final Properties detected = new Properties();
    final String forcedInterrupt = "forcedInterrupt";
    
    DefaultHandler handler = new DefaultHandler()
    {
      public void startElement( String uri, String localName, String qName,
                                Attributes attributes )
        throws SAXException
      {
        detected.setProperty( "rootElementName", qName );
        throw new MCRException( forcedInterrupt );
      }
    };
    
    try{ parser.parse( new InputSource( in ), handler ); }
    catch( Exception ex )
    {
      if( ex instanceof MCRException )
      {
        if( ! ex.getMessage().equals( forcedInterrupt ) )
          throw (MCRException)ex; // Pass MCRException to invoker
      } 
      else
      {
        String msg = "Error while detecting XML document type from input source";
        throw new MCRException( msg, ex );
      }
    }
    
    return detected.getProperty( "rootElementName" );
  }
  
  protected void forwardXML( HttpServletRequest  request,
                             HttpServletResponse response )
    throws IOException, ServletException
  {
    response.setContentType( "text/xml" );
    OutputStream out = response.getOutputStream();

    if( request.getAttribute( JDOM_ATTR ) != null )
    {
      org.jdom.Document jdom = 
        (org.jdom.Document)( request.getAttribute( JDOM_ATTR ) );
      new org.jdom.output.XMLOutputter().output( jdom, out );
    }
    else if( request.getAttribute( DOM_ATTR ) != null )
    {
      org.w3c.dom.Document dom = 
        (org.w3c.dom.Document)( request.getAttribute( DOM_ATTR ) );
      org.jdom.Document jdom = new org.jdom.input.DOMBuilder().build( dom );
      new org.jdom.output.XMLOutputter().output( jdom, out );
    }
    else if( request.getAttribute( STREAM_ATTR ) != null )
    {
      InputStream in = (InputStream)( request.getAttribute( STREAM_ATTR ) );
      MCRUtils.copyStream( in, out );
    }
    else if( request.getAttribute( BYTE_ATTR ) != null )
    {
      byte[] bytes = (byte[])( request.getAttribute( BYTE_ATTR ) );
      MCRUtils.copyStream( new ByteArrayInputStream( bytes ), out );
    }
    else if( request.getAttribute( FILE_ATTR ) != null )
    {
      File file = (File)( request.getAttribute( FILE_ATTR ) );
      FileInputStream fis = new FileInputStream( file );
      MCRUtils.copyStream( fis, out );
      fis.close();
    }
    
    out.close();
  }

  protected void doPost( HttpServletRequest  request, 
                         HttpServletResponse response )
    throws IOException, ServletException
  { doGet( request, response ); }

  protected void doGet( HttpServletRequest  request, 
                        HttpServletResponse response ) 
    throws IOException, ServletException
  {
    Source sourceXML = null;   
    String docType   = null;
    
    if( request.getAttribute( JDOM_ATTR ) != null )
    {
      org.jdom.Document jdom = 
        (org.jdom.Document)( request.getAttribute( JDOM_ATTR ) );
      sourceXML = new org.jdom.transform.JDOMSource( jdom );
      
      if( jdom.getDocType() != null ) 
        docType = jdom.getDocType().getElementName();
      else
        docType = jdom.getRootElement().getName();
    }
    else if( request.getAttribute( DOM_ATTR ) != null )
    {
      org.w3c.dom.Document dom = 
        (org.w3c.dom.Document)( request.getAttribute( DOM_ATTR ) );
      sourceXML = new DOMSource( dom );
      
      if( dom.getDoctype() != null ) 
        docType = dom.getDoctype().getName();
      else
        docType = dom.getDocumentElement().getTagName();
    }
    else if( request.getAttribute( STREAM_ATTR ) != null )
    {
      int bufferSize = 1000;
      InputStream is = (InputStream)( request.getAttribute( STREAM_ATTR ) );
      PushbackInputStream pis = new PushbackInputStream( is, bufferSize );
      pis.mark( bufferSize );
      docType = parseDocumentType( pis );
      pis.reset();
      sourceXML = new StreamSource( pis );   
    }
    else if( request.getAttribute( BYTE_ATTR ) != null )
    {
      byte[] bytes = (byte[])( request.getAttribute( BYTE_ATTR ) );
      docType = parseDocumentType( new ByteArrayInputStream( bytes ) );
      sourceXML = new StreamSource( new ByteArrayInputStream( bytes ) );
    }
    else if( request.getAttribute( FILE_ATTR ) != null )
    {
      File file = (File)( request.getAttribute( FILE_ATTR ) );
      FileInputStream fis = new FileInputStream( file );
      docType = parseDocumentType( fis );
      fis.close();
      sourceXML = new StreamSource( file );
    }
    
    Properties parameters = buildXSLParameters( request );
    String style = parameters.getProperty( "Style", "default" );
    logger.debug( "MCRLayoutServlet using style " + style );

    if( "xml".equals( style ) ) 
    {
      forwardXML( request, response );
    }
    else
    {
      String styleName = buildStylesheetName( style, docType );
      String styleDir  = "/WEB-INF/stylesheets/";
      File styleFile   = getStylesheetFile( styleDir, styleName );

      if( styleFile == null ) 
        forwardXML( request, response );
      else
      {
        Templates stylesheet = buildCompiledStylesheet( styleFile );
        Transformer transformer = buildTransformer( stylesheet );
        setXSLParameters( transformer, parameters );
        try 
        { transform( sourceXML, stylesheet, transformer, response ); } 
        catch( IOException ex ) 
        {	logger.error("IO Error while XSL transforming XML Document", ex ); }
      }
    }
  }

  public static final Properties buildXSLParameters( HttpServletRequest request )
  {
  	Properties parameters = new Properties();
	  String user = null;
    
  	// Read all *.xsl attributes that are stored in the browser session
	  HttpSession session = request.getSession( false );
  	if( session != null )
    {
		  for( Enumeration e = session.getAttributeNames(); e.hasMoreElements(); )		
      {
	  		String name = (String)( e.nextElement() );
	  		if( name.startsWith( "XSL." ) )
	  			parameters.put( name.substring( 4 ), session.getAttribute( name ) );
	  	}
  		user = (String)( session.getAttribute( "XSL.CurrentUser" ) );
    }
	    
	  // Read all *.xsl attributes provided by the invoking servlet
	  for( Enumeration e = request.getAttributeNames(); e.hasMoreElements(); )
	  {
	    String name = (String)( e.nextElement() );
	    if( name.startsWith( "XSL." ) )
        parameters.put( name.substring( 4 ), request.getAttribute( name ) );
	  }
      
    // Read all *.xsl attributes from the client HTTP request parameters
	  for( Enumeration e = request.getParameterNames(); e.hasMoreElements(); )
	  {
	    String name = (String)( e.nextElement() );
	    if( name.startsWith( "XSL." ) )
	    {
		    parameters.put( name.substring( 4 ), request.getParameter( name ) );
	    }
	  }

	  // Set some predefined XSL parameters:

	  if( user == null ) user = "gast";
    
	  String contextPath = request.getContextPath() + "/";
  	String requestURL  = getCompleteURL( request );
    
	  int pos = requestURL.indexOf( contextPath, 9 );
	  String applicationBaseURL = requestURL.substring( 0, pos ) + contextPath;

	  String servletsBaseURL = applicationBaseURL + "servlets/";

	  String defaultLang = MCRConfiguration.instance()
	    .getString( "MCR.metadata_default_lang", "en" );

	  parameters.put( "CurrentUser",           user               );
	  parameters.put( "RequestURL",            requestURL         );
	  parameters.put( "WebApplicationBaseURL", applicationBaseURL );
	  parameters.put( "ServletsBaseURL",       servletsBaseURL    );
	  parameters.put( "DefaultLang",           defaultLang        );
    
    return parameters;
  }

  protected static final String getCompleteURL( HttpServletRequest request )
  {
    StringBuffer buffer = HttpUtils.getRequestURL( request );
    String queryString = request.getQueryString();
    if( queryString != null ) buffer.append( "?" ).append( queryString );
    return buffer.toString();
  }
  
 /**
  * Builds the filename of the stylesheet to use, e. g. "playlist-simple.xsl"
  **/
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

 /**
  * Gets a File object for the given filename and directory,
  * or returns null if no such file exists.
  **/
  protected File getStylesheetFile( String dir, String name )
  {
    String path = getServletContext().getRealPath( dir + name );
    File file = new File( path );
    
    if( ! file.exists() )
    {
      logger.debug( "MCRLayoutServlet did not find stylesheet " + name );
      return null;
    }

    if( ! file.canRead() )
    {
      String msg = "XSL stylesheet " + path + " not readable";
      throw new MCRConfigurationException( msg );
    }  
    
    return file;
  }
  
  /** The XSL transformer factory to use */
  protected SAXTransformerFactory factory;  
  
  /** 
   * Builds a SAX transformer factory for later use 
   *
   * @throws MCRConfigurationException if no SAXTransformerFactory was found
   **/
  protected void buildTransformerFactory()
  {
    TransformerFactory tf = TransformerFactory.newInstance();
    
    if( ! tf.getFeature( SAXTransformerFactory.FEATURE ) )
      throw new MCRConfigurationException
      ( "Could not load a SAXTransformerFactory for use with XSLT" );
      
    factory = (SAXTransformerFactory)( tf );
  }
  
  /**
   * Reads an XSL stylesheet from the given file and returns it as compiled
   * XSL Templates object.
   *
   * @param file the File that contains the XSL stylesheet
   * @return the compiled stylesheet
   **/
  protected Templates buildCompiledStylesheet( File file )
  {
    String path = file.getPath();
    long   time = file.lastModified();
    
    Templates stylesheet = (Templates)( cache.getIfUpToDate( path, time ) );
    
    if( stylesheet == null )
    {
      try
      { 
        stylesheet = factory.newTemplates( new StreamSource( file ) ); 
        logger.debug( "MCRLayoutServlet compiled stylesheet " + file.getName() );
      }
      catch( TransformerConfigurationException exc )
      {
        String msg = "Error while compiling XSL stylesheet " + file.getName() 
                     + ": " + exc.getMessageAndLocation();
        throw new MCRConfigurationException( msg, exc );
      }
      
      cache.put( path, stylesheet );
    }
    else
    {
      logger.debug( "MCRLayoutServlet using cached stylesheet " + file.getName() );
    }
    
    return stylesheet;
  }

  /**
   * Builds a XSL transformer that uses the given XSL stylesheet 
   *
   * @param stylesheet the compiled XSL stylesheet to use
   * @return the XSL transformer that can be used to do the XSL transformation
   **/
  protected Transformer buildTransformer( Templates stylesheet )
  {
    try
    { return factory.newTransformerHandler( stylesheet ).getTransformer(); }
    catch( TransformerConfigurationException exc )
    {
      String msg = "Error while building XSL transformer: " + exc.getMessageAndLocation();
      throw new MCRConfigurationException( msg, exc );
    }
  }  
    
  /**
   * Sets XSL parameters for the given transformer by taking them from
   * the properties object provided.
   *
   * @param transformer the Transformer object thats parameters should be set
   * @param parameters the XSL parameters as name-value pairs
   **/
  protected void setXSLParameters( Transformer transformer,
                                   Properties  parameters )
  {
    Enumeration names = parameters.propertyNames();
    
    while( names.hasMoreElements() )
    {
      String name  = (String)( names.nextElement() );
      String value = parameters.getProperty( name );

      transformer.setParameter( name, value );
    }
  }

  /**
   * Transforms XML input with the given XSL stylesheet and sends
   * the output as HTTP Servlet Response to the client browser.
   *
   * @param xml the XML input document
   * @param xsl the compiled XSL stylesheet
   * @param transformer the XSL transformer to use
   * @param response the response object to send the result to
   **/
  protected void transform( Source       xml,
                            Templates    xsl,
                            Transformer  transformer,
                            HttpServletResponse response )
    throws IOException
  {
    // Set content type  from "<xsl:output media-type = "...">
    // Set char encoding from "<xsl:output encoding   = "...">
    String ct  = xsl.getOutputProperties().getProperty( "media-type" );
    String enc = xsl.getOutputProperties().getProperty( "encoding"   );
    response.setContentType( ct + "; charset=" + enc );
    logger.debug( "MCRLayoutServlet starts to output " + ct + "; charset=" + enc );

    OutputStream out = response.getOutputStream();
    
    try{ transformer.transform( xml, new StreamResult( out ) ); }
    catch( TransformerException ex )
    {
      String msg = "Error while transforming XML using XSL stylesheet: " + ex.getMessageAndLocation();
      throw new MCRException( msg, ex );
    }
    finally{ out.close(); }
  }
}
