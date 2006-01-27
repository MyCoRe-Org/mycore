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

import java.util.Hashtable;
import java.util.List;

import org.jdom.Element;
import org.jdom.Namespace;
import org.mycore.common.xml.MCRURIResolver;

/**
 * A search field definition. For each field in the configuration file
 * searchfields.xml there is one MCRFieldDef instance with attributes
 * name, data type and sortable flag.
 *  
 * @author Frank Lützenkirchen
 */
public class MCRFieldDef {
    /**
     * A map, name -> MCRFieldDef
     */
    private static Hashtable fieldTable = new Hashtable();

    /**
     * Read searchfields.xml and build the MCRFiedDef objects
     */
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
                boolean sortable = "true".equals(field.getAttributeValue("sortable")) ? true : false;

                fieldTable.put(name, new MCRFieldDef(name, type, sortable));
            }
        }
    }

    /**
     * The unique name of the field
     */
    private String name;

    /**
     * The data type of the field, see fieldtypes.xml file
     */
    private String dataType;

    /**
     * If true, this field can be used to sort query results
     */
    private boolean sortable = false;

    private MCRFieldDef(String name, String dataType, boolean sortable) {
        this.name = name;
        this.dataType = dataType;
        this.sortable = sortable;
    }

    /**
     * Returns the MCRFieldDef with the given name, or null if no
     * such field is defined in searchfields.xml
     * 
     * @param name the name of the field
     * @return the MCRFieldDef instance representing this field
     */
    public static MCRFieldDef getDef(String name) {
        return (MCRFieldDef) (fieldTable.get(name));
    }

    /**
     * Returns the name of the field
     * @return the field's name
     */
    public String getName() {
        return name;
    }

    /** 
     * Returns the data type of this field as defined in fieldtypes.xml
     * @return the data type 
     */
    public String getDataType() {
        return dataType;
    }

    /** 
     * Returns true if this field can be used as sort criteria
     * @return true, if field can be used to sort query results 
     */
    public boolean isSortable() {
        return sortable;
    }
}
