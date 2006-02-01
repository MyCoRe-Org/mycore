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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * Provides methods to automatically extract field values for indexing from
 * MCRObject or MCRFile using the definition in searchfields.xml. The
 * buildFields method returns a list of MCRFieldValue objects with values
 * extracted from the object for the given search index. This class supports
 * extracting values from MCRObject metadata, MCRFile metadata, MCRFile xml
 * content and MCRFile text content (using the text filter plug-ins).
 * 
 * @see MCRSearcher#addToIndex(String, List)
 * @author Frank Lützenkirchen
 */
public class MCRData2Fields {
    /** The logger */
    private static final Logger LOGGER = Logger.getLogger(MCRData2Fields.class);

    /**
     * Extracts field values for indexing from the given MCRObject's metadata.
     * 
     * @param obj
     *            the MCRObject thats metadata should be indexed
     * @param index
     *            the ID of the index as defined in searchfields.xml
     * @return a List of MCRFieldValue objects that contain field and value
     */
    public static List buildFields(MCRObject obj, String index) {
        List values = new ArrayList();
        List fieldDefList = MCRFieldDef.getFieldDefs(index);

        Document xml = null;
        for (int i = 0; i < fieldDefList.size(); i++) {
            MCRFieldDef fieldDef = (MCRFieldDef) (fieldDefList.get(i));

            if (!fieldDef.isUsedForObjectType(obj.getId().getTypeId()))
                continue;
            if (!MCRFieldDef.OBJECT_METADATA.equals(fieldDef.getSource()))
                continue;

            if (xml == null)
                xml = obj.createXML();
            addValues(fieldDef, xml, values);
        }
        return values;
    }

    /**
     * Extracts field values for indexing from the given MCRFile's metadata, xml
     * content or text content.
     * 
     * @param obj
     *            the MCRFile thats data should be indexed
     * @param index
     *            the ID of the index as defined in searchfields.xml
     * @return a List of MCRFieldValue objects that contain field and value
     */
    public static List buildFields(MCRFile file, String index) {
        List values = new ArrayList();
        List fieldDefList = MCRFieldDef.getFieldDefs(index);

        Document xmlContent = null;
        Document xmlMetadata = null;
        Document xmlAdditional = null;

        for (int i = 0; i < fieldDefList.size(); i++) {
            MCRFieldDef fieldDef = (MCRFieldDef) (fieldDefList.get(i));
            if (!fieldDef.isUsedForObjectType(file.getContentTypeID()))
                continue;

            if (MCRFieldDef.FILE_TEXT_CONTENT.equals(fieldDef.getSource())) {
                values.add(new MCRFieldValue(fieldDef, file));
            } else if (MCRFieldDef.FILE_XML_CONTENT.equals(fieldDef.getSource())) {
                if (xmlContent == null) {
                    try {
                        xmlContent = file.getContentAsJDOM();
                    } catch (Exception ex) {
                        String msg = "Exception while building XML content of MCRFile " + file.getOwnerID() + " " + file.getAbsolutePath();
                        LOGGER.error(msg, ex);
                    }
                }
                if (xmlContent != null)
                    addValues(fieldDef, xmlContent, values);
            } else if (MCRFieldDef.FILE_METADATA.equals(fieldDef.getSource())) {
                if (xmlMetadata == null)
                    xmlMetadata = file.createXML();
                addValues(fieldDef, xmlMetadata, values);
            } else if (MCRFieldDef.FILE_ADDITIONAL_DATA.equals(fieldDef.getSource())) {
                if (xmlAdditional == null) {
                    try {
                        xmlAdditional = file.getAdditionalData();
                    } catch (Exception ex) {
                        String msg = "Exception while reading additional XML data of MCRFile " + file.getOwnerID() + " " + file.getAbsolutePath();
                        LOGGER.error(msg, ex);
                    }
                }
                if (xmlAdditional != null)
                    addValues(fieldDef, xmlAdditional, values);
            }
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
    public static List buildFields(Document doc, String index) {
        List values = new ArrayList();
        List fieldDefList = MCRFieldDef.getFieldDefs(index);

        for (int i = 0; i < fieldDefList.size(); i++) {
            MCRFieldDef fieldDef = (MCRFieldDef) (fieldDefList.get(i));
            if (!fieldDef.isUsedForObjectType(doc.getRootElement().getName()))
                continue;
            if (!MCRFieldDef.XML.equals(fieldDef.getSource()))
                continue;
            addValues(fieldDef, doc, values);
        }
        return values;
    }

    /** Transforms xml input to search field values using XSL * */
    private static void addValues(MCRFieldDef def, Document xml, List values) {
        List fieldValues = transform(def, xml);

        if (fieldValues != null)
            for (int i = 0; i < fieldValues.size(); i++) {
                String value = ((Element) (fieldValues.get(i))).getAttributeValue("value");

                if ((value != null) && (value.trim().length() > 0)) {
                    LOGGER.debug("MCRData2Fields " + def.getName() + " := " + value);
                    values.add(new MCRFieldValue(def, value));
                }
            }
    }

    /**
     * Transforms XML input to a list of field elements that contain name, type
     * and value of the field.
     */
    private static List transform(MCRFieldDef def, Document xml) {
        Document xsl = def.getStylesheet();
        if (xsl == null)
            return null;

        try {
            JDOMResult xmlres = new JDOMResult();
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(new JDOMSource(xsl));
            transformer.transform(new JDOMSource(xml), xmlres);

            List resultList = xmlres.getResult();
            Element root = (Element) (resultList.get(0));
            return root.getChildren();
        } catch (Exception ex) {
            String msg = "Exception while transforming metadata to search field";
            throw new MCRException(msg, ex);
        }
    }

    /** Standard format for a date value, as defined in fieldtypes.xml */
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Xalan XSL extension to convert MyCoRe date values to standard format. To
     * be used in a stylesheet or searchfields.xml configuration. Usage example:
     * &lt;field name="date" type="date"
     * xpath="/mycoreobject/metadata/dates/date"
     * value="ext:normalizeDate(string(normalize-space(text())),string(@xml:lang))"
     * &gt;
     * 
     * @param date
     *            the date string in a locale-dependent format
     * @param lang
     *            the xml:lang attribute of the date element
     */
    public static String normalizeDate(String date, String lang) {
        GregorianCalendar cal = new GregorianCalendar();

        try {
            DateFormat df = MCRUtils.getDateFormat(lang);
            cal.setTime(df.parse(date));
        } catch (ParseException e) {
            try {
                cal.setTime(sdf.parse(date));
            } catch (ParseException ex) {
                return "";
            }
        }

        return sdf.format(cal.getTime());
    }
}
