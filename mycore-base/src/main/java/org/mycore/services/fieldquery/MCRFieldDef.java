/*
 * $Revision$ $Date$
 * 
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.services.fieldquery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.services.fieldquery.data2fields.MCRFieldsSelector;

/**
 * A search field definition. For each field in the configuration file
 * searchfields.xml there is one MCRFieldDef instance with attributes that
 * represent the configuration in the xml file.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRFieldDef {

    private static Hashtable<String, MCRFieldDef> fieldTable = new Hashtable<String, MCRFieldDef>();

    private final static String configFile = "searchfields.xml";

    private static Set<Namespace> namespaces = new HashSet<Namespace>();

    /**
     * Read searchfields.xml and build the MCRFiedDef objects
     */
    static {
        Element def = getConfigFile();
        if (def != null) {
            collectAllNamespaces(def);

            List<Element> children = def.getChildren("index", MCRConstants.MCR_NAMESPACE);

            for (Element index : children) {
                String id = index.getAttributeValue("id");

                List<Element> fields = index.getChildren("field", MCRConstants.MCR_NAMESPACE);

                for (Element field : fields) {
                    new MCRFieldDef(id, field);
                }
            }
        } else {
            Logger.getLogger(MCRFieldDef.class).warn("Continue without " + configFile + ".");
        }
    }

    @SuppressWarnings({ "rawtypes" })
    private static void collectAllNamespaces(Element searchfields) {
        for (Iterator iter = searchfields.getDescendants(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof Element) {
                Element element = (Element) obj;
                namespaces.add(element.getNamespace());
                namespaces.addAll(element.getAdditionalNamespaces());
            } else if (obj instanceof Attribute)
                namespaces.add(((Attribute) obj).getNamespace());
        }
    }

    public static Set<Namespace> getAllNamespaces() {
        return namespaces;
    }

    /**
     * The index this field belongs to
     */
    private String index;

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

    /**
     * A list of object names this field is used for, separated by blanks
     */
    private String objects;

    /**
     * A keyword identifying where the values of this field come from
     */
    private String source;

    /**
     * If true, the content of this field will be added by searcher as metadata
     * in hit
     */
    private boolean addable = false;

    private String xPathAttribute;

    private String valueAttribute;

    private Element def;

    /**
     * @return the searchfields-configuration file as jdom-element
     */
    public static Element getConfigFile() {
        String uri = "resource:" + configFile;
        return MCRURIResolver.instance().resolve(uri);
    }

    public MCRFieldDef(String index, Element def) {
        this.index = index.intern();
        this.def = def;
        name = def.getAttributeValue("name").intern();
        dataType = def.getAttributeValue("type").intern();
        sortable = "true".equals(def.getAttributeValue("sortable"));
        objects = def.getAttributeValue("objects");
        source = def.getAttributeValue("source");
        addable = "true".equals(def.getAttributeValue("addable"));
        xPathAttribute = def.getAttributeValue("xpath");
        valueAttribute = def.getAttributeValue("value", "text()");

        if (!fieldTable.contains(name)) {
            fieldTable.put(name, this);
        } else {
            throw new MCRException("Field \"" + name + "\" is defined repeatedly.");
        }
    }

    /**
     * Returns the MCRFieldDef with the given name, or throws
     * MCRConfigurationException if no such field is defined in searchfields.xml
     * 
     * @param name
     *            the name of the field
     * @return the MCRFieldDef instance representing this field
     */
    public static MCRFieldDef getDef(String name) {
        if (!fieldTable.containsKey(name)) {
            throw new MCRConfigurationException("Field \"" + name + "\" is not defined in searchfields.xml");
        }
        return fieldTable.get(name);
    }

    /**
     * Returns all fields that belong to the given index
     * 
     * @param index
     *            the ID of the index
     * @return a List of MCRFieldDef objects for that index
     */
    public static List<MCRFieldDef> getFieldDefs(String index) {
        List<MCRFieldDef> fields = new ArrayList<MCRFieldDef>();
        for (MCRFieldDef field : fieldTable.values()) {
            if (field.index.equals(index)) {
                fields.add(field);
            }
        }
        return fields;
    }

    /**
     * Returns the ID of the index this field belongs to
     * 
     * @return the index ID
     */
    public String getIndex() {
        return index;
    }

    /**
     * Returns the name of the field The name is internalized (see
     * {@link String#intern()}
     * 
     * @return the field's name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the data type of this field as defined in fieldtypes.xml The data
     * type is internalized (see {@link String#intern()}
     * 
     * @return the data type
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * Returns true if this field can be used as sort criteria
     * 
     * @return true, if field can be used to sort query results
     */
    public boolean isSortable() {
        return sortable;
    }

    /**
     * Returns true if this field should be added by searcher to hit
     * 
     * @return true, if this field should be added by searcher to hit
     */
    public boolean isAddable() {
        return addable;
    }

    public String getXPathAttribute() {
        return xPathAttribute;
    }

    public String getValueAttribute() {
        return valueAttribute;
    }

    /**
     * Returns true if this field is used for this type of object. For
     * MCRObject, the type is the same as in MCRObject.getId().getTypeId(). For
     * MCRFile, the type is the same as in MCRFile.getContentTypeID(). For plain
     * XML data, the type is the name of the root element.
     * 
     * @param objectType
     *            the type of object
     */
    public boolean isUsedForObjectType(String objectType) {
        if (objects == null) {
            return true;
        }

        String a = " " + objects + " ";
        String b = " " + objectType + " ";
        return a.contains(b);
    }

    public boolean isUsedFor(MCRFieldsSelector selector) {
        return getSource().equals(selector.getSourceType()) && isUsedForObjectType(selector.getObjectType());
    }

    /**
     * A keyword identifying that the source of the values of this field is the
     * MCRSearcher that does the search, this means it is technical hit metadata
     * added by the searcher when the query results are built.
     */
    public final static String SEARCHER_HIT_METADATA = "searcherHitMetadata";

    /**
     * Returns a keyword identifying where the values of this field come from.
     */
    public String getSource() {
        return source;
    }

    public List<Content> getXSLContent() {
        return (List<Content>) (def.cloneContent());
    }

    @Override
    public String toString() {
        return this.getName() + " (" + this.getIndex() + ")";
    }
}
