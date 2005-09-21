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
import java.util.*;
import org.jdom.Element;
import org.jdom.*;
import org.jdom.input.DOMBuilder;
import org.w3c.dom.Document;
import org.w3c.tidy.*;

/**
 * Converts XML, XTHML and HTML to plain text for indexing
 * 
 * @author Frank Lützenkirchen
 */
public class XmlHtmlPlugin
{

  private static String getFullText( MCRFile file )
  {
    try
    {
      if( file.getContentTypeID().equals( "xml" ) )
        return getText( file.getContentAsJDOM() );
      else if( file.getContentTypeID().equals( "html" ) )
        return getText( tidy( file ) );
      else
        return null;
    }
    catch( Exception ex )
    {
      return null;
    }
  }

  /** Converts HTML files to XML to be able to extract text nodes * */
  private static org.jdom.Document tidy( MCRFile file )
      throws java.io.IOException
  {
    Tidy tidy = new Tidy();
    tidy.setForceOutput( true );
    tidy.setXmlOut( true );
    Document doc = tidy.parseDOM( file.getContentAsInputStream(), null );
    return new DOMBuilder().build( doc );
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