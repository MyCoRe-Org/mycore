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

package org.mycore.services.fieldquery;

import org.jdom2.Element;
import org.mycore.parsers.bool.MCRCondition;

/**
 * Represents a simple query condition, which consists of a search field,
 * a value and a comparison operator.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRQueryCondition extends MCRFieldBaseValue implements MCRCondition<Object> {

    /** The comparison operator used in this condition */
    private String operator;

    public MCRQueryCondition(String fieldName, String operator, String value) {
        super(fieldName, value);
        this.operator = operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    /** Returns the comparison operator used in this condition */
    public String getOperator() {
        return this.operator;
    }

    @Override
    public String toString() {
        return getFieldName() + " " + getOperator() + " \"" + getValue() + "\"";
    }

    public Element toXML() {
        Element condition = new Element("condition");
        condition.setAttribute("field", getFieldName());
        condition.setAttribute("operator", operator);
        condition.setAttribute("value", getValue());

        return condition;
    }

    public boolean evaluate(Object o) {
        return false;
    }
}
