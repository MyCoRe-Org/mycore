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

package org.mycore.common.xml;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import org.apache.log4j.Logger;
import org.apache.xerces.parsers.SAXParser;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * Implements the MCRParserInterface using the Xerces XML to parse XML streams
 * to a DOM document.
 * 
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRParserXerces implements MCRParserInterface, ErrorHandler {

    /** The logger */
    private final static Logger LOGGER = Logger.getLogger(MCRParserXerces.class);

    /** A xerces parser instance that will validate */
    SAXBuilder builderValid;

    /** A xerces parser instance that will not validate */
    SAXBuilder builder;

    /** By default, validate xml or not? */
    private static boolean FLAG_VALIDATION = false;

    private static String FEATURE_NAMESPACES = "http://xml.org/sax/features/namespaces";

    private static String FEATURE_SCHEMA_SUPPORT = "http://apache.org/xml/features/validation/schema";

    private static String FEATURE_FULL_SCHEMA_SUPPORT = "http://apache.org/xml/features/validation/schema-full-checking";

    /**
     * Constructor for the Xerces parser. Sets default validation flag as
     * specified by the property MCR.XMLParser.ValidateSchema in
     * mycore.properties
     */
    public MCRParserXerces() {
        FLAG_VALIDATION = MCRConfiguration.instance().getBoolean("MCR.XMLParser.ValidateSchema", FLAG_VALIDATION);
        builderValid = new SAXBuilder("org.apache.xerces.parsers.SAXParser", true){
            protected XMLReader createParser() throws JDOMException {
                return new SAXParser();
            }
        };
        builderValid.setFeature(FEATURE_NAMESPACES, true);
        builderValid.setFeature(FEATURE_SCHEMA_SUPPORT, true);
        builderValid.setFeature(FEATURE_FULL_SCHEMA_SUPPORT, false);
        builderValid.setReuseParser(false);
        builderValid.setErrorHandler(this);
        builderValid.setEntityResolver(MCRURIResolver.instance());

        builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", false){
            protected XMLReader createParser() throws JDOMException {
                return new SAXParser();
            }
        };
        builder.setFeature(FEATURE_NAMESPACES, true);
        builder.setFeature(FEATURE_SCHEMA_SUPPORT, false);
        builder.setFeature(FEATURE_FULL_SCHEMA_SUPPORT, false);
        builder.setReuseParser(false);
        builder.setErrorHandler(this);
        builder.setEntityResolver(MCRURIResolver.instance());
    }

    /**
     * Parses the XML byte stream with xerces parser and returns a DOM document.
     * Uses the validation flag from mycore.properties.
     * 
     * @param uri
     *            the URI of the XML input stream
     * @throws MCRException
     *             if XML could not be parsed
     * @return the parsed XML stream as a DOM document
     */
    public Document parseURI(String uri) {
        return parseURI(uri, FLAG_VALIDATION);
    }

    /**
     * Parses the XML byte stream with xerces parser and returns a DOM document.
     * Uses the validation flag given.
     * 
     * @param uri
     *            the URI of the XML input stream
     * @param validate
     *            if true, will validate against XML Schema
     * @throws MCRException
     *             if XML could not be parsed
     * @return the parsed XML stream as a DOM document
     */
    public synchronized Document parseURI(String uri, boolean validate) {
        return parse(new InputSource(uri), validate);
    }

    /**
     * Parses the XML byte stream with xerces parser and returns a DOM document.
     * Uses the validation flag from mycore.properties
     * 
     * @param xml
     *            the XML byte stream
     * @throws MCRException
     *             if XML could not be parsed
     * @return the parsed XML stream as a DOM document
     */
    public Document parseXML(String xml) {
        return parseXML(xml, FLAG_VALIDATION);
    }

    /**
     * Parses the XML byte stream with xerces parser and returns a DOM document.
     * Uses the validation flag given.
     * 
     * @param xml
     *            the XML byte stream
     * @param validate
     *            if true, will validate against XML Schema
     * @throws MCRException
     *             if XML could not be parsed
     * @return the parsed XML stream as a DOM document
     */
    public Document parseXML(String xml, boolean validate) {
        InputSource source = new InputSource(new StringReader(xml));

        return parse(source, validate);
    }

    /**
     * Parses the XML byte stream with xerces parser and returns a DOM document.
     * Uses the validation flag from mycore.properties
     * 
     * @param xml
     *            the XML byte stream
     * @throws MCRException
     *             if XML could not be parsed
     * @return the parsed XML stream as a DOM document
     */
    public Document parseXML(byte[] xml) {
        return parseXML(xml, FLAG_VALIDATION);
    }

    /**
     * Parses the XML byte stream with xerces parser and returns a DOM document.
     * Uses the given validation flag.
     * 
     * @param xml
     *            the XML byte stream
     * @param validate
     *            if true, will validate against XML Schema
     * @throws MCRException
     *             if XML could not be parsed
     * @return the parsed XML stream as a DOM document
     */
    public Document parseXML(byte[] xml, boolean validate) {
        InputSource source = new InputSource(new ByteArrayInputStream(xml));
        return parse(source, validate);
    }

    /**
     * Parses the InputSource with xerces parser and returns a DOM document.
     * Uses the given validation flag.
     * 
     * @param source
     *            the XML InputSource
     * @param validate
     *            if true, will validate against XML Schema
     * @throws MCRException
     *             if XML could not be parsed
     * @return the parsed XML stream as a DOM document
     */
    private synchronized Document parse(InputSource source, boolean validate) {
        SAXBuilder builder = (validate ? this.builderValid : this.builder);

        try {
            return builder.build(source);
        } catch (Exception ex) {
            if (ex instanceof MCRException)
                throw (MCRException) ex;

            throw new MCRException(msg, ex);
        }
    }

    private final static String msg = "Error while parsing XML document: ";

    /**
     * Handles parser warnings
     */
    public void warning(SAXParseException ex) {
        LOGGER.warn(getSAXErrorMessage(ex), ex);
    }

    /**
     * Handles parse errors
     */
    public void error(SAXParseException ex) {
        LOGGER.error(getSAXErrorMessage(ex), ex);
        throw new MCRException(msg + getSAXErrorMessage(ex), ex);
    }

    /**
     * Handles fatal parse errors
     */
    public void fatalError(SAXParseException ex) {
        LOGGER.fatal(getSAXErrorMessage(ex));
        throw new MCRException(msg + getSAXErrorMessage(ex), ex);
    }

    /**
     * Returns a text indicating at which line and column the error occured.
     * 
     * @param ex
     *            the SAXParseException exception
     * @return the location string
     */
    private String getSAXErrorMessage(SAXParseException ex) {
        StringBuffer str = new StringBuffer();

        String systemId = ex.getSystemId();
        if (systemId != null) {
            int index = systemId.lastIndexOf('/');

            if (index != -1) {
                systemId = systemId.substring(index + 1);
            }

            str.append(systemId).append(": ");
        }

        str.append("line ").append(ex.getLineNumber());
        str.append(", column ").append(ex.getColumnNumber());
        str.append(", ");
        str.append(ex.getLocalizedMessage());

        return str.toString();
    }
}
