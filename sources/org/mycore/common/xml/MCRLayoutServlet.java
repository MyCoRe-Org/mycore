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
 * @author Marc Schlüpmann
 *
 * @version $Revision$ $Date$
 */
public class MCRLayoutServlet extends HttpServlet 
{
  /** The JAXP Transformer Factory for XSLT **/
  protected TransformerFactory factory;  

  /** Initializes the servlet **/
  public void init()
  {
    factory = TransformerFactory.newInstance();
    if( ! factory.getFeature( SAXTransformerFactory.FEATURE ) )
      throw new MCRConfigurationException
      ( "Could not find a SAXTransformerFactory for XSLT" );
  }

  /** Handles a layout request */
  public void doGet( HttpServletRequest req, HttpServletResponse res ) 
    throws IOException, ServletException
  {
    org.jdom.Document jdom = getInputXMLasJDOM( req, res );
    if( jdom == null ) 
    {
      res.sendError( res.SC_NOT_FOUND, "No XML input to layout" );
      return;
    }

    String stylesheetName = chooseStylesheet( req, jdom );
    
    // Just output as plain XML?
    if( stylesheetName == null )
    {
      renderAsXML( jdom, res );
      return;
    }
    
    // Use a stylesheet for output
    setAttributes( req );
    Templates stylesheet = getCompiledStylesheet( stylesheetName );
    transform( stylesheet, jdom, req, res );
  }
  
  protected org.jdom.Document getInputXMLasJDOM( HttpServletRequest  req,
                                                 HttpServletResponse res )
    throws IOException
  {
    try
    {
      // If invoker provides a JDOM object, use it
      org.jdom.Document jdom = (org.jdom.Document)req.getAttribute( "jdom" );
      if( jdom != null ) return jdom;
    
      // If invoker provides a DOM object, convert to JDOM and use it
      org.w3c.dom.Document dom = (org.w3c.dom.Document)req.getAttribute( "dom" );
      if( dom != null ) return new org.jdom.input.DOMBuilder().build( dom );

      // Servlet was invoked by *.xml mapping, so we read XML from the filesystem    
      String path = req.getServletPath();
      URL url = null;

      try
      { url = getServletContext().getResource( path ); }
      catch( MalformedURLException ignored ){}

      if( url == null )
      {
        res.sendError( res.SC_NOT_FOUND, path );
        return null;
      }

      String realPath = getServletContext().getRealPath( path );
      String realDir  = new File( realPath ).getParent() + File.separator;

      req.setAttribute( "xsl.DocumentBase", realDir );

      return new org.jdom.input.SAXBuilder().build( url );
    }
    catch( org.jdom.JDOMException ex )
    {
      String msg = "Error while parsing XML input for layout";
      throw new MCRException( msg, ex );
    }
  }

  protected void setAttributes( HttpServletRequest req )
  {
    String user = (String)( req.getSession().getAttribute( "xsl.LoginUser" ) );
    if( ( user == null ) || ( user.length() == 0 ) ) user = "gast";

    String contextPath = req.getContextPath();
    if( contextPath == null ) contextPath = "";
    contextPath += "/";

    String requestURL = HttpUtils.getRequestURL( req ).toString();
    int pos = requestURL.indexOf( contextPath, 9 );
    String baseURL = requestURL.substring( 0, pos ) + contextPath;

    String servletURL = baseURL + "servlets/";

    req.setAttribute( "xsl.LoginUser",       user       );
    req.setAttribute( "xsl.RequestURL",      requestURL );
    req.setAttribute( "xsl.ApplicationBase", baseURL    );
    req.setAttribute( "xsl.ServletBase",     servletURL );
  }
  
  protected String getDocumentType( org.jdom.Document doc )
  {
    // If a document type is declared, use its name  
    org.jdom.DocType doctype = doc.getDocType();
    if( doctype != null ) return doctype.getElementName();
    
    // Otherwise, use name of the root element as document type
    return doc.getRootElement().getName();
  }

  protected String chooseStylesheet( HttpServletRequest req, org.jdom.Document jdom )
  {
    String style = req.getParameter( "style" );
    if( style == null ) style = (String)( req.getAttribute( "style" ) );
   
    // "style=XML" means output as XML, do not use a stylesheet
    if( "xml".equals( style ) ) return null;

    // No style parameter means default stylesheet
    if( style == null ) style = ""; else style = "-" + style;

    // Build stylesheet name, e. g. "storyboard-simple.xsl"
    return getDocumentType( jdom ) + style + ".xsl";
  }

  protected void renderAsXML( org.jdom.Document doc, HttpServletResponse res )
    throws IOException
  {
    res.setContentType( "text/xml" );
    OutputStream out = res.getOutputStream();
    new org.jdom.output.XMLOutputter( "  ", true ).output( doc, out );
    out.close();
  }
  
  protected Templates getCompiledStylesheet( String name )
  {
    String path = getServletContext().getRealPath( "/WEB-INF/stylesheets/" + name );
    File file = new File( path );
    
    if( ! ( file.exists() && file.canRead() ) )
    {
      String msg = "XSL stylesheet " + name + " not found or not readable";
      throw new MCRConfigurationException( msg );
    }  
    
    Source source = new StreamSource( file );

    try
    { return factory.newTemplates( source ); }
    catch( TransformerConfigurationException ex )
    {
      String msg = "Error while compiling XSL stylesheet " + name;
      throw new MCRConfigurationException( msg, ex );
    }
  }

  protected void transform( Templates xsl, 
                            org.jdom.Document jdom, 
                            HttpServletRequest req,
                            HttpServletResponse res )
    throws IOException
  {
    // Set content type  from "<xsl:output media-type = "...">
    // Set char encoding from "<xsl:output encoding   = "...">

    String ct  = xsl.getOutputProperties().getProperty( "media-type" );
    String enc = xsl.getOutputProperties().getProperty( "encoding"   );
    res.setContentType( ct + "; charset=" + enc );

    try
    {
      // Output JDOM via SAXOutputter should result in best performance
      SAXTransformerFactory f = (SAXTransformerFactory)factory;
      TransformerHandler h = f.newTransformerHandler( xsl );

      Transformer t = h.getTransformer();
      for( Enumeration names = req.getAttributeNames(); names.hasMoreElements(); )
      {
        String name = (String)( names.nextElement() );
        if( ! name.startsWith( "xsl." ) ) continue;
 
        String value = (String)( req.getAttribute( name ) );
        t.setParameter( name.substring( 4 ), value );
      }
    
      OutputStream out = res.getOutputStream();
      h.setResult( new StreamResult( out ) );
    
      new org.jdom.output.SAXOutputter( h ).output( jdom ); 
      out.close();
    }
    catch( TransformerConfigurationException ex )
    {
      String msg = "Error while creating XSL Transformer for stylesheet";
      throw new MCRConfigurationException( msg, ex );
    }
    catch( org.jdom.JDOMException ex )
    {
      String msg = "Error while transforming XML via XSL stylesheet";
      throw new MCRException( msg, ex );
    }
  }
}
