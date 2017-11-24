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

package org.mycore.services.fieldquery;

import org.jdom2.Element;
import org.mycore.parsers.bool.MCRCondition;

/**
 * Represents a simple query condition, which consists of a search field,
 * a value and a comparison operator.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRQueryCondition extends MCRFieldBaseValue implements MCRCondition<Void> {

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

    public boolean evaluate(Void o) {
        //there is no 'void' instance
        throw new UnsupportedOperationException();
    }
}
