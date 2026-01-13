/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.xml.MCRURIResolver;

import net.sf.saxon.jaxp.TransformerImpl;
import net.sf.saxon.s9api.Message;

/**
 * Provides utility methods for testing XSLT functions. The general process for XSL testing is as follows:
 * <ol>
 *     <li>create a test XSL file containing match templates calling functions/templates to test</li>
 *     <li>call prepareTestDocument with the rootName matching the test template (and possible XML content)</li>
 *     <li>call transform with the test document and evaluate the result document</li>
 * </ol>
 */
public class MCRTestCaseXSLTUtil {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String ERROR_XTMM9000 = "XTMM9000";
    private static final String ERROR_XTMM9001 = "XTMM9001";

    /**
     * Returns an XML document with a root element with the given name
     *
     * @param rootName the root element name of the document
     * @return an XML document with a root element with the given name
     */
    public static Document prepareTestDocument(String rootName) {
        return new Document().addContent(new Element(rootName));
    }

    /**
     * Returns an XML document with the given name as root element and the content of the given XML tree as
     * child element. The resulting document can be used for calling XSLT test files using template matching.
     *
     * @param rootName the name used as root element name
     * @param xml      the XML tree
     * @return an XML document with the name as root element and the content of the given XML tree as children
     */
    public static Document prepareTestDocument(String rootName, Element xml) {
        return prepareTestDocument(rootName, Collections.singletonList(xml));
    }

    /**
     * Returns an XML document with the given name as root element and the content of the given XML tree as
     * child elements. The resulting document can be used for calling XSLT test files using template matching.
     *
     * @param rootName the name used as root element name
     * @param xml      the XML tree
     * @return an XML document with the name as root element and the content of the given XML tree as children
     */
    public static Document prepareTestDocument(String rootName, List<? extends Content> xml) {
        final Element root = new Element(rootName);

        for (Content xmlContent : xml) {
            root.addContent(xmlContent.detach());
        }

        return new Document(root);
    }

    /**
     * Transforms the XML document using the given XSL stylesheet from classpath with the given parameters.
     *
     * @param xml        the XML document to parse
     * @param xsl        the XSL stylesheet file used for parsing
     * @param parameters the XSL transformation parameters
     */
    public static Document transform(Document xml, String xsl, Map<String, Object> parameters)
        throws TransformerException {
        Source xslt = new StreamSource(MCRTestCaseXSLTUtil.class.getResourceAsStream(xsl));
        JDOMResult result = new JDOMResult();

        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setURIResolver(MCRURIResolver.obtainInstance());

        Transformer transformer = factory.newTransformer(xslt);
        parameters.forEach(transformer::setParameter);

        if (transformer instanceof TransformerImpl transformerImpl) {
            transformerImpl.getUnderlyingXsltTransformer().setMessageHandler(MCRTestCaseXSLTUtil::log);
        }

        transformer.transform(new JDOMSource(xml), result);

        return new Document(result.getResult());
    }

    /**
     * Logs an XSL message to the system logger.
     *
     * @param message the message to log
     */
    private static void log(Message message) {
        // error codes from https://www.w3.org/2005/xqt-errors/:
        // XTMM9000, XTMM9001 are messages; other codes are warnings/errors
        if (Strings.CS.equalsAny(message.getErrorCode().getLocalName(), ERROR_XTMM9000, ERROR_XTMM9001)) {
            LOGGER.info(message.getContent());
        } else {
            LOGGER.error(message.getContent());
        }
    }
}
