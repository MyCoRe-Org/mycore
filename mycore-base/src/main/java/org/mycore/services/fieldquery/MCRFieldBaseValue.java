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
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;

public class MCRFieldBaseValue {

    /**
     * The field this value belongs to
     */
    protected String fieldName;

    /**
     * The fields's value, as a String
     */
    protected String value;

    public MCRFieldBaseValue() {
        super();
    }

    public MCRFieldBaseValue(String name, String value) {
        this.fieldName = name;
        this.value = value;
    }

    public void setFieldName(String fieldName) {
        if (fieldName == null) {
            throw new NullPointerException("field name cannot be null.");
        }
        this.fieldName = fieldName;
    }

    /**
     * Returns the field this value belongs to
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Sets or updates the field value
     * 
     * @param value the value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns the value of the field as a String
     * 
     * @return the value of the field as a String
     */
    public String getValue() {
        return value;
    }

    /**
     * Builds a XML representation of this field's value
     * 
     * @return a 'field' element with attribute 'name' and the value as element
     *         content
     */
    public Element buildXML() {
        Element eField = new Element("field", MCRConstants.MCR_NAMESPACE);
        eField.setAttribute("name", getFieldName());
        eField.addContent(value);
        return eField;
    }

    /**
     * Parses a XML representation of a field value
     * 
     * @param xml
     *            the field value as XML element
     * @return the parsed MCRFieldValue object
     */
    public static MCRFieldBaseValue parseXML(Element xml) {
        String name = xml.getAttributeValue("name", "");
        String value = xml.getText();

        if (name.length() == 0) {
            throw new MCRException("Field value attribute 'name' is empty");
        }
        if (value.length() == 0) {
            throw new MCRException("Field value is empty");
        }
        return new MCRFieldBaseValue(name, value);
    }

    @Override
    public String toString() {
        return getFieldName() + " = " + value;
    }

}
