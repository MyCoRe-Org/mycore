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

import java.util.List;

import org.jdom.Element;
import org.mycore.common.MCRCache;

public class MCREditorDefReader
{
  /**
   * Reads that editor definition from the given 
   * webpage that has the given ID
   **/
  static Element readDef( String webpage, String editorRefID )
  {
    Element definition = new Element( "definition" );
    definition.addContent( resolveInclude( "webapp:" + webpage, editorRefID ) );
    return definition;
  }
  
  /**
   * Returns that direct or indirect child element of the given element, thats ID 
   * attribute has the given value.
   * 
   * @param id the value the ID attribute must have
   * @param parent the element to start searching with
   * @return the child element that has the given ID, or null if no such element exists.
   */
  protected static Element findElementByID( String id, Element parent )
  {
    List children = parent.getChildren();
    for( int i = 0; i < children.size(); i++ )
    {
      Element child = (Element)( children.get( i ) );
      if( id.equals( child.getAttributeValue( "ID" ) ) )
        return child;
      else
      {
        Element found = findElementByID( id, child );
        if( found != null ) return found;
      }
    }
    return null;
  }  
  
  /** 
   * A cache of reusable resolved includes. Key is URI and IDREF, 
   * cached value is a container element that holds the resolved includes. 
   */
  protected static MCRCache includesCache = new MCRCache( 100 );
  
  /**
   * Resolves the uri and idref to a list of elements to include.
   * If idref is null or empty, the root element at the given URI is used and its 
   * children are returned as includes. If idref is not empty, the direct or indirect 
   * child element of the element at URI is looked up, and its children are used.
   * All contained includes are resolved recursively, so the returned result is 
   * include-free.  
   * 
   * @param uri the URI where to get the elements from
   * @param idref if not null, include contents of element with that ID
   * @return a List of resolved, included elements
   */
  protected static List resolveInclude( String uri, String idref )
  {
    if( idref == null ) idref = "";
    
    // May be the included resource is already in the cache
    String key = idref + "@" + uri;
    Element cached = (Element)( includesCache.get( key ) );
    if( cached != null) 
    {
      MCREditorServlet.logger.info( "Resolved include from cache: " + key );
      return cached.cloneContent();
    }

    MCREditorServlet.logger.info( "Resolving include from URI: " + key );
    // Get the elements to include from uri
    Element container = MCREditorResolver.readXML( uri );
    
    // If idref is given, include contents of element with that id
    if( idref.length() > 0 ) 
      container = findElementByID( idref, container );
    
    // Recursively resolve include elements in the included resource
    resolveIncludes( container );

    // If container/@cacheable != "false", cache this include for later reuse
    boolean doNotCache = "false".equals( container.getAttributeValue( "cacheable" ) );
    if( ! doNotCache ) 
    {
      MCREditorServlet.logger.info( "Resolved include is cacheable, putting into cache: " + key );
      includesCache.put( key, container );
    }

    return container.cloneContent();
  }

  /**
   * Recursively removes include elements that are direct or indirect
   * children of the given container element and replaces them with the 
   * included resource. Includes that may be contained in included 
   * resources are recursively resolved, too.
   *  
   * @param container The element where to start resolving includes
   **/
  protected static void resolveIncludes( Element container )
  {
    List children = container.getContent();
    
    for( int i = 0; i < children.size(); i++ )
    {
      Element child = (Element)( children.get( i ) );
      if( child.getName().equals( "include" ) )
      {
        children.remove( child );
        String idref = child.getAttributeValue( "idref" );
        String uri   = child.getAttributeValue( "uri"   );
        List includes = resolveInclude( uri, idref );
        children.addAll( i--, includes );
      }
      else resolveIncludes( child );
    }
  }
}
