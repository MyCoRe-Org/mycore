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

package org.mycore.coma.model.validation;

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

public class MCRTEIValidator implements ErrorHandler {

    public static final String MCR_TRANSCRIPTION_SCHEMA = "xsd/mcrtranscr.xsd";

    public static final String FATAL_ERROR = "fatalError";

    public static final String ERROR = "error";

    public static final String WARNING = "warning";

    public MCRTEIValidator(Source teiSource) {
        this.teiSource = teiSource;

        this.exceptionMap = new Hashtable<>();
        this.exceptionMap.put("warning", new ArrayList<>());
        this.exceptionMap.put("error", new ArrayList<>());
        this.exceptionMap.put("fatalError", new ArrayList<>());
    }

    private Schema getSchema(String path) {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schemaFactory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", false);
            schemaFactory.setErrorHandler(this);
            return schemaFactory.newSchema(MCRTEIValidator.class.getClassLoader().getResource(
                path));
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    public void validate() throws IOException, SAXException {
        Validator validator = this.getSchema(MCR_TRANSCRIPTION_SCHEMA).newValidator();
        validator.setErrorHandler(this);

        try {
            validator.validate(this.teiSource);
        } catch (SAXException e) {
            throw e;
        }
    }

    private Hashtable<String, List<SAXParseException>> exceptionMap;

    private Source teiSource;

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

    public List<SAXParseException> getWarnings() {
        return this.exceptionMap.get(WARNING);
    }

    public List<SAXParseException> getErrors() {
        return this.exceptionMap.get(ERROR);
    }

    public List<SAXParseException> getFatals() {
        return this.exceptionMap.get(FATAL_ERROR);
    }

}
