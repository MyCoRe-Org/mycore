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

package org.mycore.services.fieldquery;

import org.jdom.Element;
import org.mycore.common.MCRNormalizer;
import org.mycore.datamodel.ifs.MCRFile;

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
    private MCRFieldDef field;

    /**
     * The fields's value, as a String
     */
    private String value;

    /**
     * The MCRFile thats content should become the value of the field when
     * indexing data
     */
    private MCRFile file;

    /**
     * Creates a new field value
     * 
     * @param field
     *            the field this value belongs to
     * @param value
     *            the value of the field, as a String
     */
    public MCRFieldValue(MCRFieldDef field, String value) {
        this.field = field;
        this.value = value;
        if (field.getDataType().equals("text") || field.getDataType().equals("name"))
            this.value = MCRNormalizer.normalizeString(value);
    }

    /**
     * Creates a new field value
     * 
     * @param field
     *            the field this value belongs to
     * @param file
     *            the MCRFile thats content should become the value of the field
     *            when indexing data
     */
    MCRFieldValue(MCRFieldDef field, MCRFile file) {
        this.field = field;
        this.file = file;
    }

    /**
     * Returns the field this value belongs to
     */
    public MCRFieldDef getField() {
        return field;
    }

    /**
     * Returns the value of the field as a String
     * 
     * @return the value of the field as a String, or null if the value is the
     *         content of an MCRFile
     * @see #getFile()
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the MCRFile thats content should become the value of the field
     * when indexing data
     * 
     * @return the MCRFile to be indexed, or null if the value can be retrieved
     *         using the getValue() method
     * @see #getValue()
     */
    public MCRFile getFile() {
        return file;
    }

    /**
     * Builds a XML representation of this field's value
     * 
     * @return a 'field' element with attribute 'name' and the value as element
     *         content
     */
    public Element buildXML() {
        Element eField = new Element("field", MCRFieldDef.mcrns);
        eField.setAttribute("name", field.getName());
        eField.addContent(value);
        return eField;
    }
}
