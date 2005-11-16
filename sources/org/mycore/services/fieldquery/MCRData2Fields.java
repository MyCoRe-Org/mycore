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
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.ifs.MCRAudioVideoExtender;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * Provides methods to automatically extract field values for indexing from
 * MCRObject or MCRFile using the definition in searchfields.xml. The
 * buildFields method return a list of MCRSearchField objects with values
 * extracted from the object for the given search index. This class supports
 * extracting values from MCRObject metadata, MCRFile metadata, MCRFile xml
 * content and MCRFile text content using text filter plug-in.
 * 
 * @see MCRSearcherBase#addToIndex(String, List)
 * 
 * @author Frank Lützenkirchen
 */
public class MCRData2Fields {
    /** The logger */
    private static final Logger LOGGER = Logger.getLogger(MCRData2Fields.class);

    /** Table indexID to List of search field elements from searchfields.xml */
    private static Hashtable indexTable = new Hashtable();

    /** Reads searchfields.xml and fills the internal table of index definitions * */
    private static synchronized void buildIndexTable() {
        Namespace mcrns = Namespace.getNamespace("mcr", "http://www.mycore.org/");

        String uri = "resource:searchfields.xml";
        Element def = MCRURIResolver.instance().resolve(uri);

        List children = def.getChildren("index", mcrns);

        for (int i = 0; i < children.size(); i++) {
            Element index = (Element) (children.get(i));
            String id = index.getAttributeValue("id");
            indexTable.put(id, index.getChildren("field", mcrns));
        }
    }

    /**
     * Extracts field values for indexing from the given MCRObject's metadata.
     * 
     * @param obj
     *            the MCRObject thats metadata should be indexed
     * @param index
     *            the ID of the index as defined in searchfields.xml
     * @return a List of MCRSearchField objects that contain name, type and
     *         value
     */
    public static List buildFields(MCRObject obj, String index) {
        return buildFields(obj, obj.getId().getTypeId(), index);
    }

    /**
     * Extracts field values for indexing from the given MCRFile's metadata, xml
     * content or text content.
     * 
     * @param obj
     *            the MCRFile thats data should be indexed
     * @param index
     *            the ID of the index as defined in searchfields.xml
     * @return a List of MCRSearchField objects that contain name, type and
     *         value
     */
    public static List buildFields(MCRFile file, String index) {
        return buildFields(file, file.getContentTypeID(), index);
    }
    
    /**
     * Extracts field values for indexing from the given JDOM xml document.
     * 
     * @param doc
     *            the JDOM xml document thats data should be indexed
     * @param index
     *            the ID of the index as defined in searchfields.xml
     * @return a List of MCRSearchField objects that contain name, type and
     *         value
     */
    public static List buildFields(Document doc, String index) {
        return buildFields(doc, doc.getRootElement().getName(), index);
    }

    private static List buildFields(Object obj, String type, String index) {
        if (indexTable.isEmpty()) {
            buildIndexTable();
        }

        List values = new LinkedList();

        // Are there any search fields defined for this index?
        List fieldDefList = (List) (indexTable.get(index));

        if (fieldDefList == null) {
            return values;
        }

        List xmlFields = new LinkedList();
        List fileFields = new LinkedList();

        for (int i = 0; i < fieldDefList.size(); i++) {
            Element fieldDef = (Element) (fieldDefList.get(i));
            String typeList = fieldDef.getAttributeValue("objects", type);

            // Is this field irrelevant for the current object type?
            if ((" " + typeList + " ").indexOf(" " + type + " ") == -1) {
                continue;
            }

            String defau = "xml";
            if (obj instanceof MCRFile)
                defau = "file.metadata";
            if (obj instanceof MCRObject)
                defau = "object.metadata";
            
            String source = fieldDef.getAttributeValue("source", defau);

            if ("object.metadata".equals(source) || "file.xml".equals(source) || "xml".equals(source)) {
                xmlFields.add(fieldDef);
            } else if ("file.metadata".equals(source)) {
                fileFields.add(fieldDef);
            } else if ("file.textfilter".equals(source)) {
                if (obj instanceof MCRFile) {
                    MCRSearchField field = new MCRSearchField();
                    field.setName(fieldDef.getAttributeValue("name"));
                    field.setFile((MCRFile) obj);
                    values.add(field);
                }
            }
        }

        if (!xmlFields.isEmpty()) {
            Document xml = getXML(obj);

            if (xml != null) {
                addTransformedFields(xml, index, xmlFields, values);
            }
        }

        if ((!fileFields.isEmpty()) && (obj instanceof MCRFile)) {
            Document xml = buildXML((MCRFile) obj);
            addTransformedFields(xml, index, fileFields, values);
        }

        return values;
    }

    /**
     * Build a XML representation of all technical metadata of this MCRFile and
     * its MCRAudioVideoExtender, if present. That xml can be used for indexing
     * this data.
     */
    private static Document buildXML(MCRFile file) {
        Element root = new Element("file");
        root.setAttribute("id", file.getID());
        root.setAttribute("owner", file.getOwnerID());
        root.setAttribute("name", file.getName());
        root.setAttribute("path", file.getAbsolutePath());
        root.setAttribute("size", Long.toString(file.getSize()));
        root.setAttribute("extension", file.getExtension());
        root.setAttribute("contentTypeID", file.getContentTypeID());
        root.setAttribute("contentType", file.getContentType().getLabel());
        root.setAttribute("modified", sdf.format(file.getLastModified().getTime()));

        if (file.hasAudioVideoExtender()) {
            MCRAudioVideoExtender ext = file.getAudioVideoExtender();
            root.setAttribute("bitRate", String.valueOf(ext.getBitRate()));
            root.setAttribute("frameRate", String.valueOf(ext.getFrameRate()));
            root.setAttribute("duration", ext.getDurationTimecode());
            root.setAttribute("mediaType", ((ext.getMediaType() == MCRAudioVideoExtender.AUDIO) ? "audio" : "video"));
        }

        return new Document(root);
    }

