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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;
import java.util.List;

import org.jdom.Element;
import org.mycore.common.xml.MCRURIResolver;

/**
 * Reads configuration file fieldtypes.xml and provides methods to check the
 * operators allowed for a given type, to get the default search operator for
 * that type, and so on.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRFieldType {
    /** Default search operators by type */
    private final static Properties defOps = new Properties();

    /** Allowed search operators by type */
    private final static Hashtable<String, List<String>> allOps = new Hashtable<String, List<String>>();

    /**
     * Reads in configuration file fieldtypes.xml
     */
    static {
        Element fieldtypes = MCRURIResolver.instance().resolve("resource:fieldtypes.xml");
        for (Element type : (List<Element>) (fieldtypes.getChildren("type", MCRFieldDef.mcrns))) {
            String name = type.getAttributeValue("name");
            String def = type.getAttributeValue("default");
            defOps.setProperty(name, def);

            List<String> operators = new ArrayList<String>();
            for (Element oper : (List<Element>) (type.getChildren("operator", MCRFieldDef.mcrns)))
                operators.add(oper.getAttributeValue("token"));
            allOps.put(name, operators);
        }
    }

    /**
     * Checks if the given field type is valid
     * 
     * @param fieldType
     *            the field type, e.g. "identifier"
     * @return true, if this field type is known, false otherwise
     */
    public static boolean isValidType(String fieldType) {
        return defOps.containsKey(fieldType);
    }

    /**
     * Checks if the given search operator can be used for this field type
     * 
     * @param fieldType
     *            the field type, e.g. "identifier"
     * @return true, if this search operator can be used, false otherwise
     * @return
     */
    public static boolean isValidOperatorForType(String fieldType, String operator) {
        if (!isValidType(fieldType))
            return false;
        List operators = allOps.get(fieldType);
        return operators.contains(operator);
    }

    /**
     * Returns the default search operator for this field type.
     * 
     * @param fieldType
     *            the field type, e.g. "identifier"
     * @return the default search operator recommended for this type
     */
    public static String getDefaultOperator(String fieldType) {
        return defOps.getProperty(fieldType);
    }

    /**
     * Returns a list of search operators allowed for this type
     * 
     * @param fieldType
     *            the field type, e.g. "identifier"
     * @return a List of Strings containing the allowed search operators
     */
    public static List<String> getAllowedOperatorsForType(String fieldType) {
        return allOps.get(fieldType);
    }
}
