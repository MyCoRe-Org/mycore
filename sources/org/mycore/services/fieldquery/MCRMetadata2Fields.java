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

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * Transforms XML metadata to search field values for indexing searchable data.
 * Reads search field configuration from file searchfields.xml. This file and
 * the result of the transformation are described by the schema
 * searchfields.xsd.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRMetadata2Fields {
    /** The logger * */
    private static final Logger LOGGER = Logger.getLogger(MCRMetadata2Fields.class);

    public static final String METADATA="metadata";
    public static final String CONTENT="content";

    /**
     * Transforms XML stored in a MCRFile to search field values.
     * 
     * @param file
     *            the MCRFile, must contain xml document
     * @return a List of JDOM field elements with values, see searchfields.xsd
     *         for schema
     * @throws IOException
     *             if MCRFile content could not be read
     * @throws JDOMException
     *             if MCRFile xml content could not be parsed
     */
    public static List buildFields(MCRFile file, String definition) throws IOException, JDOMException {
        return buildFields(file.getContentAsJDOM(), file.getContentTypeID(), definition);
    }

    /**
     * Transforms XML metadata of MCRObject to search field values.
     * 
     * @param obj
     *            the MCRObject
     * @return a List of JDOM field elements with values, see searchfields.xsd
     *         for schema
     */
    public static List buildFields(MCRObject obj, String definition) {
        return buildFields(obj.createXML(), obj.getId().getTypeId(), definition);
    }

    /**
     * Transforms XML data in a JDOM document to search field values.
     * 
     * @param input
     *            the XML document
     * @param type
     *            the type of metadata
     * @return a List of JDOM field elements with values, see searchfields.xsd
     *         for schema
     */
    public static List buildFields(Document input, String type, String definition) {
        String uri = "resource:searchfields.xml";
        Element def = MCRURIResolver.instance().resolve(uri);

        List l =def.getChildren();
        for (int i=0; i<l.size(); i++){
            if(((Element)l.get(i)).getAttributeValue("id").equals(definition)){
                def = (Element) l.get(i);
                break;
            }
        }
        Document xsl = buildStylesheet(type, def);

        try {
            JDOMSource xslsrc = new JDOMSource(xsl);
            JDOMSource xmlsrc = new JDOMSource(input);
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

    /**
     * Builds stylesheet to transform a given type of metadata using a given
     * searchfields definition *
     */
    private static synchronized Document buildStylesheet(String type, Element definition) {
        Document xsl = (Document) (stylesheets.get(type));

        if (xsl == null) {
            Namespace xslns = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");
            Namespace mcrns = Namespace.getNamespace("mcr", "http://www.mycore.org/");
            Namespace xmlns = Namespace.getNamespace("xml", "http://www.w3.org/XML/1998/namespace");
            Namespace xlinkns = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
            Namespace xalanns = Namespace.getNamespace("xalan", "http://xml.apache.org/xalan");
            Namespace extns = Namespace.getNamespace("ext", "xalan://org.mycore.services.fieldquery.MCRMetadata2Fields");

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

            List fields = definition.getChildren("field", mcrns);

            for (int i = 0; i < fields.size(); i++) {
                Element field = (Element) ((Element) (fields.get(i))).clone();
                String xpath = field.getAttributeValue("xpath");

                if ((xpath == null) || (xpath.trim().length() == 0)) {
                    continue;
                }

                Element forEach = new Element("for-each", xslns);
                forEach.setAttribute("select", xpath);
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

            stylesheets.put(type, xsl);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("---------- stylesheet to build search fields ---------");

                XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
                LOGGER.debug(out.outputString(xsl));
                LOGGER.debug("------------------------------------------------------");
            }
        }

        return xsl;
    }

    /** Standard format for a date value, as defined in fieldtypes.xml * */
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

    /**
     * Test application, reads a metadata xml file from local filesystem and
     * builds search fields from it. If log level is DEBUG, output will show the
     * stylesheet that was used and the search fields that have been generated.
     */
    public static void main(String[] args) throws Exception {
        Document xml = new SAXBuilder().build(new File("c:\\demo-document.xml"));
        buildFields(xml, "document", METADATA);
    }
}
