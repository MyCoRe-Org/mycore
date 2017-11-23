/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.parsers.bool;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.jdom2.Element;

/**
 * @author Frank Lützenkirchen
 */
public abstract class MCRSetCondition<T> implements MCRCondition<T> {

    public static final String AND = "and";

    public static final String OR = "or";

    protected String operator;

    protected List<MCRCondition<T>> children = new LinkedList<>();

    protected MCRSetCondition(String operator) {
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }

    public MCRSetCondition<T> addChild(MCRCondition<T> condition) {
        children.add(condition);
        return this;
    }

    public MCRSetCondition<T> addAll(Collection<MCRCondition<T>> conditions) {
        children.addAll(conditions);
        return this;
    }

    public List<MCRCondition<T>> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < children.size(); i++) {
            sb.append("(").append(children.get(i)).append(")");
            if (i < children.size() - 1) {
                sb.append(' ').append(operator.toUpperCase(Locale.ROOT)).append(' ');
            }
        }
        return sb.toString();
    }

    public Element toXML() {
        Element cond = new Element("boolean").setAttribute("operator", operator);
        for (MCRCondition<T> child : children) {
            cond.addContent(child.toXML());
        }
        return cond;
    }

    public abstract boolean evaluate(T o);
}
