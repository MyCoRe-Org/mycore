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

package org.mycore.frontend.editor;

import java.io.*;
import java.util.*;
import java.net.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.jdom.*;

import org.mycore.common.*;

/**
 * Represents a HTTP request to edit an XML document in EditorServlet.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 **/
class MCREditorRequest
{
  protected static MCRCache editorCache = new MCRCache( 20 );

  HttpServletRequest  request;
  HttpServletResponse response;
  ServletContext      context;
  List                parameters;
  Vector              variables;
  Vector              repeats;
  String              editorID;
  String              objectID;
  Document            editor;

  MCREditorRequest( HttpServletRequest  request,
                    HttpServletResponse response,
                    ServletContext      context )
    throws Exception
  {
    this.request    = request;
    this.response   = response;
    this.context    = context;
    this.parameters = new ArrayList();
    this.variables  = new Vector();
    this.repeats    = new Vector();
    this.editorID   = request.getParameter( "editor" );
    this.objectID   = request.getParameter( "id"     );

    if( objectID == null ) objectID = "0";

    editor = loadEditorXML();

    for( Enumeration e = request.getParameterNames(); e.hasMoreElements(); )
      parameters.add( e.nextElement() );

    Collections.sort( parameters );
  }

  void processRequest()
    throws Exception
  {
    String action = request.getParameter( "action" );

    if( "load".equals( action ) ) 
    {
      MCREditorXMLSource source = getXMLSource();
      if( ! source.isEditingAllowed( request, context, objectID ) )
      {
        String msg = "You are not authorized to edit this object!";
        response.sendError( response.SC_FORBIDDEN, msg );
      }
      else
      {
        Document doc = source.loadDocument( objectID, context );
        if( doc == null )
        {
          String msg = "There is no object to edit with ID = " + objectID;
          response.sendError( response.SC_NOT_FOUND, msg );
        }
        else
        {
          setVariablesFromElement( doc.getRootElement(), "", "" );
          setRepeatsFromVariables();
          sendEditorPageXML();
        }
      }
    }
    else if( "new".equals( action ) )
    {
      sendEditorPageXML();
    }
    else if( "submit".equals( action ) ) 
    {
      String button = "";
      
      for( Enumeration e = request.getParameterNames(); e.hasMoreElements(); )
      {
        String name = (String)( e.nextElement() );
        if( name.startsWith( "s-" ) || name.startsWith( "p-" ) ||
            name.startsWith( "m-" ) || name.startsWith( "u-" ) ||
            name.startsWith( "d-" ) )
        {
          button = name;
          break;
        }
      }  
      
      if( button.startsWith( "s-" ) )
      {
        setVariablesFromRequest();
        Document doc = buildOutputXML();

        MCREditorXMLTarget target = getXMLTarget();
        if( ! target.isEditingAllowed( request, context, objectID ) )
        {
          String msg = "You are not authorized to edit this object!";
          response.sendError( response.SC_FORBIDDEN, msg );
        }
        else
        {
          String url = target.saveDocument( doc, objectID, context );
          if( url == null )
          {
            request.setAttribute( "MCRLayoutServlet.Input.JDOM", doc );
            RequestDispatcher rd = context.getNamedDispatcher( "MCRLayoutServlet" );
            rd.forward( request, response );
          }
          else
          {
            response.sendRedirect( url );
            return;
          }
        }
      }
      else
      {
        setVariablesFromRequest();
        setRepeatsFromRequest();

        int pos = button.lastIndexOf( "-" );
  
               action = button.substring( 0, 1 );
        String path   = button.substring( 2, pos );
        int    nr     = Integer.parseInt( button.substring( pos + 1, button.length() - 2 ) );

        if( "p".equals( action ) )
          doPlus( path, nr );
        else if( "m".equals( action ) )
          doMinus( path, nr );
        else if( "u".equals( action ) )
          doUp( path, nr );
        else if( "d".equals( action ) )
          doUp( path, nr + 1 );
    
        sendEditorPageXML();
      }  
    }
    else response.sendError( response.SC_BAD_REQUEST, "missing parameter 'action'" );
  }

  MCREditorXMLSource getXMLSource()
    throws Exception
  {
    String classname = editor.getRootElement().getChild( "workflow" )
                       .getChild( "source" ).getAttributeValue( "class" );
    Object instance = Class.forName( classname ).newInstance();
    return (MCREditorXMLSource)( instance );
  }

  MCREditorXMLTarget getXMLTarget()
    throws Exception
  {
    String classname = editor.getRootElement().getChild( "workflow" )
                       .getChild( "target" ).getAttributeValue( "class" );
    Object instance = Class.forName( classname ).newInstance();
    return (MCREditorXMLTarget)( instance );
  }

  void addVariable( String path, String value )
  {
    if( value.trim().length() == 0 ) return;
    variables.addElement( new MCREditorVariable( path, value ) );
    System.out.println( "get var " + path + " = " + value );
  }
    
  MCREditorVariable getVariable( int index )
  { return (MCREditorVariable)( variables.elementAt( index ) ); }
    
