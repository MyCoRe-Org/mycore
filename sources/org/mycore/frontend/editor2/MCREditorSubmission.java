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

import org.mycore.common.*;

import org.apache.log4j.*;
import org.apache.commons.fileupload.*;

import org.jdom.*;

import java.util.*;

/**
 * Container class that holds all data and files submitted
 * from an HTML page that contains a MyCoRe XML editor form.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 **/
public class MCREditorSubmission
{
  protected final static Logger logger = Logger.getLogger(  MCREditorServlet.class );

  private List variables = new ArrayList();
  private List files     = new ArrayList();
  
  private Document xml;
  
  private Hashtable node2file = new Hashtable();
  private Hashtable file2node = new Hashtable();
  
  private MCRRequestParameters parms;
  
  MCREditorSubmission( MCRRequestParameters parms, Element editor )
  {
    this.parms = parms;
    buildVariables( parms, editor );
    Collections.sort( variables );
    buildXML();
  }

  MCREditorSubmission( Element input )
  { setVariablesFromElement( input, "/", "" ); }

  private void setVariablesFromElement( Element element, String prefix, String suffix )
  {
    String path = prefix + element.getName() + suffix;
    String text = element.getText();

    addVariable( path, text );  
 
    List attributes = element.getAttributes();
    for( int i = 0; i < attributes.size(); i++ )
    {
      Attribute attribute = (Attribute)( attributes.get( i ) );
      addVariable( path + "/@" + attribute.getName(), attribute.getValue() );
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
      setVariablesFromElement( child, path + "/", suffix );
    }
  }

  private void addVariable( String path, String text )
  {
    if( ( text == null ) || ( text.trim().length() == 0 ) ) return;

    MCREditorServlet.logger.debug( "Editor XML input " + path + "=" + text );
    variables.add( new MCREditorVariable( path, text ) );
  }

  public MCRRequestParameters getParameters()
  { return parms; }

  public List getVariables()
  { return variables; }

  public List getFiles()
  { return files; }
  
  public Document getXML()
  { return xml; }
  
  public FileItem getFile( Object xmlNode )
  { return (FileItem)( node2file.get( xmlNode ) ); }

  public Object getXMLNode( FileItem file )
  { return file2node.get( file ); }

  private void buildVariables( MCRRequestParameters parms, Element editor )
  {
    for( Enumeration e = parms.getParameterNames(); e.hasMoreElements(); )
    {
      String name = (String)( e.nextElement() );
      if( name.startsWith( "_" ) ) continue;
      
      String[] values = parms.getParameterValues( name );
      String sortNr   = parms.getParameter( "_sortnr-" + name );
      String ID       = parms.getParameter( "_id@" + name );
      String delete   = parms.getParameter( "_delete-" + name );

      if( "true".equals( delete ) && ( parms.getFileItem( name ) == null ) ) continue;
      if( sortNr == null ) continue;
      if( ( values == null ) || ( values.length == 0 ) ) continue;

      for( int k = 0; k < values.length; k++ )
      {
        String value = values[ k ];
        if( ( value == null ) || ( value.trim().length() == 0 ) ) continue;
        String nname = ( k == 0 ? name : name + "[" + (k+1) + "]" );

        Element component = resolveComponent( editor, ID );
        FileItem file = parms.getFileItem( name );

        MCREditorVariable var = new MCREditorVariable( nname, value, sortNr, file, component );
        if( ! var.value.trim().equals( var.autofill ) )
        { 
          variables.add( var );
          if( file != null ) files.add( file );
        }
      }
    }
  }

  Element buildSourceVarXML()
  {
    Element input = new Element( "input" );
    for( int i = 0; i < variables.size(); i++ )
    {
      MCREditorVariable var = (MCREditorVariable)( variables.get( i ) );
      input.addContent( var.buildXML() );
    }
    return input;
  }
  
  private Element resolveComponent( Element parent, String ID )
  {
    if( ( ID == null ) || ( ID.trim().length() == 0 ) ) return null;
    
    List children = parent.getChildren();
    for( int i = 0; i < children.size(); i++ )
    {
      Element child = (Element)( children.get( i ) );
      if( ID.equals( child.getAttributeValue( "id" ) ) )
        return child;
      else
      {
        Element found = resolveComponent( child, ID );
        if( found != null ) return found;
      }
    }
    return null;
  }

  private void buildXML()
  {
    MCREditorVariable first = (MCREditorVariable)( variables.get( 0 ) );  
    Element root = new Element( first.pathElements[ 0 ] );

    for( int i = 0; i < variables.size(); i++ )
    {
      MCREditorVariable var = (MCREditorVariable)( variables.get( i ) ); 
      MCREditorServlet.logger.debug( var.getName() + "=" + var.getValue() );

      Element  parent   = root;
      String[] elements = var.pathElements;

      for( int j = 1; j < elements.length; j++ )
      {
        String name = elements[ j ];
        if( name.endsWith( "]" ) )
        {
          int pos = name.lastIndexOf( "[" );
          name = name.substring( 0, pos ) + "_XXX_" +
                 name.substring( pos + 1, name.length() - 1 );
        }
         
        Element child = parent.getChild( name );
        if( child == null )
        {
          child = new Element( name );
          parent.addContent( child );
        }
        parent = child;
      }
    
      Object node;
        
      if( var.attributeName == null ) 
      {
        parent.addContent( var.value );     
        node = parent;
      }
      else
      {
        parent.setAttribute( var.attributeName, var.value );
        node = parent.getAttribute( var.attributeName );
      }
    
      FileItem file = parms.getFileItem( var.name );
      if( file != null )
      {
        file2node.put( file, node );
        node2file.put( node, file );
      }
    }

    renameRepeatedElements( root );
    xml = new Document( root );
  }

  private void renameRepeatedElements( Element element )
  {
    String name = element.getName();
    int pos = name.lastIndexOf( "_XXX_" );
  
    if( pos >= 0 ) element.setName( name.substring( 0, pos ) ); 
      
    List children = element.getChildren();
    for( int i = 0; i < children.size(); i++ )
      renameRepeatedElements( (Element)( children.get( i ) ) );
  }
}
