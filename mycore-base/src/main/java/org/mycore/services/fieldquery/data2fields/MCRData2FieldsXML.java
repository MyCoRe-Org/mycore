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

import java.util.Collections;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMResult;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRFieldValue;

public class MCRData2FieldsXML implements MCRData2Fields {

    /** The XSL transformer factory to use */
    public static SAXTransformerFactory factory;

    static {
        TransformerFactory tf = TransformerFactory.newInstance();
        if (!tf.getFeature(SAXTransformerFactory.FEATURE)) {
            throw new MCRConfigurationException("Could not load a SAXTransformerFactory for use with XSLT");
        }

        factory = (SAXTransformerFactory) tf;
        factory.setURIResolver(MCRURIResolver.instance());
    }

    private MCRContent xmlSource;

    private MCRFieldsSelector selector;

    public MCRData2FieldsXML(String index, Document xml) {
        this(new MCRJDOMContent(xml), new MCRFieldsSelector(index, xml.getRootElement().getName(), "xml"));
    }

    public MCRData2FieldsXML(MCRContent xmlSource, MCRFieldsSelector selector) {
        this.xmlSource = xmlSource;
        this.selector = selector;
    }

    public void addFieldValues(MCRIndexEntry entry) {
        Source sourceXML = null;
        try {
            sourceXML = xmlSource.getSource();
        } catch (Exception ignored) {
        }

        if (sourceXML == null)
            return;

        Templates stylesheet = MCRTemplatesFactory.getTemplates(selector);
        List<Element> elements = transform(sourceXML, stylesheet);
        addValuesFromElements(entry, elements);
    }

    @SuppressWarnings("unchecked")
    private static List<Element> transform(Source sourceXML, Templates stylesheet) {
        try {
            JDOMResult result = new JDOMResult();
            Transformer transformer = factory.newTransformerHandler(stylesheet).getTransformer();
            transformer.transform(sourceXML, result);

            List results = result.getResult();
            if (results.isEmpty())
                return Collections.EMPTY_LIST;

            Element fieldValues = (Element) (results.get(0));
            return (List<Element>) (fieldValues.getChildren());
        } catch (Exception ex) {
            String msg = "Exception while transforming metadata to search field";
            throw new MCRException(msg, ex);
        }
    }

    private static void addValuesFromElements(MCRIndexEntry entry, List<Element> elements) {
        if (elements != null)
            for (Element element : elements)
                addValueFromElement(entry, element);
    }

    private static void addValueFromElement(MCRIndexEntry entry, Element element) {
        String value = element.getTextTrim();
        if ((value == null) || value.isEmpty())
            return;
        String name = element.getName();
        entry.addValue(new MCRFieldValue(name, value));
    }
}
