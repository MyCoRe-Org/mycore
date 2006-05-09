/*
 * $RCSfile$
 * $Revision$ $Date$
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

package org.mycore.parsers.bool;

import java.util.LinkedList;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Element;

/**
 * @author Frank Lützenkirchen
 */
public class MCROrCondition implements MCRCondition {
    private List children;

    public MCROrCondition() {
        this.children = new LinkedList();
    }

    public MCROrCondition(MCRCondition firstChild) {
        this.children = new LinkedList();
        addChild(firstChild);
    }

    public MCROrCondition(MCRCondition firstChild, MCRCondition secondChild) {
        this.children = new LinkedList();
        addChild(firstChild);
        addChild(secondChild);
    }

    public void addChild(MCRCondition child) {
        this.children.add(child);
    }

    public List getChildren() {
        return children;
    }

    public boolean evaluate(Object o) {
        if (children.size() == 0) return true;
        for (int i = 0; i < children.size(); i++) {
            if (((MCRCondition) children.get(i)).evaluate(o)) {
                return true;
            }
        }
        
        return false;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < children.size(); i++) {
            sb.append("(").append(children.get(i)).append(")");

            if (i < (children.size() - 1)) {
                sb.append(" OR ");
            }
        }

        return sb.toString();
    }

    public Element toXML() {
        Element cond = new Element("boolean");
    	cond.setAttribute("operator", "or");        

        for (int i = 0; i < children.size(); i++) {
            MCRCondition child = (MCRCondition) (children.get(i));
            cond.addContent(child.toXML());
        }

        return cond;
    }

    public Element info() {
        Element el = new Element("info");
        el.setAttribute(new Attribute("type", "or"));
        el.setAttribute(new Attribute("children", "" + children.size()));

        return el;
    }

    public void accept(MCRConditionVisitor visitor) {
        visitor.visitType(info());

        for (int i = 0; i < children.size(); i++) {
            ((MCRCondition) children.get(i)).accept(visitor);
        }
    }
}