  void setVariablesFromRequest()
  {
    for( int i = 0; i < parameters.size(); i++ )
    {
      String parameter = (String)( parameters.get( i ) );
      if( parameter.startsWith( "v-" ) )
      {
        String[] values = request.getParameterValues( parameter );
        int      pos    = parameter.indexOf( "-", 2 );
        String   path   = parameter.substring( pos + 1 ); // remove ordernr
          
        for( int j = 0; ( values != null ) && ( j < values.length ); j++ )
        {
          String autofill = request.getParameter( "a-" + path );

          if( ( autofill != null ) && ( autofill.length() > 0 ) && autofill.trim().equals( values[ j ].trim() ) )
            continue; // remove unchanged autofill
          else
            addVariable( path, values[ j ] );
        }
      }	
    }  
  }
    
  void setVariablesFromElement( Element element, String prefix, String suffix )
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

  void setRepeatsFromVariables()
  {
    Hashtable maxtable = new Hashtable();
     
    for( int i = 0; i < variables.size(); i++ )
    {
      String[] path = getVariable( i ).getPathElements();
      String prefix = path[ 0 ];
        
      for( int j = 1; j < path.length; j++ )
      {
        String name = path[ j ];
        int pos1 = name.lastIndexOf( "[" );
        int pos2 = name.lastIndexOf( "]" );
          
        if( pos1 != -1 )
        {
          String elem = name.substring( 0, pos1 );
          String num  = name.substring( pos1 + 1, pos2 );
          String key  = prefix + "/" + elem;
          
          int numNew = Integer.parseInt( num );
        
          if( maxtable.containsKey( key ) )
          {
            int numOld = Integer.parseInt( (String)( maxtable.get( key ) ) );
            maxtable.remove( key );
            numNew = Math.max( numOld, numNew );
          }
          
          maxtable.put( key, String.valueOf( numNew ) );
        }          
        prefix = prefix + "/" + name;
      }
    }
      
    for( Enumeration e = maxtable.keys(); e.hasMoreElements(); )
    {
      String path  = (String)( e.nextElement() );
      String value = (String)( maxtable.get( path ) );
      repeats.addElement( new MCREditorVariable( path, value ) );
      System.out.println( "get num " + path + " = " + value );
    }
  }
    
  void setRepeatsFromRequest()
  {
    for( int i = 0; i < parameters.size(); i++ )
    {
      String parameter = (String)( parameters.get( i ) );
      if( parameter.startsWith( "n-" ) )
      {
        String value = request.getParameter( parameter );
        repeats.addElement( new MCREditorVariable( parameter.substring( 2 ), value ) );
        System.out.println( "get num " + parameter.substring( 2 ) + " = " + value );
      }	
    }  
  }
    
  Document loadEditorXML()
    throws Exception
  {
    String name = "/WEB-INF/editor/editor-" + editorID + ".xml";
    String path = context.getRealPath( name );
    File   file = new File( path );
    long   time = file.lastModified();

    Document editor = (Document)( editorCache.getIfUpToDate( path, time ) );
    if( editor == null )
    {
      System.out.println( name + " has to be built from file" );      
      editor = new org.jdom.input.SAXBuilder().build( file );
      editorCache.put( path, editor );
    }

    return editor;
  }

  void sendEditorPageXML()
    throws Exception
  {  
    Element editorRoot = editor.getRootElement();
    Element components = (Element)( editorRoot.getChild( "components" ).clone() );
    Element title      = (Element)( editorRoot.getChild( "title"      ).clone() );

    Element root = new Element( "editorpage" );
    root.addContent( title                   );
    root.addContent( buildVariablesElement() );
    root.addContent( buildRepeatsElement()   );
    root.addContent( components              );
      
    Document doc = new Document( root );
    doc.setDocType( new DocType( "editorpage" ) );

    request.setAttribute( "XSL.EditorID", editorID );
    request.setAttribute( "XSL.ObjectID", objectID );
    
    request.setAttribute( "MCRLayoutServlet.Input.JDOM", doc );
    RequestDispatcher rd = context.getNamedDispatcher( "MCRLayoutServlet" );
    rd.forward( request, response );
  }

  Element buildVariablesElement()
  {
    Element ve = new Element( "variables" );
    for( int i = 0; i < variables.size(); i++ )
    {
      Element variable = new Element( "variable" );
      variable.setAttribute( "path", getVariable( i ).getPath() );
      variable.addContent( getVariable( i ).getValue() );
      ve.addContent( variable );
      System.out.println( "set var " + getVariable( i ).getPath() + " = " + getVariable( i ).getValue() );
    }
    return ve;
  }
    
  Element buildRepeatsElement()
  {
    Element vr = new Element( "repeats" );
    for( int i = 0; i < repeats.size(); i++ )
    {
      Element repeat = new Element( "repeat" );
      MCREditorVariable var = (MCREditorVariable)( repeats.elementAt( i ) );
      repeat.setAttribute( "path", var.getPath()  );
      repeat.setAttribute( "num",  var.getValue() );
      vr.addContent( repeat );
      System.out.println( "set num " + var.getPath() + " = " + var.getValue() );
    }
    return vr;
  }

