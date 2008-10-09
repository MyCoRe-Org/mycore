/*
 * $Revision$ 
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.editor;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLHelper;

/*
 * Reads in definition of editor forms like search mask and data input forms.
 * Resolves includes and prepares editor form for output. 
 */
public class MCREditorDefReader 
{
  private final static Logger LOGGER = Logger.getLogger( MCREditorDefReader.class );
  
  private Element editor; 
  
  HashMap<String,Element> id2component = new HashMap<String,Element>();
  HashMap<Element,String> referencing2ref = new HashMap<Element,String>();

  /**
   * Reads the editor definition from the given URI
   * 
   * @param validate
   *            if true, validate editor definition against schema
   */
  MCREditorDefReader( String uri, String ref, boolean validate ) 
  {
    long time = System.nanoTime();
    
    Element include = new Element( "include" ).setAttribute( "uri", uri );
    if( ( ref != null ) && ( ref.length() > 0 ) ) include.setAttribute( "ref", ref );
    
    editor = new Element( "editor" );
    editor.setAttribute( "id", ref );
    editor.addContent( include );
    resolveChildren( editor );
    resolveReferences();
    if( validate ) validate( uri, ref );
    
    time =  ( System.nanoTime() - time ) / 1000000;
    LOGGER.info( "Finished reading editor definition in " + time + " ms" );
  }
  
  private void validate( String uri, String ref )
  {
    if( ( ref != null ) && ( ref.length() > 0 ) ) uri += "#" + ref;
    LOGGER.info( "Validating editor " + uri + "..." );
    
    Document doc = new Document( editor );
    Namespace xsi = Namespace.getNamespace( "xsi", "http://www.w3.org/2001/XMLSchema-instance" );
    editor.setAttribute( "noNamespaceSchemaLocation", "editor.xsd", xsi );

    XMLOutputter xout = new XMLOutputter();
    xout.setFormat( Format.getPrettyFormat() );
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try 
    {
      xout.output( doc, baos );
      baos.close();
      MCRXMLHelper.parseXML( baos.toByteArray(), true );
    } 
    catch( Exception ex ) 
    {
      String msg = "Error validating editor " + uri;
      LOGGER.error( msg );
      throw new MCRConfigurationException( msg, ex );
    }

    editor.detach();
    editor.removeAttribute("noNamespaceSchemaLocation", xsi);
    LOGGER.info( "Validation succeeded." );
  }

  /**
   * Returns the complete editor with all references resolved 
   */
  Element getEditor()
  { return editor; }
  
  /**
   * Recursively removes include elements that are direct or indirect children
   * of the given container element and replaces them with the included
   * resource. Includes that may be contained in included resources are
   * recursively resolved, too.
   * 
   * @param element
   *            The element where to start resolving includes
   */
  private boolean resolveIncludes( Element element )
  {
    boolean replaced = false;
    
    String ref = element.getAttributeValue( "ref", "" );
    if( element.getName().equals( "include" ) )
    {
      String uri = element.getAttributeValue( "uri" );
      if( uri != null )
      {
        LOGGER.info( "Including " + uri + ( (ref.length() > 0) ? "#" + ref : "" ) ); 
        Element parent = element.getParentElement();
        int pos = parent.indexOf( element );
      
        Element container = MCRURIResolver.instance().resolve( uri );
        List<Content> found;
        
        if( ref.length() == 0 )
          found = container.cloneContent();
        else
        {
          found = findContent( container, ref );
          ref = "";
        }
        replaced = true;
        parent.addContent( pos, found );
        element.detach();
      }
    }
    else
    {
      String id = element.getAttributeValue( "id", "" );
      if( id.length() > 0 ) id2component.put( id, element );
      
      setDefaultAttributes( element );
      fixConditionedVariables( element );
      resolveChildren( element );
    }

    if( ref.length() > 0 ) referencing2ref.put( element, ref );
    return replaced;
  }
  
  private void resolveChildren( Element parent )
  {
    for( int i = 0; i < parent.getContentSize(); i++ )
    {
      Content child = parent.getContent( i );
      if( ( child instanceof Element ) && resolveIncludes( (Element)child ) ) i--;
    }
  }

  private List<Content> findContent( Element candidate, String id )
  {
    if( id.equals( candidate.getAttributeValue( "id" ) ) )
      return candidate.cloneContent();
    else
    {
      for( Element child : (List<Element>)( candidate.getChildren() ) )
      { 
        List<Content> found = findContent( child, id );
        if( found != null ) return found;
      }
      return null;
    }
  }
  
  /**
   * Returns that direct or indirect child element of the given element, thats
   * ID attribute has the given value.
   * 
   * @param id
   *            the value the ID attribute must have
   * @param candidate
   *            the element to start searching with
   * @return the element below that has the given ID, or null if no such
   *         element exists.
   */
  static Element findElementByID( String id, Element candidate )
  {
    if( id.equals( candidate.getAttributeValue( "id" ) ) )
      return candidate;
    else
    {
      for( Element child : (List<Element>)( candidate.getChildren() ) )
      { 
        Element found = findElementByID( id, child );
        if( found != null ) return found;
      }
      return null;
    }
  }
  
