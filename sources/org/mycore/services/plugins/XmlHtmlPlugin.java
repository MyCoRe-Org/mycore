/**
 * $RCSfile$ $Revision$ $Date: 2005/08/11
 * 13:47:35 $ This file is part of ** M y C o R e ** Visit our homepage at
 * http://www.mycore.de/ for details. This program is free software; you can use
 * it, redistribute it and / or modify it under the terms of the GNU General
 * Public License (GPL) as published by the Free Software Foundation; either
 * version 2 of the License or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program, normally in the file license.txt. If not, write to the Free Software
 * Foundation Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307 USA
 */

package org.mycore.services.plugins;

import org.mycore.datamodel.ifs.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
//import java.io.IOException;
import java.io.InputStream;
//import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

import org.jdom.*;
//import org.jdom.output.*;
//import org.jdom.input.DOMBuilder;
//import org.w3c.dom.Document;
import org.w3c.tidy.*;
//import org.xml.sax.InputSource;
//import org.xml.sax.SAXException;

/**
 * Converts XML, XTHML and HTML to plain text for indexing
 * 
 * @author Frank Lützenkirchen
 * @author Harald Richter
 */
public class XmlHtmlPlugin  implements TextFilterPlugin
{
    private static final int MAJOR = 1;

    private static final int MINOR = 0;

    private static HashSet contentTypes;

    private static String info = null;

    /**
     *  
     */
    public XmlHtmlPlugin() {
        super();
        if (contentTypes == null) {
            contentTypes = new HashSet();
            if (MCRFileContentTypeFactory.isTypeAvailable("xml"))
                contentTypes.add(MCRFileContentTypeFactory.getType("xml"));
            if (MCRFileContentTypeFactory.isTypeAvailable("html"))
                contentTypes.add(MCRFileContentTypeFactory.getType("html"));
        }
        if (info == null)
            info = new StringBuffer("This filter converts XML, XTHML and HTML to plain text").toString();
            
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.plugins.TextFilterPlugin#getName()
     */
    public String getName() {
        return "Frank's amazing xml and html Filter";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.plugins.TextFilterPlugin#getInfo()
     */
    public String getInfo() {
        return info;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.plugins.XmlHtmlPlugin#getSupportedContentTypes()
     */
    public HashSet getSupportedContentTypes() {
        return contentTypes;
    }
    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.plugins.TextFilterPlugin#transform(org.mycore.datamodel.ifs.MCRFileContentType,org.mycore.datamodel.ifs.MCRContentInputStream,
     *      java.io.OutputStream)
     */
    public Reader transform(MCRFileContentType ct, InputStream input)
            throws FilterPluginTransformException {
        if (getSupportedContentTypes().contains(ct)) {
            String tx = getFullText(ct, input );
            return new StringReader(tx);
        } else
            throw new FilterPluginTransformException("ContentType " + ct
                    + " is not supported by " + getName() + "!");
    }
    
    /**
     * @see org.mycore.services.plugins.TextFilterPlugin#getMajorNumber()
     */
    public int getMajorNumber() {
        return MAJOR;
    }

    /**
     * @see org.mycore.services.plugins.TextFilterPlugin#getMinorNumber()
     */
    public int getMinorNumber() {
        return MINOR;
    }

  private static String getFullText(MCRFileContentType ct, InputStream input ) 
  {
    try
    {
      if( ct.getID().equals( "xml" ) )
      {
        org.jdom.input.SAXBuilder builder = new org.jdom.input.SAXBuilder();
        return getText( builder.build(input) );//file.getContentAsJDOM() );
      }
      else if( ct.getID().equals( "html" ) )
      {
        org.jdom.Document xml = tidy( input );
        return getText( xml );
      }
      else
        return null;
    }
    catch( Exception ex )
    {
      ex.printStackTrace();
      return null;
    }
  }

  /** Converts HTML files to XML to be able to extract text nodes * */
  private static org.jdom.Document tidy( InputStream input )
      throws java.io.IOException
  {
/*    
    org.cyberneko.html.parsers.DOMParser parser = new org.cyberneko.html.parsers.DOMParser();
    
    try
    {
      parser.parse( new InputSource(input) );
    } catch (SAXException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    DOMBuilder builder = new DOMBuilder();
    org.jdom.Document document = builder.build( parser.getDocument() );
    return document;
*/    
    Tidy tidy = new Tidy();
    tidy.setForceOutput( true );
    tidy.setXmlOut( true );
    tidy.setShowWarnings(false);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    tidy.parseDOM( input, out);
    
    byte[] output = out.toByteArray();
    org.jdom.input.SAXBuilder builder = new org.jdom.input.SAXBuilder();
    org.jdom.Document xml = null;
    try
    {
      xml = builder.build(new ByteArrayInputStream(output));
    } catch (JDOMException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    return xml;
  }

  /** Extracts text of text nodes and comment nodes from xml files * */
  private static String getText( org.jdom.Document xml )
  {
    StringBuffer buffer = new StringBuffer();
    xml2txt( buffer, xml.getContent() );
    return buffer.toString();
  }

  /** Extracts text of text nodes and comment nodes from xml files * */
  private static void xml2txt( StringBuffer buffer, List content )
  {
    for( int i = 0; ( content != null ) && ( i < content.size() ); i++ )
    {
      Object obj = content.get( i );
      if( obj instanceof Element )
      {
        Element elem = (Element)obj;
        xml2txt( buffer, elem.getContent() );
      }
      else if( obj instanceof Text )
      {
        Text text = (Text)obj;
        buffer.append( text.getTextTrim() ).append( "\n\n" );
      }
      else if( obj instanceof Comment )
      {
        Comment comm = (Comment)obj;
        buffer.append( comm.getText() ).append( "\n\n" );
      }
    }
  }
}