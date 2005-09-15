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

/**
 * Represents a single search field as defined by searchfields.xml
 * 
 * @author Frank Lützenkirchen
 **/
public class MCRSearchField
{
  /** Sort this field in ascending order **/
  public final static boolean ASCENDING  = true;
  
  /** Sort this field in descending order **/
  public final static boolean DESCENDING = false;
  
  /** Name of the field as defined in searchfields.xml **/
  public String name;
  
  /** Data type of the field as defined in fieldtypes.xml **/
  public String type = "text";
  
  /** Sort order of this field if it is part of the sort criteria **/
  public boolean order = ASCENDING;
  
  public String getName()
  { return name;}
  
  public void setName( String name )
  { this.name = name; }
  
  public boolean getSortOrder()
  { return order; }
  
  public void setSortOrder( boolean order )
  { this.order = order; }
  
  public String getDataType()
  { return type; }
  
  public void setDataType( String type )
  { this.type = type; }
}
