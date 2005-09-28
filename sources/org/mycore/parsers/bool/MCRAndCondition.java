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
public class MCRAndCondition implements MCRCondition {
    private List children;

    public MCRAndCondition() {
        this.children = new LinkedList();
    }

    public MCRAndCondition(MCRCondition firstchild) {
        this.children = new LinkedList();
        addChild(firstchild);
    }

    public MCRAndCondition(MCRCondition firstchild, MCRCondition secondchild) {
        this.children = new LinkedList();
        addChild(firstchild);
        addChild(secondchild);
    }

    public void addChild(MCRCondition child) {
        this.children.add(child);
    }

    public List getChildren() {
        return children;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < children.size(); i++) {
            sb.append("(").append(children.get(i)).append(")");

            if (i < (children.size() - 1)) {
                sb.append(" AND ");
            }
        }

        return sb.toString();
    }

    public boolean evaluate(Object o) {
        for (int i = 0; i < children.size(); i++) {
            if (!((MCRCondition) children.get(i)).evaluate(o)) {
                return false;
            }
        }

        return true;
    }

    public Element toXML() {
        Element cond = new Element("and");

        for (int i = 0; i < children.size(); i++) {
            MCRCondition child = (MCRCondition) (children.get(i));
            cond.addContent(child.toXML());
        }

        return cond;
    }

    public Element info() {
        Element el = new Element("info");
        el.setAttribute(new Attribute("type", "AND"));
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
