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

package org.mycore.services.fieldquery;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;

/**
 * @author Frank Lützenkirchen
 **/
public class MCRAndOrCondition implements MCRQueryCondition
{
  private List children;
  private String type;
  
  public final static String AND = "and";
  public final static String OR  = "or";
  
  public MCRAndOrCondition( String type, MCRQueryCondition firstChild )
  {
    if( ! ( type.equals( AND ) || type.equals( OR ) ) )
      throw new IllegalArgumentException( "and|or expected as condition type" );
    
    this.type = type;
    this.children = new ArrayList();
    this.children.add( firstChild );
  }
  
  public void addChild( MCRQueryCondition child )
  { this.children.add( child ); }
  
  public List getChildren()
  { return children; }
  
  public String getType()
  { return type; }
  
  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    for( int i = 0; i < children.size(); i++ )
    {
      sb.append( "(" ).append( children.get(i) ).append( ")" );
      if( i < ( children.size() - 1 ) )
        sb.append( " " + type + " " );
    }
    return sb.toString();
  }
  
  public Element toXML()
  {
    Element cond = new Element( type );
    for( int i = 0; i < children.size(); i++ )
    {
      MCRQueryCondition child = (MCRQueryCondition)( children.get(i) );
      cond.addContent( child.toXML() );
    }
    return cond;
  }
}
