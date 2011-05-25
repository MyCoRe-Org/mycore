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

package org.mycore.frontend.basket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.xml.MCRURIResolver;

/**
 * Implements a basket of entries.
 * Each entry has a unique ID and contains an XML element.
 * When adding entries, XML is read from any URI using URIResolver.   
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRBasket
{
  /** The root element if the basket data */
  private Element root;

  /** Map of entries, key is the entry ID */
  private Map<String, Element> entryMap = new HashMap<String, Element>();

  /**
   * Creates a new basket of the given type.
   * 
   * @type a unique ID identifying this type of basket.
   */
  public MCRBasket( String type )
  {
    root = new Element( "basket" );
    root.setAttribute( "type", type );
    new Document( root );
  }

  /**
   * Checks if the basket contains an entry with the given ID.
   */
  public boolean contains( String id )
  {
    return entryMap.containsKey( id );
  }

  /**
   * Returns the entry xml element with the given ID. 
   */
  public Element get( String id )
  {
    return entryMap.get( id );
  }
  
  /**
   * Returns a list of all entries.
   */
  public List<Element> getEntries()
  {
    return root.getChildren( "entry" );
  }

  /**
   * Reads an XML element from the given URI using MCRURIResolver, 
   * and stores a copy of it in the basket under the given ID.  
   * 
   * @param id, the ID of the entry, for example a document ID.
   * @param uri the URI to read the xml data from, using URIResolver.
   */
  public void add( String id, String uri )
  {
    if( contains( id ) ) return;

    Element entry = buildEntry( id, uri );
    entryMap.put( id, entry );
    root.addContent( entry );
  }

  /**
   * Builds a new entry element.
   */
  private Element buildEntry( String id, String uri )
  {
    Element entry = new Element( "entry" );
    entry.setAttribute( "id", id );

    Element content = MCRURIResolver.instance().resolve( uri );
    entry.addContent( (Element)( content.clone() ) );

    return entry;
  }

  /**
   * Removes the entry with the given ID.
   */
  public void remove( String id )
  {
    Element entry = get( id );
    if( entry != null ) entry.detach();
    entryMap.remove( id );
  }

  /**
   * Moves the entry with the given ID one position up
   * in basket document order.
   */
  public void up( String id )
  {
    move( id, -1 );
  }

  /**
   * Moves the entry with the given ID one position down
   * in basket document order.
   */
  public void down( String id )
  {
    move( id, 1 );
  }

  /**
   * Changes the position of the entry with the given ID 
   * by moving it the given number of entries up or down.
   */
  public void move( String id, int change )
  {
    Element entry = get( id );
    List<Element> children = root.getChildren();

    int posOld = children.indexOf( entry );
    int posNew = posOld + change;
    if( ( posNew < 0 ) || ( posNew > children.size() - 1 ) ) return;

    children.remove( posOld );
    children.add( posNew, entry );
  }

  /**
   * Removes all entries from the basket.
   */
  public void clear()
  {
    root.removeContent();
    entryMap.clear();
  }

  /**
   * Sets a comment for the given entry.
   */
  public void setComment( String id, String comment )
  {
    Element entry = get( id );
    entry.removeChildren( "comment" );
    if( ( comment != null ) && ( ! comment.isEmpty() ) )
      entry.addContent( new Element( "comment" ).setText( comment ) );
  }

  /**
   * Returns an XML representation of this basket.
   */
  public Document getXML()
  {
    return root.getDocument();
  }
}
