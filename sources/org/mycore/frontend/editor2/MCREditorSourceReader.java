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

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.mycore.common.*;
import org.mycore.frontend.servlets.*;

import org.jdom.Element;
import org.jdom.Attribute;

/**
 * This class contains the functionality to read XML input that
 * should be editor in the editor.
 * 
 * @author Frank Lützenkirchen
 **/
public class MCREditorSourceReader
{
  /**
   * Reads XML input from an url and returns a list of source variable elements
   * that can be processed by editor.xsl
   **/
  static List readSource( Element editor, Map parameters )
  {
    Element source = editor.getChild( "source" );
    String[] urlFromRequest   = (String[])( parameters.get( "XSL.editor.source.url" ) );
    String[] tokenFromRequest = (String[])( parameters.get( "XSL.editor.source.id"  ) );
    String[] newFromRequest   = (String[])( parameters.get( "XSL.editor.source.new" ) );

    String url = null;

    if( ( newFromRequest != null ) && ( newFromRequest[ 0 ].equals( "true" ) ) )
    {
      url = null;
    }
    else if( ( urlFromRequest != null ) && ( urlFromRequest[ 0 ].trim().length() > 0 ) )
    {
      url = urlFromRequest[ 0 ].trim();
    }
    else if( ( tokenFromRequest != null ) && ( tokenFromRequest[ 0 ].trim().length() > 0 ) && ( source != null ) )
    {
      String urlFromDef   = source.getAttributeValue( "url" );
      String tokenFromDef = source.getAttributeValue( "token" );
      if( ( urlFromDef != null ) && ( tokenFromDef != null ) )
      {
        int pos = urlFromDef.indexOf( tokenFromDef );
        if( pos != -1 )
        {
          String before = urlFromDef.substring( 0, pos );
          String after  = urlFromDef.substring( pos + tokenFromDef.length() );
          url = before + tokenFromRequest[ 0 ].trim() + after;
        }
      }
    }
    else if( ( source != null ) && ( source.getAttributeValue( "url", "" ).trim().length() > 0 ) )
    {
      url = source.getAttributeValue( "url" );
    }

    if( ( url == null ) || ( url.trim().length() == 0 ) )
    {
      MCREditorServlet.logger.info( "Editor is started empty without XML input" );
      return null;
    }
    else
    {
      if( ! ( url.startsWith( "http://" ) || url.startsWith( "https://" ) || url.startsWith( "file://" ) ) )
      {
        url = MCRServlet.getBaseURL() + url;
      }

      StringBuffer sb = new StringBuffer( url );
      sb.append( url.indexOf( "?" ) == -1 ? "?" : "&" );
      sb.append( "MCRSessionID=" );
      sb.append( MCRSessionMgr.getCurrentSession().getID() );
      url = sb.toString();

      MCREditorServlet.logger.info( "Editor reading XML input from " + url );
      Element input = MCREditorResolver.readXML( url );
      return buildVariables( input );
    }
  }

  private static List buildVariables( Element input )
  { 
    List variables = new ArrayList();
    setVariablesFromElement( variables, input, "/", "" );
    return variables;
  }

  private static void setVariablesFromElement( List variables, Element element, String prefix, String suffix )
  {
    String path = prefix + element.getName() + suffix;
    String text = element.getText();

    addVariable( variables, path, text );  
 
    List attributes = element.getAttributes();
    for( int i = 0; i < attributes.size(); i++ )
    {
      Attribute attribute = (Attribute)( attributes.get( i ) );
      addVariable( variables, path + "/@" + attribute.getName(), attribute.getValue() );
    }

    List children = element.getChildren();
    suffix = "";

    for( int i = 0, nr = 1; i < children.size(); i++ )
    {
      Element child = (Element)( children.get( i ) );
      if( i > 0 )
      {
        Element before = (Element)( children.get( i - 1 ) ); 
        if( child.getName().equals( before.getName() ) )
          suffix = "[" + String.valueOf( ++nr ) + "]";  
        else 
          nr = 1;
      }
      setVariablesFromElement( variables, child, path + "/", suffix );
    }
  }

  private static void addVariable( List variables, String path, String text )
  {
    if( ( text == null ) || ( text.trim().length() == 0 ) ) return;

    MCREditorServlet.logger.debug( "Editor XML input " + path + "=" + text );

    Element var = new Element( "source-variable" );
    var.setAttribute( "name", path );
    var.setAttribute( "value", text );
    variables.add( var );
  }
}

