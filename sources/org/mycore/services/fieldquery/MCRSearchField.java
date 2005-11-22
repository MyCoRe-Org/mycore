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

import java.util.List;
import java.util.Properties;

import org.jdom.Element;
import org.jdom.Namespace;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.ifs.MCRFile;

/**
 * Represents a single search field as defined by searchfields.xml. Objects of
 * this class are used for sorting MCRResults and for extracting field values
 * for object metadata or content using MCRData2Fields.
 * 
 * @author Frank Lützenkirchen
 * 
 * @see MCRData2Fields#buildFields
 * @see MCRResults#sort(List)
 */
public class MCRSearchField {
    private static Properties fieldTypes = new Properties();

    static {
        Namespace mcrns = Namespace.getNamespace("mcr", "http://www.mycore.org/");

        String uri = "resource:searchfields.xml";
        Element def = MCRURIResolver.instance().resolve(uri);

        List children = def.getChildren("index", mcrns);

        for (int i = 0; i < children.size(); i++) {
            Element index = (Element) (children.get(i));
            List fields = index.getChildren("field", mcrns);

            for (int j = 0; j < fields.size(); j++) {
                Element field = (Element) (fields.get(j));
                String name = field.getAttributeValue("name");
                String type = field.getAttributeValue("type");
                fieldTypes.put(name, type);
            }
        }
    }

    /** Sort this field in ascending order * */
    public final static boolean ASCENDING = true;

    /** Sort this field in descending order * */
    public final static boolean DESCENDING = false;

    /** Name of the field as defined in searchfields.xml * */
    private String name;

    /** Sort order of this field if it is part of the sort criteria * */
    private boolean order = ASCENDING;

    /** The value of this field * */
    private String value;

    /**
     * The file to read the field's content from if it represents a fulltext
     * stream *
     */
    private MCRFile file;
    
    /** The field is sortable, used by lucene to store original data * */
    private boolean sortable = false;

    /** Creates a new search field with the given name **/
    public MCRSearchField( String name )
    { this.name = name; }

    /** Returns the name of the field * */
    public String getName() {
        return name;
    }

    /** Sets the name of the field * */
    public void setName(String name) {
        this.name = name;
    }

    /** Returns the sort order if this field is part of the sort criteria * */
    public boolean getSortOrder() {
        return order;
    }

    /** Sets the sort order if this field is part of the sort criteria * */
    public void setSortOrder(boolean order) {
        this.order = order;
    }

    /** Returns the data type of this field as defined in fieldtypes.xml * */
    public String getDataType() {
        return fieldTypes.getProperty(name);
    }

    /** Returns the data type of this field as defined in fieldtypes.xml * */
    public static String getDataType(String fieldName) {
        return fieldTypes.getProperty(fieldName);
    }

    /**
     * Returns the value of this field, filled by MCRData2Fields.buildFields
     * method
     * 
     * @see MCRData2Fields#buildFields
     */
    public String getValue() {
        return value;
    }

    /** Sets the value of this field * */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Sets the MCRFile thats text content should be the value of this field.
     */
    public void setFile(MCRFile file) {
        this.file = file;
    }

    /**
     * Returns the MCRFile thats text content should be the value of this field.
     * If MCRData2Fields.buildFields has set a non-null value here, this means
     * the indexer should use the text filter plug-ins to extract the fulltext
     * of this file and set that as the content of this field.
     */
    public MCRFile getFile() {
        return file;
    }
    
    /** Returns if this field is sortable * */
    public boolean getSortabale() {
        return sortable;
    }

    /** Sets the sortable flag for this field * */
    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }
}