    /** Transforms xml input to search field values using XSL * */
    private static void addTransformedFields(Document xml, String index, List fields, List values) {
        List fieldValues = transform(xml, index, fields);

        for (int i = 0; i < fieldValues.size(); i++) {
            Element fv = (Element) (fieldValues.get(i));

            String value = fv.getAttributeValue("value");

            if ((value == null) || (value.trim().length() == 0)) {
                continue;
            }

            MCRSearchField field = new MCRSearchField();
            field.setName(fv.getAttributeValue("name"));
            field.setValue(value);
            field.setSortable("true".equals(fv.getAttributeValue("sortable")) ? true : false);
            values.add(field);
        }
    }

    /**
     * For MCRObject, returns the object's metadata as XML, for MCRFile, returns
     * the file's content as XML, or null.
     */
    private static Document getXML(Object obj) {
        if (obj instanceof Document) {
            return (Document)obj;
        }
        else if (obj instanceof MCRFile) {
            MCRFile file = (MCRFile) obj;

            try {
                return file.getContentAsJDOM();
            } catch (Exception ex) {
                String msg = "Exception while indexing XML content of MCRFile";
                LOGGER.warn(msg, ex);

                return null;
            }
        } else {
            return ((MCRObject) obj).createXML();
        }
    }

    /**
     * Transforms XML input to a list of field elements that contain name, type
     * and value of the field.
     */
    private static List transform(Document xml, String index, List xmlFields) {
        Document xsl = buildStylesheet(index, xmlFields);

        try {
            JDOMSource xslsrc = new JDOMSource(xsl);
            JDOMSource xmlsrc = new JDOMSource(xml);
            JDOMResult xmlres = new JDOMResult();
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(xslsrc);
            transformer.transform(xmlsrc, xmlres);

            List resultList = xmlres.getResult();
            Element root = (Element) (resultList.get(0));

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("---------- search fields ---------");

                XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
                LOGGER.debug(out.outputString(root.getChildren()));
                LOGGER.debug("----------------------------------");
            }

            return root.getChildren();
        } catch (Exception ex) {
            String msg = "Exception while transforming metadata to search fields";
            throw new MCRException(msg, ex);
        }
    }

    /** Cached stylesheets for metadata to searchfield value transformation * */
    private static MCRCache stylesheets = new MCRCache(20);

    /** Build a stylesheet from a list of search field definitions * */
    private static Document buildStylesheet(String index, List fields) {
        String key = buildKey(index, fields);
        Document xsl = (Document) (stylesheets.get(key));

        if (xsl == null) {
            Namespace xslns = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");
            Namespace mcrns = Namespace.getNamespace("mcr", "http://www.mycore.org/");
            Namespace xmlns = Namespace.getNamespace("xml", "http://www.w3.org/XML/1998/namespace");
            Namespace xlinkns = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
            Namespace xalanns = Namespace.getNamespace("xalan", "http://xml.apache.org/xalan");
            Namespace extns = Namespace.getNamespace("ext", "xalan://org.mycore.services.fieldquery.MCRData2Fields");

            Element stylesheet = new Element("stylesheet");
            stylesheet.setAttribute("version", "1.0");
            stylesheet.setNamespace(xslns);
            stylesheet.addNamespaceDeclaration(xmlns);
            stylesheet.addNamespaceDeclaration(xlinkns);
            stylesheet.addNamespaceDeclaration(xalanns);
            stylesheet.addNamespaceDeclaration(extns);
            stylesheet.setAttribute("extension-element-prefixes", "ext");

            xsl = new Document(stylesheet);

            Element template = new Element("template", xslns);
            template.setAttribute("match", "/");
            stylesheet.addContent(template);

            Element searchfields = new Element("searchfields", mcrns);
            template.addContent(searchfields);

            for (int i = 0; i < fields.size(); i++) {
                Element field = (Element) ((Element) (fields.get(i))).clone();
                String xpath = field.getAttributeValue("xpath");

                if ((xpath == null) || (xpath.trim().length() == 0)) {
                    continue;
                }

                Element forEach = new Element("for-each", xslns);
                forEach.setAttribute("select", xpath);

                field.removeAttribute("source");
                field.removeAttribute("objects");
                field.removeAttribute("xpath");

                searchfields.addContent(forEach);
                forEach.addContent(field);
                field.setAttribute("value", "{" + field.getAttributeValue("value") + "}");

                List attributes = field.getChildren("attribute", mcrns);

                for (int j = 0; j < attributes.size(); j++) {
                    Element attribute = (Element) (attributes.get(j));
                    attribute.setAttribute("value", "{" + attribute.getAttributeValue("value") + "}");
                }
            }

            stylesheets.put(key, xsl);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("---------- stylesheet to build search fields ---------");

                XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
                LOGGER.debug(out.outputString(xsl));
                LOGGER.debug("------------------------------------------------------");
            }
        }

        return xsl;
    }

    /** Build a key for caching and re-using stylesheets * */
    private static String buildKey(String index, List fields) {
        StringBuffer key = new StringBuffer(index).append(':');

        for (int i = 0; i < fields.size(); i++)
            key.append(((Element) (fields.get(i))).getAttributeValue("name"));

        return key.toString();
    }

    /** Standard format for a date value, as defined in fieldtypes.xml */
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Xalan XSL extension to convert MyCoRe date values to standard format. To
     * be used in a stylesheet or searchfields.xml configuration. Usage example:
     * 
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
