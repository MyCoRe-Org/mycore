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

import java.util.HashMap;
import java.util.Map;

import org.mycore.common.MCRConfiguration;

/**
 * Returns validating or non-validating XML parsers.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXMLParserFactory {

    private final static String PROPERTY_PARSER_CLASS = "MCR.XMLParser.Class";

    private final static String PROPERTY_VALIDATE_BY_DEFAULT = "MCR.XMLParser.ValidateSchema";

    private final static String DEFAULT_PARSER_CLASS = "org.mycore.common.xml.MCRXMLParserXerces";

    private static Map<Boolean, MCRXMLParser> parsers;

    private static boolean validateByDefault = false;

    static {
        validateByDefault = MCRConfiguration.instance().getBoolean(PROPERTY_VALIDATE_BY_DEFAULT, true);
        parsers = new HashMap<Boolean, MCRXMLParser>();
        parsers.put(false, buildParser(false));
        parsers.put(true, buildParser(true));
    }

    private static MCRXMLParser buildParser(boolean validate) {
        Object o = MCRConfiguration.instance().getInstanceOf(PROPERTY_PARSER_CLASS, DEFAULT_PARSER_CLASS);
        MCRXMLParser parser = (MCRXMLParser) o;
        parser.setValidating(validate);
        return parser;
    }

    /** Returns a validating parser */
    public static MCRXMLParser getValidatingParser() {
        return parsers.get(true);
    }

    /** Returns a non-validating parser */
    public static MCRXMLParser getNonValidatingParser() {
        return parsers.get(false);
    }

    /**
     * Returns a parser. The configuration property 
     * MCR.XMLParser.ValidateSchema (default false) will
     * determine if the parser will validate or not. 
     */
    public static MCRXMLParser getParser() {
        return parsers.get(validateByDefault);
    }

    /** 
     * Returns a parser.
     * 
     * @param validate if true, the parser will validate the XML against the schema.
     */
    public static MCRXMLParser getParser(boolean validate) {
        return parsers.get(validate);
    }
}
