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
  private Properties map = new Properties();
  
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
  public Properties getData()
  { return map; }
  
  /**
   * Set data value of the hit
   * 
   * @param key the name of the data value
   * @param value the value as string
   **/
  public void setDataValue(String key, String value)
  { map.put( key, value ); }
  
  /**
   * Creates an XML representation of this hit and its data
   * 
   * @return the hit data as XML
   **/
  public Element buildXML()
  {
    Element el = new Element("mcrhit");
    el.setAttribute(new Attribute("mcrid", this.id));
    
    if( ! map.isEmpty() )
    {
      Iterator it = map.keySet().iterator();   
      while(it.hasNext())
      {
        String key = (String) it.next();
        el.addContent( new Element("data")
          .setAttribute("name", key)
          .setAttribute("value", map.getProperty(key) ) );
      }
    }
    return el;
  }
}
