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

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Comment;
import org.jdom2.Content;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Parent;
import org.jdom2.ProcessingInstruction;
import org.jdom2.Text;
import org.jdom2.Verifier;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.streams.MCRByteArrayOutputStream;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class provides some static utility methods to deal with XML/DOM
 * elements, nodes etc.
 * 
 * @author Detlev Degenhardt
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRXMLHelper {

    private static final Logger LOGGER = Logger.getLogger(MCRXMLHelper.class);

    /**
     * Removes characters that are illegal in XML text nodes or attribute
     * values.
     * 
     * @param text
     *            the String that should be used in XML elements or attributes
     * @return the String with all illegal characters removed
     */
    public static String removeIllegalChars(String text) {
        if (text == null || text.trim().length() == 0) {
            return text;
        }
        if (org.jdom2.Verifier.checkCharacterData(text) == null) {
            return text;
        }

        // It seems we have to filter out invalid XML characters...
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (Verifier.isXMLCharacter(text.charAt(i))) {
                sb.append(text.charAt(i));
            }
        }
        return sb.toString();
    }

    /**
     * validates <code>doc</code> using XML Schema defined <code>schemaURI</code>
     * @param doc document to be validated
     * @param schemaURI URI of XML Schema document
     * @throws SAXException if validation fails
     * @throws IOException if resolving resources fails
     */
    public static void validate(Document doc, String schemaURI) throws SAXException, IOException {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        sf.setResourceResolver(MCREntityResolver.instance());
        Schema schema;
        try {
            schema = sf.newSchema(MCRURIResolver.instance().resolve(schemaURI, null));
        } catch (TransformerException e) {
            Throwable cause = e.getCause();
            if (cause == null) {
                throw new IOException(e);
            }
            if (cause instanceof SAXException) {
                throw (SAXException) cause;
            }
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new IOException(e);
        }
        Validator validator = schema.newValidator();
        validator.setResourceResolver(MCREntityResolver.instance());
        validator.validate(new JDOMSource(doc));
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
        try {
            return JDOMEquivalent.equivalent(canonicalElement(d1), canonicalElement(d2));
        } catch (Exception e) {
            LOGGER.warn("Could not compare documents.", e);
            return false;
        }
    }

    /**
     * checks whether two elements are equal.
     * 
     * This test performs a deep check across all child components of a
     * element.
     * 
     * @param e1
     *            first Element to compare
     * @param e2
     *            second Element to compare
     * @return true, if e1 and e2 are deep equal
     * @see Document#equals(java.lang.Object)
     */
    public static boolean deepEqual(Element e1, Element e2) {
        try {
            return JDOMEquivalent.equivalent(canonicalElement(e1), canonicalElement(e2));
        } catch (Exception e) {
            LOGGER.warn("Could not compare elements.", e);
            return false;
        }
    }

    private static Element canonicalElement(Parent e) throws IOException, SAXParseException {
        XMLOutputter xout = new XMLOutputter(Format.getCompactFormat());
        MCRByteArrayOutputStream bout = new MCRByteArrayOutputStream();
        if (e instanceof Element) {
            xout.output((Element) e, bout);
        } else {
            xout.output((Document) e, bout);
        }
        Document xml = MCRXMLParserFactory.getNonValidatingParser().parseXML(
            new MCRByteContent(bout.getBuffer(), 0, bout.size()));
        return xml.getRootElement();
    }

    private static class JDOMEquivalent {

        private JDOMEquivalent() {
        }

        public static boolean equivalent(Element e1, Element e2) {
            return equivalentName(e1, e2) && equivalentAttributes(e1, e2)
                && equivalentContent(e1.getContent(), e2.getContent());
        }

        public static boolean equivalent(Text t1, Text t2) {
            String v1 = t1.getValue();
            String v2 = t2.getValue();
            boolean equals = v1.equals(v2);
            if (!equals && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Text differs \"" + t1 + "\"!=\"" + t2 + "\"");
            }
            return equals;
        }

        public static boolean equivalent(DocType d1, DocType d2) {
            boolean equals = d1.getPublicID().equals(d2.getPublicID()) && d1.getSystemID().equals(d2.getSystemID());
            if (!equals && LOGGER.isDebugEnabled()) {
                LOGGER.debug("DocType differs \"" + d1 + "\"!=\"" + d2 + "\"");
            }
            return equals;
        }

        public static boolean equivalent(Comment c1, Comment c2) {
            String v1 = c1.getValue();
            String v2 = c2.getValue();
            boolean equals = v1.equals(v2);
            if (!equals && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Comment differs \"" + c1 + "\"!=\"" + c2 + "\"");
            }
            return equals;
        }

        public static boolean equivalent(ProcessingInstruction p1, ProcessingInstruction p2) {
            String t1 = p1.getTarget();
            String t2 = p2.getTarget();
            String d1 = p1.getData();
            String d2 = p2.getData();
            boolean equals = t1.equals(t2) && d1.equals(d2);
            if (!equals && LOGGER.isDebugEnabled()) {
                LOGGER.debug("ProcessingInstruction differs \"" + p1 + "\"!=\"" + p2 + "\"");
            }
            return equals;
        }

        public static boolean equivalentAttributes(Element e1, Element e2) {
            List<Attribute> aList1 = e1.getAttributes();
            List<Attribute> aList2 = e2.getAttributes();
            if (aList1.size() != aList2.size()) {
                if(LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Number of attributes differ \"" + aList1 + "\"!=\"" + aList2 + "\" for element "
                        + e1.getName());
                }
                return false;
            }
            HashSet<String> orig = new HashSet<String>(aList1.size());
            for (Attribute attr : aList1) {
                orig.add(attr.toString());
            }
            for (Attribute attr : aList2) {
                orig.remove(attr.toString());
            }
            if (!orig.isEmpty() && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Attributes differ \"" + aList1 + "\"!=\"" + aList2 + "\"");
            }
            return orig.isEmpty();
        }

        public static boolean equivalentContent(List<Content> l1, List<Content> l2) {
            if (l1.size() != l2.size()) {
                if(LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Number of content list elements differ " + l1.size() + "!=" + l2.size());
                }
                return false;
            }
            boolean result = true;
            Iterator<Content> i1 = l1.iterator();
            Iterator<Content> i2 = l2.iterator();
            while (result && i1.hasNext() && i2.hasNext()) {
                Object o1 = i1.next();
                Object o2 = i2.next();
                if (o1 instanceof Element && o2 instanceof Element) {
                    result = equivalent((Element) o1, (Element) o2);
                } else if (o1 instanceof Text && o2 instanceof Text) {
                    result = equivalent((Text) o1, (Text) o2);
                } else if (o1 instanceof Comment && o2 instanceof Comment) {
                    result = equivalent((Comment) o1, (Comment) o2);
                } else if (o1 instanceof ProcessingInstruction && o2 instanceof ProcessingInstruction) {
                    result = equivalent((ProcessingInstruction) o1, (ProcessingInstruction) o2);
                } else if (o1 instanceof DocType && o2 instanceof DocType) {
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

            return ns1.equals(ns2) && localName1.equals(localName2);
        }
    }
}
