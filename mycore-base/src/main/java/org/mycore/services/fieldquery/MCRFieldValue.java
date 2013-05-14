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
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRNormalizer;

/**
 * Represents the value of a field in a query. This can be a value that is part
 * of the query results (hit sort data or meta data) or a value that is built
 * when data is indexed.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRFieldValue {

    /**
     * The field this value belongs to
     */
    @Deprecated
    private MCRFieldDef field;

    /**
     * The field this value belongs to
     */
    private String fieldName;

    /**
     * The fields's value, as a String
     */
    private String value;

    /**
     * Creates a new field value
     * 
     * @param field
     *            the field this value belongs to
     * @param value
     *            the value of the field, as a String
     */
    @Deprecated
    public MCRFieldValue(MCRFieldDef field, String value) {
        if (field == null) {
            throw new NullPointerException("MCRFieldDef cannot be null.");
        }
        this.field = field;
        setValue(value);
        this.fieldName = field.getName();
    }

    /**
     * Creates a new field value
     * 
     * @param field
     *            the field this value belongs to
     * @param value
     *            the value of the field, as a String
     */
    public MCRFieldValue(String fieldName, String value) {
        setFieldName(fieldName);
        this.value = value;
    }

    public void setFieldName(String fieldName) {
        if (fieldName == null) {
            throw new NullPointerException("field name cannot be null.");
        }
        this.fieldName = fieldName;
        try {
            this.field = MCRFieldDef.getDef(fieldName);
        } catch (MCRConfigurationException ce) {
            this.field = null;
        }
    }

    /**
     * Returns the field this value belongs to
     */
    @Deprecated
    public MCRFieldDef getField() {
        return field;
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
        if (field != null) {
            eField.setAttribute("name", field.getName());
        } else {
            eField.setAttribute("name", getFieldName());
        }
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
    public static MCRFieldValue parseXML(Element xml) {
        String name = xml.getAttributeValue("name", "");
        String value = xml.getText();

        if (name.length() == 0) {
            throw new MCRException("Field value attribute 'name' is empty");
        }
        if (value.length() == 0) {
            throw new MCRException("Field value is empty");
        }
        return new MCRFieldValue(name, value);
    }

    @Override
    public String toString() {
        return getFieldName() + " = " + value;
    }

}
