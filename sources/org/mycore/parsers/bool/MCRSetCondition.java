/*
 * 
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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Element;

/**
 * @author Frank Lützenkirchen
 */
public abstract class MCRSetCondition implements MCRCondition {

    public final static String AND = "and";

    public final static String OR = "or";

    protected String operator;

    protected List<MCRCondition> children = new LinkedList<MCRCondition>();

    protected MCRSetCondition(String operator) {
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }

    public MCRSetCondition addChild(MCRCondition condition) {
        children.add(condition);
        return this;
    }

    public MCRSetCondition addAll(Collection<MCRCondition> conditions) {
        children.addAll(conditions);
        return this;
    }

    public List<MCRCondition> getChildren() {
        return children;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < children.size(); i++) {
            sb.append("(").append(children.get(i)).append(")");
            if (i < (children.size() - 1))
                sb.append(' ').append(operator.toUpperCase()).append(' ');
        }
        return sb.toString();
    }

    public Element toXML() {
        Element cond = new Element("boolean").setAttribute("operator", operator);
        for (MCRCondition child : children)
            cond.addContent(child.toXML());
        return cond;
    }

    public Element info() {
        Element el = new Element("info");
        el.setAttribute(new Attribute("type", operator));
        el.setAttribute(new Attribute("children", "" + children.size()));
        return el;
    }

    public void accept(MCRConditionVisitor visitor) {
        visitor.visitType(info());
        for (MCRCondition child : children)
            child.accept(visitor);
    }

    public abstract boolean evaluate(Object o);
}
