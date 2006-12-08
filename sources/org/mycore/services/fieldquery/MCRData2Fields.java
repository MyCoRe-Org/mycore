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
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRMetaISO8601Date;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * Provides methods to automatically extract field values for indexing from
 * MCRObject, MCRFile or any XML document using the definition in
 * searchfields.xml. The buildFields method returns a list of MCRFieldValue
 * objects with values extracted from the object for the given search index.
 * This class supports extracting values from MCRObject metadata, MCRFile
 * metadata, MCRFile xml content. MCRFile additional data, MCRFile text content
 * using the text filter plug-ins, and any plain XML document.
 * 
 * @see MCRSearcher#addToIndex(String, List)
 * @author Frank Lützenkirchen
 */
public class MCRData2Fields {
    /** The logger */
    private static final Logger LOGGER = Logger.getLogger(MCRData2Fields.class);

    /** A template element to be used for building individual stylesheet */
    private static Element xslTemplate;

    /** Builds a template element to be used for building individual stylesheet */
    private static void buildXSLTemplate() {
        xslTemplate = new Element("stylesheet");
        xslTemplate.setAttribute("version", "1.0");
        xslTemplate.setNamespace(MCRFieldDef.xslns);
        xslTemplate.addNamespaceDeclaration(MCRFieldDef.xmlns);
        xslTemplate.addNamespaceDeclaration(MCRFieldDef.xlinkns);
        xslTemplate.addNamespaceDeclaration(MCRFieldDef.xalanns);
        xslTemplate.addNamespaceDeclaration(MCRFieldDef.extns);
        xslTemplate.setAttribute("extension-element-prefixes", "ext");

        Element param = new Element("param", MCRFieldDef.xslns);
        param.setAttribute("name", "objectType");
        xslTemplate.addContent(param);

        Element template = new Element("template", MCRFieldDef.xslns);
        template.setAttribute("match", "/");
        xslTemplate.addContent(template);

        Element fieldValues = new Element("fieldValues", MCRFieldDef.mcrns);
        template.addContent(fieldValues);
    }

    private static MCRCache stylesheets = new MCRCache(10);

    private static Document buildStylesheet(String index, String source) {
        String key = index + "//" + source;
        Document xsl = (Document) (stylesheets.get(key));

        if (xsl == null) {
            if (xslTemplate == null)
                buildXSLTemplate();
            Element root = (Element) (xslTemplate.clone());
            Element fv = root.getChild("template", MCRFieldDef.xslns).getChild("fieldValues", MCRFieldDef.mcrns);

            List<MCRFieldDef> fieldDefs = MCRFieldDef.getFieldDefs(index);
            for (int i = 0; i < fieldDefs.size(); i++) {
                MCRFieldDef fieldDef = fieldDefs.get(i);
                if (source.indexOf(fieldDef.getSource()) == -1)
                    continue;
                Element fragment = fieldDef.getXSL();
                if (fragment != null)
                    fv.addContent(fragment);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("---------- Stylesheet for \"" + index + "\" / " + source + " ----------");
                XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
                LOGGER.debug("\n" + out.outputString(root));
            }

            xsl = new Document(root);
            stylesheets.put(key, xsl);
        }

        return xsl;
    }

    /**
     * Extracts field values for indexing from the given MCRObject's metadata.
     * 
     * @param obj
     *            the MCRObject thats metadata should be indexed
     * @param index
     *            the ID of the index as defined in searchfields.xml
     * @return a List of MCRFieldValue objects that contain field and value
     */
    public static List<MCRFieldValue> buildFields(MCRObject obj, String index) {
        String source = MCRFieldDef.OBJECT_METADATA + " " + MCRFieldDef.OBJECT_CATEGORY;
        Document xsl = buildStylesheet(index, source);
        Document xml = obj.createXML();
        return buildValues(xsl, xml, obj.getId().getTypeId());
    }

