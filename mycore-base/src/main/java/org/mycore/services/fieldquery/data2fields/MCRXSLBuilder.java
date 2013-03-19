/*
 * $Revision$ 
 * $Date$
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

package org.mycore.services.fieldquery.data2fields;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.services.fieldquery.MCRFieldDef;

public class MCRXSLBuilder {

    private final static Logger LOGGER = Logger.getLogger(MCRXSLBuilder.class);

    private final static Namespace xalanns = Namespace.getNamespace("xalan", "http://xml.apache.org/xalan");

    private final static Namespace extns = Namespace.getNamespace("ext", "xalan://" + MCRXSLBuilder.class.getName());

    private Document document;

    private Element fieldValues;

    public MCRXSLBuilder() {
        Element stylesheet = new Element("stylesheet", MCRConstants.XSL_NAMESPACE);
        document = new Document(stylesheet);

        stylesheet.setAttribute("version", "1.0");
        stylesheet.setAttribute("extension-element-prefixes", "ext");

        setAdditionalNamespaces(stylesheet);

        Element template = new Element("template", MCRConstants.XSL_NAMESPACE);
        template.setAttribute("match", "/");
        stylesheet.addContent(template);

        fieldValues = new Element("fieldValues", MCRConstants.MCR_NAMESPACE);
        template.addContent(fieldValues);
    }

    private void setAdditionalNamespaces(Element stylesheet) {
        Set<Namespace> namespaces = new HashSet<Namespace>();
        namespaces.add(Namespace.XML_NAMESPACE);
        namespaces.add(xalanns);
        namespaces.add(extns);
        namespaces.addAll(MCRConstants.getStandardNamespaces());
        namespaces.addAll(MCRFieldDef.getAllNamespaces());

        for (Namespace namespace : namespaces)
            stylesheet.addNamespaceDeclaration(namespace);
    }

    public void addXSLForField(MCRFieldDef field) {

        fieldValues.addContent(field.getXSLContent());

        String xpath = field.getXPathAttribute();
        String value = field.getValueAttribute();

        if (xpath != null) {
            // <xsl:for-each select="{@xpath}">
            Element forEach = new Element("for-each", MCRConstants.XSL_NAMESPACE);
            forEach.setAttribute("select", xpath);
            fieldValues.addContent(forEach);

            if ("objectCategory".equals(field.getSource())) {
                // current(): <format classid="DocPortal_class_00000006"
                // categid="FORMAT0002"/>
                // URI: classification:metadata:levels:parents:{class}:{categ}
                Element forEach2 = new Element("for-each", MCRConstants.XSL_NAMESPACE);
                forEach.addContent(forEach2);
                String uri = "document(concat('classification:metadata:0:parents:',current()/@classid,':',current()/@categid))//category";
                forEach2.setAttribute("select", uri);
                forEach = forEach2;
            }

            // <xsl:value-of select="{@value}" />
            Element valueOf = new Element("value-of", MCRConstants.XSL_NAMESPACE);
            valueOf.setAttribute("select", value);

            // <name>value</name>
            Element fieldValue = new Element(field.getName(), MCRConstants.MCR_NAMESPACE);
            fieldValue.addContent(valueOf);

            forEach.addContent(fieldValue);
        }
    }

    public Document getStylesheet() {
        return document;
    }

    public String toString() {
        return new XMLOutputter(Format.getPrettyFormat()).outputString(document);
    }

    /**
     * Xalan XSL extension to convert MyCoRe date values to standard format. To
     * be used in a stylesheet or searchfields.xml configuration. Usage example:
     * &lt;field name="date" type="date"
     * xpath="/mycoreobject/metadata/dates/date"
     * value="ext:normalizeDate(string(text()))" &gt;
     * 
     * @param sDate
     *            the date string in a locale-dependent format
     */
    public static String normalizeDate(String sDate) {
        try {
            MCRISO8601Date iDate = new MCRISO8601Date();
            iDate.setDate(sDate.trim());
            String isoDateString = iDate.getISOString();
            if(isoDateString == null){
                return "";
            }
            
            if (isoDateString.length() < 10) {
                return isoDateString;
            }
            
            return isoDateString.substring(0, 10);
        } catch (Exception ex) {
            LOGGER.debug(ex);
            return "";
        }
    }
}
