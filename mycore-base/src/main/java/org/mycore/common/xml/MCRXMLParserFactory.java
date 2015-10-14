/*
 * 
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

import org.jdom2.input.sax.XMLReaderJDOMFactory;
import org.jdom2.input.sax.XMLReaderSAX2Factory;
import org.mycore.common.config.MCRConfiguration;

/**
 * Returns validating or non-validating XML parsers.
 * 
 * @author Frank L\u00FCtzenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRXMLParserFactory {
    private static boolean VALIDATE_BY_DEFAULT = MCRConfiguration.instance().getBoolean("MCR.XMLParser.ValidateSchema",
        true);

    private final static String XMLREADER_CLASS_NAME = "org.apache.xerces.parsers.SAXParser";

    private static XMLReaderJDOMFactory nonValidatingFactory = new XMLReaderSAX2Factory(false, XMLREADER_CLASS_NAME);

    private static XMLReaderJDOMFactory validatingFactory = new XMLReaderSAX2Factory(true, XMLREADER_CLASS_NAME);

    private static ThreadLocal<MCRXMLParserImpl> nonValidating = new ThreadLocal<MCRXMLParserImpl>() {
        @Override
        protected MCRXMLParserImpl initialValue() {
            return new MCRXMLParserImpl(nonValidatingFactory);
        }
    };

    private static ThreadLocal<MCRXMLParserImpl> validating = new ThreadLocal<MCRXMLParserImpl>() {
        @Override
        protected MCRXMLParserImpl initialValue() {
            return new MCRXMLParserImpl(validatingFactory);
        }
    };

    private static ThreadLocal<MCRXMLParserImpl> nonValidatingSilent = new ThreadLocal<MCRXMLParserImpl>() {
        @Override
        protected MCRXMLParserImpl initialValue() {
            return new MCRXMLParserImpl(nonValidatingFactory, true);
        }
    };

    private static ThreadLocal<MCRXMLParserImpl> validatingSilent = new ThreadLocal<MCRXMLParserImpl>() {
        @Override
        protected MCRXMLParserImpl initialValue() {
            return new MCRXMLParserImpl(validatingFactory, true);
        }
    };

    /** Returns a validating parser */
    public static MCRXMLParser getValidatingParser() {
        return validating.get();
    }

    /** Returns a non-validating parser */
    public static MCRXMLParser getNonValidatingParser() {
        return nonValidating.get();
    }

    /**
     * Returns a parser. The configuration property 
     * MCR.XMLParser.ValidateSchema (default false) will
     * determine if the parser will validate or not. 
     */
    public static MCRXMLParser getParser() {
        return VALIDATE_BY_DEFAULT ? validating.get() : nonValidating.get();
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
            return validate ? validatingSilent.get() : nonValidatingSilent.get();
        } else {
            return validate ? validating.get() : nonValidating.get();
        }
    }
}