    /**
     * Extracts field values for indexing from the given MCRFile's metadata, xml
     * content or text content.
     * 
     * @param file
     *            the MCRFile thats data should be indexed
     * @param index
     *            the ID of the index as defined in searchfields.xml
     * @return a List of MCRFieldValue objects that contain field and value
     */
    public static List<MCRFieldValue> buildFields(MCRFile file, String index) {
        List<MCRFieldValue> values = new ArrayList<MCRFieldValue>();

        boolean foundSourceXMLContent = false;
        boolean foundSourceFileMetadata = false;
        boolean foundSourceFileAdditional = false;

        // Handle source FILE_TEXT_CONTENT
        List<MCRFieldDef> fieldDefList = MCRFieldDef.getFieldDefs(index);
        for (int i = 0; i < fieldDefList.size(); i++) {
            MCRFieldDef fieldDef = fieldDefList.get(i);
            if (!fieldDef.isUsedForObjectType(file.getContentTypeID()))
                continue;

            if (MCRFieldDef.FILE_TEXT_CONTENT.equals(fieldDef.getSource()))
                values.add(new MCRFieldValue(fieldDef, file));

            if (MCRFieldDef.FILE_XML_CONTENT.equals(fieldDef.getSource()))
                foundSourceXMLContent = true;
            if (MCRFieldDef.FILE_METADATA.equals(fieldDef.getSource()))
                foundSourceFileMetadata = true;
            if (MCRFieldDef.FILE_ADDITIONAL_DATA.equals(fieldDef.getSource()))
                foundSourceFileAdditional = true;
        }

        // Handle source FILE_XML_CONTENT
        if (foundSourceXMLContent) {
            Document xsl = buildStylesheet(index, MCRFieldDef.FILE_XML_CONTENT);
            Document xml = null;
            try {
                xml = file.getContentAsJDOM();
            } catch (Exception ex) {
                String msg = "Exception while building XML content of MCRFile " + file.getOwnerID() + " " + file.getAbsolutePath();
                LOGGER.error(msg, ex);
            }
            if (xml != null)
                values.addAll(buildValues(xsl, xml, file.getContentTypeID()));
        }

        // Handle source FILE_METADATA
        if (foundSourceFileMetadata) {
            Document xsl = buildStylesheet(index, MCRFieldDef.FILE_METADATA);
            Document xml = file.createXML();
            values.addAll(buildValues(xsl, xml, file.getContentTypeID()));
        }

        // Handle source FILE_ADDITIONAL_DATA
        if (foundSourceFileAdditional) {
            Document xsl = buildStylesheet(index, MCRFieldDef.FILE_ADDITIONAL_DATA);
            Document xml = null;
            try {
                xml = file.getAllAdditionalData();
            } catch (Exception ex) {
                String msg = "Exception while reading additional XML data of MCRFile " + file.getOwnerID() + " " + file.getAbsolutePath();
                LOGGER.error(msg, ex);
            }
            if (xml != null)
                values.addAll(buildValues(xsl, xml, file.getContentTypeID()));
        }

        return values;
    }

    /**
     * Extracts field values for indexing from the given JDOM xml document.
     * 
     * @param doc
     *            the JDOM xml document thats data should be indexed
     * @param index
     *            the ID of the index as defined in searchfields.xml
     * @return a List of MCRFieldValue objects that contain name, type and value
     */
    public static List<MCRFieldValue> buildFields(Document doc, String index) {
        Document xsl = buildStylesheet(index, MCRFieldDef.XML);
        return buildValues(xsl, doc, doc.getRootElement().getName());
    }

    /** Transforms xml input to search field values using XSL * */
    private static List<MCRFieldValue> buildValues(Document xsl, Document xml, String objectType) {
        List<MCRFieldValue> values = new ArrayList<MCRFieldValue>();

        List fieldValues = null;
        try {
            JDOMResult xmlres = new JDOMResult();
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setURIResolver(MCRURIResolver.instance());
            Transformer transformer = factory.newTransformer(new JDOMSource(xsl));
            transformer.setParameter("objectType", objectType);
            transformer.transform(new JDOMSource(xml), xmlres);

            List resultList = xmlres.getResult();
            Element root = (Element) (resultList.get(0));
            fieldValues = root.getChildren();
        } catch (Exception ex) {
            String msg = "Exception while transforming metadata to search field";
            throw new MCRException(msg, ex);
        }

        if (fieldValues != null)
            for (int i = 0; i < fieldValues.size(); i++) {
                Element fieldValue = (Element) (fieldValues.get(i));
                String value = fieldValue.getChildTextTrim("value", MCRFieldDef.mcrns);
                String name = fieldValue.getAttributeValue("name");
                MCRFieldDef def = MCRFieldDef.getDef(name);

                if ((value != null) && (value.length() > 0)) {
                    LOGGER.debug("MCRData2Fields " + name + " := " + value);
                    values.add(new MCRFieldValue(def, value));
                }
            }
        return values;
    }

    /**
     * Xalan XSL extension to convert MyCoRe date values to standard format. To
     * be used in a stylesheet or searchfields.xml configuration. Usage example:
     * &lt;field name="date" type="date"
     * xpath="/mycoreobject/metadata/dates/date"
     * value="ext:normalizeDate(string(text()))" &gt;
     * 
     * @param date
     *            the date string in a locale-dependent format
     */
    public static String normalizeDate(String sDate) {
        try {
            MCRMetaISO8601Date iDate = new MCRMetaISO8601Date();
            iDate.setDate(sDate.trim());
            return iDate.getISOString().substring(0, 10);
        } catch (Exception ex) {
            LOGGER.debug(ex);
            return "";
        }
    }
}
