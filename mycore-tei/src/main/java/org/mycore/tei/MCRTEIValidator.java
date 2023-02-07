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

package org.mycore.tei;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * SAX Error handler for validating TEI XML
 */
public class MCRTEIValidator implements ErrorHandler {

    /**
     * XML Schema file for transcriptions
     */
    public static final String MCR_TRANSCRIPTION_SCHEMA = "xsd/mcrtranscr.xsd";

    /** 
     * Exception map key: fatalError 
     */
    public static final String FATAL_ERROR = "fatalError";

    /** 
     * Exception map key: error 
     */
    public static final String ERROR = "error";

    /** 
     * Exception map key: warning 
     */
    public static final String WARNING = "warning";

    private Hashtable<String, List<SAXParseException>> exceptionMap;

    private Source teiSource;

    /**
     * initialize TEI validator
     * @param teiSource - the TEI XML source
     */
    public MCRTEIValidator(Source teiSource) {
        this.teiSource = teiSource;

        this.exceptionMap = new Hashtable<>();
        this.exceptionMap.put(WARNING, new ArrayList<>());
        this.exceptionMap.put(ERROR, new ArrayList<>());
        this.exceptionMap.put(FATAL_ERROR, new ArrayList<>());
    }

    private Schema getSchema(String path) throws SAXException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", false);
        schemaFactory.setErrorHandler(this);
        return schemaFactory.newSchema(MCRTEIValidator.class.getClassLoader().getResource(
            path));
    }

    /**
     * run the TEI validator
     * @throws IOException - if the input cannot be read
     * @throws SAXException - if the XML parsing fails
     */
    public void validate() throws IOException, SAXException {
        Validator validator = this.getSchema(MCR_TRANSCRIPTION_SCHEMA).newValidator();
        validator.setErrorHandler(this);
        validator.validate(this.teiSource);
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {
        this.exceptionMap.get(WARNING).add(exception);
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
        this.exceptionMap.get(ERROR).add(exception);
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        this.exceptionMap.get(FATAL_ERROR).add(exception);
    }

    /**
     * @return the list of warnings
     */
    public List<SAXParseException> getWarnings() {
        return this.exceptionMap.get(WARNING);
    }

    /**
     * @return the list of errors
     */
    public List<SAXParseException> getErrors() {
        return this.exceptionMap.get(ERROR);
    }

    /**
     * @return the list of fatal errors
     */
    public List<SAXParseException> getFatals() {
        return this.exceptionMap.get(FATAL_ERROR);
    }

}
