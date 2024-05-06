/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.common.util;

import net.sf.saxon.jaxp.TransformerImpl;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.xml.MCRURIResolver;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.util.Map;

public class MCRTestCaseXSLTUtil {
    /**
     * Returns an XML document with the given method name as root element and the content of the given XML tree as
     * child element. The resulting document can be used for calling XSLT test files using template matching.
     *
     * @param method the method name used as root element name
     * @param xml    the XML tree
     * @return an XML document with the method name as root element and the content of the given XML tree as children
     */
    public static Document xmlTest(String method, Element xml) {
        final Element root = new Element(method);
        root.addContent(xml.detach());

        return new Document(root);
    }

    /**
     * Transforms the XML document using the given XSL stylesheet with the given parameters.
     *
     * @param parameters the XSL parameters
     * @param xsl        the XSL stylesheet file used for parsing
     * @param xml        the XML document to parse
     */
    public static Document transform(Document xml, String xsl, Map<String, Object> parameters)
        throws TransformerException {
        Source xslt = new StreamSource(MCRTestCaseXSLTUtil.class.getResourceAsStream(xsl));
        JDOMResult result = new JDOMResult();

        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setURIResolver(MCRURIResolver.instance());

        Transformer transformer = factory.newTransformer(xslt);
        parameters.forEach(transformer::setParameter);

        if (transformer instanceof TransformerImpl transformerImpl) {
            transformerImpl.getUnderlyingXsltTransformer()
                .setMessageHandler(msg -> System.err.println(msg.getContent().getStringValue()));
        }

        transformer.transform(new JDOMSource(xml), result);

        return new Document(result.getResult());
    }
}
