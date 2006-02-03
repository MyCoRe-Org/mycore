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
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;
import org.mycore.common.xml.MCRURIResolver;

/**
 * A search field definition. For each field in the configuration file
 * searchfields.xml there is one MCRFieldDef instance with attributes that
 * represent the configuration in the xml file.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRFieldDef {
    /** The logger */
    private static final Logger LOGGER = Logger.getLogger(MCRFieldDef.class);

    /**
     * name -> MCRFieldDef
     */
    private static Hashtable fieldTable = new Hashtable();

    private static Namespace xslns = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");

    static Namespace mcrns = Namespace.getNamespace("mcr", "http://www.mycore.org/");

    private static Namespace xmlns = Namespace.getNamespace("xml", "http://www.w3.org/XML/1998/namespace");

    private static Namespace xlinkns = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

    private static Namespace xalanns = Namespace.getNamespace("xalan", "http://xml.apache.org/xalan");

    private static Namespace extns = Namespace.getNamespace("ext", "xalan://org.mycore.services.fieldquery.MCRData2Fields");

    /**
     * Read searchfields.xml and build the MCRFiedDef objects
     */
    static {
        buildXSLTemplate();

        String uri = "resource:searchfields.xml";
        Element def = MCRURIResolver.instance().resolve(uri);

        List children = def.getChildren("index", mcrns);

        for (int i = 0; i < children.size(); i++) {
            Element index = (Element) (children.get(i));
            String id = index.getAttributeValue("id");

            List fields = index.getChildren("field", mcrns);

            for (int j = 0; j < fields.size(); j++)
            	new MCRFieldDef(id, (Element)fields.get(j));
        }
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

    public MCRFieldDef(String index, Element def) {
        this.index = index;
        this.name = def.getAttributeValue("name");
        this.dataType = def.getAttributeValue("type");
        this.sortable = "true".equals(def.getAttributeValue("sortable"));
        this.objects = def.getAttributeValue("objects", (String) null);
        this.source = def.getAttributeValue("source");

        fieldTable.put(name, this);
        buildStylesheet(def);
    }

    /**
     * Returns the MCRFieldDef with the given name, or null if no such field is
     * defined in searchfields.xml
     * 
     * @param name
     *            the name of the field
     * @return the MCRFieldDef instance representing this field
     */
    public static MCRFieldDef getDef(String name) {
        return (MCRFieldDef) (fieldTable.get(name));
    }

    /**
     * Returns all fields that belong to the given index
     * 
     * @param index
     *            the ID of the index
     * @return a List of MCRFieldDef objects for that index
     */
    public static List getFieldDefs(String index) {
        List fields = new ArrayList();
        for (Iterator iter = fieldTable.values().iterator(); iter.hasNext();) {
            MCRFieldDef field = (MCRFieldDef) (iter.next());
            if (field.index.equals(index))
                fields.add(field);
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
     * Returns the name of the field
     * 
     * @return the field's name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the data type of this field as defined in fieldtypes.xml
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
     * Returns true if this field is used for this type of object.
     * For MCRObject, the type is the same as in MCRObject.getId().getTypeId().
     * For MCRFile, the type is the same as in MCRFile.getContentTypeID().
     * For plain XML data, the type is the name of the root element. 
     * 
     * @param objectType
     *            the type of object
     * @return
     */
    boolean isUsedForObjectType(String objectType) {
        if (objects == null)
            return true;

        String a = " " + objects + " ";
        String b = " " + objectType + " ";
        return (a.indexOf(b) >= 0);
    }

    /**
     * A keyword identifying that the source of the values of this field is the
     * createXML() method of MCRObject
     * 
     * @see org.mycore.datamodel.metadata.MCRObject#createXML()
     */
    public final static String OBJECT_METADATA = "objectMetadata";

    /**
     * A keyword identifying that the source of the values of this field is the
     * createXML() method of MCRFile
     * 
     * @see org.mycore.datamodel.ifs.MCRFile#createXML()
     */
    public final static String FILE_METADATA = "fileMetadata";

    /**
     * A keyword identifying that the source of the values of this field is the
     * getAdditionalData() method of MCRFile
     * 
     * @see org.mycore.datamodel.ifs.MCRFilesystemNode#getAdditionalData()
     */
    public final static String FILE_ADDITIONAL_DATA = "fileAdditionalData";

    /**
     * A keyword identifying that the source of the values of this field is the
     * XML content of the MCRFile
     * 
     * @see org.mycore.datamodel.ifs.MCRFile#getContentAsJDOM()
     */
    public final static String FILE_XML_CONTENT = "fileXMLContent";

    /**
     * A keyword identifying that the source of the values of this field is the
     * XML content of a pure JDOM xml document
     */
    public final static String XML = "xml";

    /**
     * A keyword identifying that the source of the values of this field is the
     * text content of the MCRFile, using text filter plug-ins.
     */
    public final static String FILE_TEXT_CONTENT = "fileTextContent";

    /**
     * A keyword identifying that the source of the values of this field is the
     * MCRSearcher that does the search, this means it is technical hit metadata
     * added by the searcher when the query results are built.
     */
    public final static String SEARCHER_HIT_METADATA = "searcherHitMetadata";

    /**
     * Returns a keyword identifying where the values of this field come from.
     * 
     * @see #FILE_METADATA
     * @see #FILE_TEXT_CONTENT
     * @see #FILE_XML_CONTENT
     * @see #OBJECT_METADATA
     * @see #SEARCHER_HIT_METADATA
     * @see #XML
     */
    public String getSource() {
        return source;
    }

    /** A template element to be used for building individual stylesheet */
    private static Element xslTemplate;

    /** Builds a template element to be used for building individual stylesheet */
    private static void buildXSLTemplate() {
        xslTemplate = new Element("stylesheet");
        xslTemplate.setAttribute("version", "1.0");
        xslTemplate.setNamespace(xslns);
        xslTemplate.addNamespaceDeclaration(xmlns);
        xslTemplate.addNamespaceDeclaration(xlinkns);
        xslTemplate.addNamespaceDeclaration(xalanns);
        xslTemplate.addNamespaceDeclaration(extns);
        xslTemplate.setAttribute("extension-element-prefixes", "ext");

        Element template = new Element("template", xslns);
        template.setAttribute("match", "/");
        xslTemplate.addContent(template);

        Element fieldValues = new Element("fieldValues", mcrns);
        template.addContent(fieldValues);

        Element forEach = new Element("for-each", xslns);
        fieldValues.addContent(forEach);
    }

    /** The stylesheet to build values for this field from XML source data */
    private Document xsl = null;

    /** Returns a stylesheet to build values for this field from XML source data */
    Document getStylesheet() {
        return xsl;
    }

    /** Builds the stylesheet to build values for this field from XML source data */
    private void buildStylesheet(Element fieldDef) {
        String xpath = fieldDef.getAttributeValue("xpath");
        if ((xpath == null) || (xpath.trim().length() == 0)) {
            return;
        }

        Element stylesheet = (Element) (xslTemplate.clone());
        xsl = new Document(stylesheet);

        // <xsl:for-each select="{@xpath}">
        Element forEach = stylesheet.getChild("template", xslns).getChild("fieldValues", mcrns).getChild("for-each", xslns);
        forEach.setAttribute("select", xpath);

        // <mcr:fieldValue>
        Element fieldValue = new Element("fieldValue", mcrns);
        forEach.addContent(fieldValue);

        // <mcr:value>
        Element valueElem = new Element("value", mcrns);
        fieldValue.addContent(valueElem);

        // <xsl:value-of select="{@value}" />
        String valueExpr = fieldDef.getAttributeValue("value");
        Element valueOf = new Element("value-of", xslns);
        valueOf.setAttribute("select", valueExpr);
        valueElem.addContent(valueOf);

        List attributes = fieldDef.getChildren("attribute", mcrns);

        for (int j = 0; j < attributes.size(); j++) {
            Element attribDef = (Element) (attributes.get(j));

            // <mcr:attribute name="{@name}">
            Element attribute = new Element("attribute", mcrns);
            fieldValue.addContent(attribute);
            attribute.setAttribute("name", attribDef.getAttributeValue("name"));

            // <xsl:value-of select="{@value}" />
            valueOf = new Element("value-of", xslns);
            valueOf.setAttribute("select", attribDef.getAttributeValue("value"));
            attribute.addContent(valueOf);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("---------- Stylesheet for search field <" + name + "> ----------");
            XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
            LOGGER.debug(out.outputString(xsl));
        }
    }
}
