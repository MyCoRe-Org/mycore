/**
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

package org.mycore.services.fieldquery;

import java.util.List;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Iterator;

import org.jdom.Attribute;
import org.jdom.Element;

/**
 * Represents a single result hit of a query
 * 
 * @author Arne Seifert
 * @author Frank Lützenkirchen
 **/
public class MCRHit
{
  /** The ID of this object that matched the query **/
  private String id;
  
  /** Data for sorting this hit or hit metadata like rank, score **/
  private Properties sortData = new Properties();
  
  /** List of Properties objects that contain technical hit metadata from the backend searcher **/
  private List metaData = new LinkedList();
  
  /**
   * Creates a new result hit with the given object ID
   * 
   * @param id the ID of the object that matched the query
   **/
  public MCRHit(String id)
  { this.id = id; }
    
  /**
   * Returns the ID of the object that matched the query
   * 
   * @return the ID of the object that matched the query
   **/
  public String getID()
  { return id; }
  
  /**
   * Returns hit metadata for sorting or ranking
   * 
   * @return hit metadata as name-value pairs of Strings
   **/
  public Properties getSortData()
  { return sortData; }
  
  /**
   * Set data value of the hit
   * 
   * @param key the name of the data value
   * @param value the value as string
   **/
  public void addSortData(String key, String value)
  { if( ! sortData.containsKey( key ) ) sortData.put( key, value ); }

  /**
   * Adds metadata about the hit. This metadata comes from the searcher and 
   * may contain additional information like score, rank, derivate ID, file path,
   * position where something was found in the content etc. There may be more than 
   * just one set of such metadata because the same criteria may have been found
   * in more than just one MCRFile of the same MCRObject etc. 
   * 
   * @param metadata a Properties object containing hit metadata as name-value pairs
   **/
  public void addMetaData( Properties metadata )
  { if( metadata != null ) metaData.add( metadata ); }
  
  /**
   * Returns technical metadata about the hit that was provided by the searcher.
   * 
   * @return a List of Properties objects containing hit metadata as name-value pairs
   **/
  public List getMetaData()
  { return metaData; }

  /**
   * Combines the data of two MCRHit objects with the same ID, but
   * from different searchers result sets by copying the sort data and
   * hit metadata of both objects. 
   * 
   * @param a the first hit from the first searcher
   * @param b the other hit from the other searcher
   * @return
   **/
  static MCRHit buildMergedHitData( MCRHit a, MCRHit b )
  {
    // If there is nothing to merge, return existing hit
    if( b == null ) return a;
    if( a == null ) return b;
    
    // Copy ID
    MCRHit c = new MCRHit( a.getID() );
    
    // Copy sort data
    c.sortData = ( a.sortData.isEmpty() ? b.sortData : a.sortData );
    
    // Copy metadata sets
    c.metaData.addAll( a.metaData );
    c.metaData.addAll( b.metaData );
    
    return c;
  }
  
  /**
   * Creates an XML representation of this hit and its data
   * 
   * @return the hit data as XML
   **/
  public Element buildXML()
  {
    Element el = new Element("mcrhit");
    el.setAttribute(new Attribute("mcrid", this.id));
    
    if( ! sortData.isEmpty() )
      addDataProperties( el, "sortData", sortData );

    if( ! metaData.isEmpty() )
    {
      for( int i = 0; i < metaData.size(); i++ )
      {
        Properties prop = (Properties)( metaData.get(i) );
        if( ! prop.isEmpty() ) 
          addDataProperties( el, "metaData", prop );
      }
    }
    
    return el;
  }

  private void addDataProperties( Element parent, String name, Properties data )
  {
    Element coll = new Element( name );
    parent.addContent(coll);
    
    Iterator it = data.keySet().iterator();   
    while(it.hasNext())
    {
      String key = (String) it.next();
      
      Element d = new Element( "data" );
      coll.addContent( d );
      d.setAttribute( "name", key );
      d.addContent( data.getProperty(key) );
    }
  }
}
