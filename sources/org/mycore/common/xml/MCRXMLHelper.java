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

import java.util.Iterator;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Comment;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.ProcessingInstruction;
import org.jdom.Text;
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
            Object o = MCRConfiguration.instance().getInstanceOf("MCR.XMLParser.Class", "org.mycore.common.xml.MCRParserXerces");
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
     * Removes characters that are illegal in XML text nodes or attribute
     * values.
     * 
     * @param text
     *            the String that should be used in XML elements or attributes
     * @return the String with all illegal characters removed
     */
    public static String removeIllegalChars(String text) {
        if ((text == null) || (text.trim().length() == 0))
            return text;
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

    /**
     * checks whether two documents are equal.
     * 
     * This test performs a deep check across all child components of a
     * Document.
     * 
     * @param d1
     *            first Document to compare
     * @param d2
     *            second Document to compare
     * @return true, if d1 and d2 are deep equal
     * @see Document#equals(java.lang.Object)
     */
    public static boolean deepEqual(Document d1, Document d2) {
        return JDOMEquivalent.equivalent(d1, d2);
    }

    private static class JDOMEquivalent {

        private JDOMEquivalent() {
        }

        public static boolean equivalent(Document d1, Document d2) {
            return equivalentContent(d1, d2);
        }

        public static boolean equivalent(Element e1, Element e2) {
            return equivalentName(e1, e2) && equivalentAttributes(e1, e2) && equivalentContent(e1.getDescendants(), e2.getDescendants());
        }

        public static boolean equivalent(Text t1, Text t2) {
            String v1 = t1.getValue();
            String v2 = t2.getValue();
            return v1.equals(v2);
        }

        public static boolean equivalent(DocType d1, DocType d2) {
            return (((d1.getPublicID() == d2.getPublicID()) || d1.getPublicID().equals(d2.getPublicID())) && ((d1.getSystemID() == d2.getSystemID()) || d1
                    .getSystemID().equals(d2.getSystemID())));
        }

        public static boolean equivalent(Comment c1, Comment c2) {
            String v1 = c1.getValue();
            String v2 = c2.getValue();
            return v1.equals(v2);
        }

        public static boolean equivalent(ProcessingInstruction p1, ProcessingInstruction p2) {
            String t1 = p1.getTarget();
            String t2 = p2.getTarget();
            String d1 = p1.getData();
            String d2 = p2.getData();
            return t1.equals(t2) && d1.equals(d2);
        }

        public static boolean equivalent(Attribute a1, Attribute a2) {
            String v1 = a1.getValue();
            String v2 = a2.getValue();
            return equivalentName(a1, a2) && v1.equals(v2);
        }

        public static boolean equivalentAttributes(Element e1, Element e2) {
            List aList1 = e1.getAttributes();
            List aList2 = e2.getAttributes();
            if (aList1.size() != aList2.size()) {
                return false;
            }
            Iterator i1 = aList1.iterator();
            Iterator i2 = aList2.iterator();
            while (i1.hasNext()) {
                Attribute a1 = (Attribute) i1.next();
                Attribute a2 = (Attribute) i2.next();
                if (!equivalent(a1, a2)) {
                    return false;
                }
            }
            return true;
        }

        public static boolean equivalentContent(Document d1, Document d2) {
            // XXX short circuit if content size1 != content size2
            return equivalentContent(d1.getDescendants(), d2.getDescendants());
        }

        public static boolean equivalentContent(Element e1, Element e2) {
            // XXX short circuit if content size1 != content size2
            return equivalentContent(e1.getDescendants(), e2.getDescendants());
        }

        public static boolean equivalentContent(Iterator i1, Iterator i2) {
            boolean result = true;
            while (result && i1.hasNext() && i2.hasNext()) {
                Object o1 = i1.next();
                Object o2 = i2.next();
                if ((o1 instanceof Element) && (o2 instanceof Element)) {
                    result = equivalent((Element) o1, (Element) o2);
                    // XXX Hmm, this should work and avoid much recursion
                    // if we can guarentee i1, i2 are instances of
                    // DescendantIterator
                    //
                    // result = equivalentName((Element) o1, (Element) o2) &&
                    // equivalentAttributes((Element) o1, (Element) o2);
                } else if ((o1 instanceof Text) && (o2 instanceof Text)) {
                    result = equivalent((Text) o1, (Text) o2);
                } else if ((o1 instanceof Comment) && (o2 instanceof Comment)) {
                    result = equivalent((Comment) o1, (Comment) o2);
                } else if ((o1 instanceof ProcessingInstruction) && (o2 instanceof ProcessingInstruction)) {
                    result = equivalent((ProcessingInstruction) o1, (ProcessingInstruction) o2);
                } else if ((o1 instanceof DocType) && (o2 instanceof DocType)) {
                    result = equivalent((DocType) o1, (DocType) o2);
                } else {
                    result = false;
                }
            }
            return result;
        }

        public static boolean equivalentName(Element e1, Element e2) {
            Namespace ns1 = e1.getNamespace();
            String localName1 = e1.getName();

            Namespace ns2 = e2.getNamespace();
            String localName2 = e2.getName();

            return (ns1.equals(ns2)) && (localName1.equals(localName2));
        }

        public static boolean equivalentName(Attribute a1, Attribute a2) {
            Namespace ns1 = a1.getNamespace();
            String localName1 = a1.getName();

            Namespace ns2 = a2.getNamespace();
            String localName2 = a2.getName();

            return (ns1.equals(ns2)) && (localName1.equals(localName2));
        }
    }
}
