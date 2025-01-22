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

package org.mycore.common.xml;

import static org.jdom2.JDOMConstants.SAX_FEATURE_NAMESPACES;
import static org.jdom2.JDOMConstants.SAX_FEATURE_NAMESPACE_PREFIXES;
import static org.jdom2.JDOMConstants.SAX_FEATURE_VALIDATION;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.jdom2.JDOMException;
import org.jdom2.input.sax.XMLReaderJDOMFactory;
import org.jdom2.input.sax.XMLReaderSAX2Factory;
import org.mycore.common.config.MCRConfiguration2;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Returns validating or non-validating XML parsers.
 * 
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRXMLParserFactory {

    private static final boolean VALIDATE_BY_DEFAULT = MCRConfiguration2.getBoolean("MCR.XMLParser.ValidateSchema")
        .orElse(true);

    private static final XMLReaderJDOMFactory NON_VALIDATING_FACTORY = new MCRXMLReaderSAX2Factory(false);

    private static final XMLReaderJDOMFactory VALIDATING_FACTORY = new MCRXMLReaderSAX2Factory(true);

    private static final ThreadLocal<MCRXMLParserImpl> NON_VALIDATING = ThreadLocal.withInitial(
        () -> new MCRXMLParserImpl(NON_VALIDATING_FACTORY));

    private static final ThreadLocal<MCRXMLParserImpl> VALIDATING = ThreadLocal.withInitial(
        () -> new MCRXMLParserImpl(VALIDATING_FACTORY));

    private static final ThreadLocal<MCRXMLParserImpl> NON_VALIDATING_SILENT = ThreadLocal.withInitial(
        () -> new MCRXMLParserImpl(NON_VALIDATING_FACTORY, true));

    private static final ThreadLocal<MCRXMLParserImpl> VALIDATING_SILENT = ThreadLocal.withInitial(
        () -> new MCRXMLParserImpl(VALIDATING_FACTORY, true));

    /** Returns a validating parser */
    public static MCRXMLParser getValidatingParser() {
        return VALIDATING.get();
    }

    /** Returns a non-validating parser */
    public static MCRXMLParser getNonValidatingParser() {
        return NON_VALIDATING.get();
    }

    /**
     * Returns a parser. The configuration property 
     * MCR.XMLParser.ValidateSchema (default false) will
     * determine if the parser will validate or not. 
     */
    public static MCRXMLParser getParser() {
        return VALIDATE_BY_DEFAULT ? VALIDATING.get() : NON_VALIDATING.get();
    }

    /** 
     * Returns a parser.
     * 
     * @param validate if true, the parser will validate the XML against the schema.
     */
    public static MCRXMLParser getParser(boolean validate) {
        return getParser(validate, false);
    }

    /** 
     * Returns a parser.
     * 
     * @param validate if true, the parser will validate the XML against the schema.
     * @param silent if true, exception's are not logged
     */
    public static MCRXMLParser getParser(boolean validate, boolean silent) {
        if (silent) {
            return validate ? VALIDATING_SILENT.get() : NON_VALIDATING_SILENT.get();
        } else {
            return validate ? VALIDATING.get() : NON_VALIDATING.get();
        }
    }

    /*
     * required in Java 10 as ClassLoader used in super does not find the driver
     */
    private static class MCRXMLReaderSAX2Factory extends XMLReaderSAX2Factory {

        MCRXMLReaderSAX2Factory(boolean validate) {
            super(validate);
        }

        @Override
        public XMLReader createXMLReader() throws JDOMException {
            try {
                XMLReader reader = MCRXMLHelper
                    .asSecureXMLReader(SAXParserFactory.newDefaultInstance().newSAXParser().getXMLReader());
                reader.setFeature(SAX_FEATURE_VALIDATION, isValidating());
                reader.setFeature(SAX_FEATURE_NAMESPACES, true);
                reader.setFeature(SAX_FEATURE_NAMESPACE_PREFIXES, true);
                return reader;
            } catch (SAXException | ParserConfigurationException e) {
                throw new JDOMException("Unable to create SAX2 XMLReader.", e);
            }
        }
    }
}
