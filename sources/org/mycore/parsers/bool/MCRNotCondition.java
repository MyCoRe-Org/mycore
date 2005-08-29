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

package org.mycore.parsers.bool;

import org.jdom.Attribute;
import org.jdom.Element;

/**
 * @author Frank Lützenkirchen
 **/
public class MCRNotCondition implements MCRCondition
{
    private MCRCondition child;
    
    public MCRNotCondition( MCRCondition child )
    { this.child = child; }
    
    public MCRCondition getChild()
    { return child; }
    
    public String toString()
    { return "not (" + child + ")"; }

    public boolean evaluate(Object o)
    { return !child.evaluate(o); }
    
    public Element toXML()
    {
        Element not = new Element( "not" );
        not.addContent( child.toXML() );
        return not;
    }
    
    public Element info()
    {
        Element el = new Element("info");
        el.setAttribute(new Attribute("type", "not"));
        el.setAttribute(new Attribute("children", "1"));
        return el;
    }
    
    public void accept(MCRConditionVisitor visitor)
    {
        visitor.visitType(info());
        child.accept(visitor);
    }

}
