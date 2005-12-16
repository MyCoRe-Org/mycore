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

import org.jdom.Document;
import org.jdom.Verifier;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

/**
 * This class provides some static utility methods to deal with XML/DOM
 * elements, nodes etc. The class *must* be considered as "work in progress"!
 * There is plenty left to do.
 * 
 * @author Detlev Degenhardt
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRXMLHelper {
    private static MCRParserInterface PARSER;

    /** Returns the XML Parser as configured in mycore.properties */
    private static MCRParserInterface getParser() throws MCRException {
        if (PARSER == null) {
            Object o = MCRConfiguration.instance().getInstanceOf("MCR.parser_class_name");
            PARSER = (MCRParserInterface) o;
        }

        return PARSER;
    }

    /**
     * Parses an XML file from a URI and returns it as DOM. Use the validation
     * value from mycore.properties.
     * 
     * @param uri
     *            the URI of the XML file
     * @throws MCRException
     *             if XML could not be parsed
     * @return the XML file as a DOM object
     */
    public static Document parseURI(String uri) throws MCRException {
        return getParser().parseURI(uri);
    }

    /**
     * Parses an XML file from a URI and returns it as DOM. Use the given
     * validation flag.
     * 
     * @param uri
     *            the URI of the XML file
     * @param valid
     *            the validation flag
     * @throws MCRException
     *             if XML could not be parsed
     * @return the XML file as a DOM object
     */
    public static Document parseURI(String uri, boolean valid) throws MCRException {
        return getParser().parseURI(uri, valid);
    }

    /**
     * Parses an XML String and returns it as DOM. Use the validation value from
     * mycore.properties.
     * 
     * @param xml
     *            the XML String to be parsed
     * @throws MCRException
     *             if XML could not be parsed
     * @return the XML file as a DOM object
     */
    public static Document parseXML(String xml) throws MCRException {
        return getParser().parseXML(xml);
    }

    /**
     * Parses an XML String and returns it as DOM. Use the given validation
     * flag.
     * 
     * @param xml
     *            the XML String to be parsed
     * @param valid
     *            the validation flag
     * @throws MCRException
     *             if XML could not be parsed
     * @return the XML file as a DOM object
     */
    public static Document parseXML(String xml, boolean valid) throws MCRException {
        return getParser().parseXML(xml, valid);
    }

    /**
     * Parses an Byte Array and returns it as DOM. Use the validation value from
     * mycore.properties.
     * 
     * @param xml
     *            the XML Byte Array to be parsed
     * @throws MCRException
     *             if XML could not be parsed
     * @return the XML file as a DOM object
     */
    public static Document parseXML(byte[] xml) throws MCRException {
        return getParser().parseXML(xml);
    }

    /**
     * Parses an Byte Array and returns it as DOM. Use the given validation
     * flag.
     * 
     * @param xml
     *            the XML Byte Array to be parsed
     * @param valid
     *            the validation flag
     * @throws MCRException
     *             if XML could not be parsed
     * @return the XML file as a DOM object
     */
    public static Document parseXML(byte[] xml, boolean valid) throws MCRException {
        return getParser().parseXML(xml, valid);
    }

    /**
     * Removes characters that are illegal in XML text nodes or attribute values.
     * 
     * @param text the String that should be used in XML elements or attributes
     * @return the String with all illegal characters removed
     */
    public static String removeIllegalChars(String text) {
        if ((text == null) || (text.trim().length() == 0))
            return null;
        if (org.jdom.Verifier.checkCharacterData(text) == null)
            return text;

        // It seems we have to filter out invalid XML characters...
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            if (Verifier.isXMLCharacter(text.charAt(i)))
                sb.append(text.charAt(i));
        }
        return sb.toString();
    }
}