  /**
   * Recursively resolves references by the @ref attribute and
   * replaces them with the referenced component.
   */
  private void resolveReferences()
  {
    for( Iterator it = referencing2ref.keySet().iterator() ; it.hasNext(); )
    {
      Element referencing = (Element)(it.next());
      String id = referencing2ref.get( referencing );
      LOGGER.debug( "Resolving reference to " + id ); 

      Element found = id2component.get( id );
      if( found == null )
      {
        String msg = "Reference to component " + id + " could not be resolved";
        throw new MCRConfigurationException( msg );
      }
      
      String name = referencing.getName();
      referencing.removeAttribute( "ref" );
      it.remove();
      
      if( name.equals( "cell" ) || name.equals( "repeater" ) )
      {
        if( found.getParentElement().getName().equals( "components" ) )
          referencing.addContent( 0, found.detach() );
        else
          referencing.addContent( 0, (Element)(found.clone()) );
      }
      else if( name.equals( "panel" ) )
      {
        if( referencing2ref.containsValue( id ) )
          referencing.addContent( 0, found.cloneContent() );
        else
        {
          found.detach();
          List<Content> content = found.getContent();
          for( int i = 0; ! content.isEmpty(); i++ )
          {
            Content child = content.remove( 0 );
            referencing.addContent( i, child );
          }
        }
      }
      else if( name.equals( "include" ) )
      {
        Element parent = referencing.getParentElement();
        int pos = parent.indexOf( referencing );
        referencing.detach();
        
        if( referencing2ref.containsValue( id ) )
          parent.addContent( pos, found.cloneContent() );
        else
        {
          found.detach();
          List<Content> content = found.getContent();
          for( int i = pos; ! content.isEmpty(); i++ )
          {
            Content child = content.remove( 0 );
            parent.addContent( i, child );
          }
        }
      }
    }

    Element components = editor.getChild( "components" );
    String root = components.getAttributeValue( "root" );
    components.removeAttribute( "root" );
    
    for( int i = 0; i < components.getContentSize(); i++ )
    {
      Content child = components.getContent( i );
      if( ! ( child instanceof Element ) ) continue;
      if( ((Element)child).getName().equals( "headline" ) ) continue;
      if( ! root.equals( ((Element)child).getAttributeValue( "id" ) ) )
        components.removeContent( i-- );
    }  
  }

  /**
   * This map contains default attribute values to set for a given element name
   */
  private static HashMap<String,Properties> defaultAttributes = new HashMap<String,Properties>();
  
  static
  {
    defaultAttributes.put( "cell", new Properties() );
    defaultAttributes.get( "cell" ).setProperty( "row", "1" );
    defaultAttributes.get( "cell" ).setProperty( "col", "1" );
    defaultAttributes.get( "cell" ).setProperty( "class", "editorCell" );
    defaultAttributes.put( "headline", new Properties() );
    defaultAttributes.get( "headline" ).setProperty( "class", "editorHeadline" );
    defaultAttributes.put( "repeater", new Properties() );
    defaultAttributes.get( "repeater" ).setProperty( "class", "editorRepeater" );
    defaultAttributes.get( "repeater" ).setProperty( "min", "1" );
    defaultAttributes.get( "repeater" ).setProperty( "max", "10" );
    defaultAttributes.put( "panel", new Properties() );
    defaultAttributes.get( "panel" ).setProperty( "class", "editorPanel" );
    defaultAttributes.put( "editor", new Properties() );
    defaultAttributes.get( "editor" ).setProperty( "class", "editor" );
    defaultAttributes.put( "helpPopup", new Properties() );
    defaultAttributes.get( "helpPopup" ).setProperty( "class", "editorButton" );
    defaultAttributes.put( "textfield", new Properties() );
    defaultAttributes.get( "textfield" ).setProperty( "class", "editorTextfield" );
    defaultAttributes.put( "textarea", new Properties() );
    defaultAttributes.get( "textarea" ).setProperty( "class", "editorTextarea" );
    defaultAttributes.put( "file", new Properties() );
    defaultAttributes.get( "file" ).setProperty( "class", "editorFile" );
    defaultAttributes.put( "password", new Properties() );
    defaultAttributes.get( "password" ).setProperty( "class", "editorPassword" );
    defaultAttributes.put( "subselect", new Properties() );
    defaultAttributes.get( "subselect" ).setProperty( "class", "editorButton" );
    defaultAttributes.put( "submitButton", new Properties() );
    defaultAttributes.get( "submitButton" ).setProperty( "class", "editorButton" );
    defaultAttributes.put( "cancelButton", new Properties() );
    defaultAttributes.get( "cancelButton" ).setProperty( "class", "editorButton" );
    defaultAttributes.put( "button", new Properties() );
    defaultAttributes.get( "button" ).setProperty( "class", "editorButton" );
    defaultAttributes.put( "list", new Properties() );
    defaultAttributes.get( "list" ).setProperty( "class", "editorList" );
    defaultAttributes.put( "checkbox", new Properties() );
    defaultAttributes.get( "checkbox" ).setProperty( "class", "editorCheckbox" );
  }
  
  /**
   * Sets default attribute values for the given element, if any
   */
  private void setDefaultAttributes( Element element ) 
  {
    Properties defaults = defaultAttributes.get( element.getName() );
    if( defaults == null ) return;
    for( Iterator it = defaults.keySet().iterator() ; it.hasNext(); )
    {
      String key = (String)( it.next() );
      if( element.getAttribute( key )  == null )
        element.setAttribute( key, defaults.getProperty( key ) );
    }
  }

  /** 
   * Transforms @var attribute values that have a condition like
   * title[@type='main'] into escaped internal syntax
   * title__type__main 
   */
  private void fixConditionedVariables( Element element )
  {
    String var = element.getAttributeValue( "var", "" ); 
    if( var.contains( "[@" ) )
    {
      var = var.replace( "[@", "__" );
      var = var.replace( "='", "__" );
      var = var.replace( "']", "" );
      element.setAttribute( "var", var );
    }
  }
}