  void doPlus( String prefix, int nr )
  {
    changeRepeatNumber( prefix, +1 );
    changeVariablesAndRepeats( prefix, nr, +1 );
  }
        
  void doMinus( String prefix, int nr )
  {
    changeRepeatNumber( prefix, -1 );

    String prefix2; 
    if( nr > 1 ) 
      prefix2 = prefix + "[" + nr + "]";  
    else
      prefix2 = prefix;  

    for( int i = 0; i < variables.size(); i++ )
    {
      String path = getVariable( i ).getPath();

      if( path.startsWith( prefix2 + "/"  ) || path.equals( prefix2 ) )
        variables.removeElementAt( i-- );
    }
 
    for( int i = 0; i < repeats.size(); i++ )
    {
      String path = ( (MCREditorVariable)( repeats.elementAt( i ) ) ).getPath();

      if( path.startsWith( prefix2 + "/"  ) )
        repeats.removeElementAt( i-- );
    }

    changeVariablesAndRepeats( prefix, nr, -1 );
  }
    
  void doUp( String prefix, int nr )
  {
    String prefix1 = prefix + ( nr > 2 ? "[" + String.valueOf( nr - 1 ) + "]" : "" );
    String prefix2 = prefix + "[" + String.valueOf( nr ) + "]";
      
    for( int i = 0; i < variables.size(); i++ )
    {
      MCREditorVariable var = getVariable( i );
      String path = var.getPath();
        
      if( path.startsWith( prefix1 + "/"  ) || path.equals( prefix1 ) )
      {
        String rest = path.substring( prefix1.length() );
        var.setPath( prefix2 + rest );
      }  
      else if( path.startsWith( prefix2 ) || path.equals( prefix2 ) )
      {
        String rest = path.substring( prefix2.length() );
        var.setPath( prefix1 + rest );
      }  
    }

    for( int i = 0; i < repeats.size(); i++ )
    {
      MCREditorVariable var = (MCREditorVariable)( repeats.get( i ) );
      String path = var.getPath();

      if( path.startsWith( prefix1 + "/" ) )
      {
        String rest = path.substring( prefix1.length() );
        var.setPath( prefix2 + rest );
      }
      else if( path.startsWith( prefix2 + "/" ) )
      {
        String rest = path.substring( prefix2.length() );
        var.setPath( prefix1 + rest );
      }
    }
  }
        
  void changeRepeatNumber( String prefix, int change )
  {
    for( int i = 0; i < repeats.size(); i++ )
    {
      MCREditorVariable var = (MCREditorVariable)( repeats.elementAt( i ) );
      if( var.getPath().equals( prefix ) )
      {
        int value = Integer.parseInt( var.getValue() ) + change;
        if( value == 0 ) value = 1;
        var.setValue( String.valueOf( value ) );
        return;
      }
    }
  }  
      
  void changeVariablesAndRepeats( String prefix, int nr, int change )
  {
    ArrayList list = new ArrayList();
    list.addAll( variables );
    list.addAll( repeats   );
      
    for( int i = 0; i < list.size(); i++ )
    {
      MCREditorVariable var = (MCREditorVariable)( list.get( i ) );
      String path = var.getPath();
        
      if( ! path.startsWith( prefix + "[" ) ) continue;
       
      String rest = path.substring( prefix.length() + 1 );
        
      int pos = rest.indexOf( "]" );
      int num = Integer.parseInt( rest.substring( 0, pos ) );
        
      if( num > nr )
      {
        num += change;

        StringBuffer newpath = new StringBuffer( prefix );
        if( num > 1 ) 
          newpath.append( "[" ).append( num ).append( "]" );
        newpath.append( rest.substring( pos + 1 ) );

        var.setPath( newpath.toString() );
      }
    }
  }
    
  Document buildOutputXML()
    throws Exception
  {
    Element root = new Element( getVariable( 0 ).getPathElements()[ 0 ] );

    for( int i = 0; i < variables.size(); i++ )
    {
      Element  parent   = root;
      String[] elements = getVariable( i ).getPathElements();

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
        
      String attribute = getVariable( i ).getAttributeName();
      String value     = getVariable( i ).getValue();
        
      if( attribute == null ) 
        parent.addContent( value );         
      else
        parent.setAttribute( attribute, value );
    }
      
    renameRepeatedElements( root );

    return new Document( root );
  }
    
  void saveOutputDocument( Document doc )
    throws Exception
  {
    String name = "/" + editorID + "-" + objectID + ".xml";
    String path = context.getRealPath( name );
    File   file = new File( path );
      
    OutputStream out = new FileOutputStream( file );

    org.jdom.output.XMLOutputter outputter =
      new org.jdom.output.XMLOutputter( "  ", true );

    outputter.output( doc, out );
    out.close();
  }
    
  void renameRepeatedElements( Element element )
  {
    String name = element.getName();
    int pos = name.lastIndexOf( "_XXX_" );
  
    if( pos >= 0 ) element.setName( name.substring( 0, pos ) ); 
      
    List children = element.getChildren();
    for( int i = 0; i < children.size(); i++ )
      renameRepeatedElements( (Element)( children.get( i ) ) );
  }
}

