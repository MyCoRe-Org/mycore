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

package org.mycore.common.xml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.streams.MCRByteArrayOutputStream;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * This class provides some static utility methods to deal with XML/DOM
 * elements, nodes etc.
 *
 * @author Detlev Degenhardt
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRXMLHelper {

    private static final Logger LOGGER = LogManager.getLogger(MCRXMLHelper.class);

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
     * @see JDOMtoGSONSerializer
     *
     * @param content the jdom element to serialize
     * @return a gson element
     */
    public static JsonElement jsonSerialize(Content content) {
        return JDOMtoGSONSerializer.serialize(content);
    }

    /**
     * @see JDOMtoGSONSerializer#serializeElement(Element)
     *
     * @param element the jdom element to serialize
     * @return a gson object
     */
    public static JsonObject jsonSerialize(Element element) {
        return JDOMtoGSONSerializer.serializeElement(element);
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
        Document xml = MCRXMLParserFactory.getNonValidatingParser()
            .parseXML(new MCRByteContent(bout.getBuffer(), 0, bout.size()));
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
                LOGGER.debug("Text differs \"{}\"!=\"{}\"", t1, t2);
            }
            return equals;
        }

        public static boolean equivalent(DocType d1, DocType d2) {
            boolean equals = d1.getPublicID().equals(d2.getPublicID()) && d1.getSystemID().equals(d2.getSystemID());
            if (!equals && LOGGER.isDebugEnabled()) {
                LOGGER.debug("DocType differs \"{}\"!=\"{}\"", d1, d2);
            }
            return equals;
        }

        public static boolean equivalent(Comment c1, Comment c2) {
            String v1 = c1.getValue();
            String v2 = c2.getValue();
            boolean equals = v1.equals(v2);
            if (!equals && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Comment differs \"{}\"!=\"{}\"", c1, c2);
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
                LOGGER.debug("ProcessingInstruction differs \"{}\"!=\"{}\"", p1, p2);
            }
            return equals;
        }

        public static boolean equivalentAttributes(Element e1, Element e2) {
            List<Attribute> aList1 = e1.getAttributes();
            List<Attribute> aList2 = e2.getAttributes();
            if (aList1.size() != aList2.size()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Number of attributes differ \"{}\"!=\"{}\" for element {}", aList1, aList2,
                        e1.getName());
                }
                return false;
            }
            HashSet<String> orig = new HashSet<>(aList1.size());
            for (Attribute attr : aList1) {
                orig.add(attr.toString());
            }
            for (Attribute attr : aList2) {
                orig.remove(attr.toString());
            }
            if (!orig.isEmpty() && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Attributes differ \"{}\"!=\"{}\"", aList1, aList2);
            }
            return orig.isEmpty();
        }

        public static boolean equivalentContent(List<Content> l1, List<Content> l2) {
            if (l1.size() != l2.size()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Number of content list elements differ {}!={}", l1.size(), l2.size());
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

    /**
     * Helper class to serialize jdom XML to gson JSON.
     * <p>
     * To support fast javascript dot property access its decided to use the underscore (_)
     * for attributes and the dollar sign ($) for text nodes. The colon sign (:) is used for
     * namespaces (you have to use square brackets in javascript for accessing those).
     * </p>
     *
     * <ul>
     *   <li><b>_version</b> -> version attribute</li>
     *   <li><b>$text</b> -> text node</li>
     *   <li><b>_xmlns:mods</b> -> mods namespace</li>
     *   <li><b>_mods:title</b> -> title attribute with mods namespace</li>
     * </ul>
     *
     * <b>Example</b>
     * <pre>
     * {
     *   "_version": "3.0",
     *   "_xmlns:mods": "http://www.loc.gov/mods/v3"
     *   "mods:titleInfo": {
     *     "mods:title": {
     *       "$text": "hello xml serializer"
     *     }
     *   }
     * }
     * </pre>
     * <ul>
     *   <li><b>get the version</b> -> mods._version -> "3.0"</li>
     *   <li><b>get the text of the title</b> -> mods["mods:titleInfo"]["mods:title"].$text -> "hello xml serializer"</li>
     * </ul>
     * <b>BE AWARE THAT MIXED CONTENT IS NOT SUPPORTED!</b>
     *
     * @author Matthias Eichner
     */
    private static class JDOMtoGSONSerializer {

        /**
         * This method is capable of serializing Elements and Text nodes.
         * Return null otherwise.
         *
         * @param content the content to serialize
         * @return the serialized content, or null if the type is not supported
         */
        public static JsonElement serialize(Content content) {
            if (content instanceof Element) {
                return serializeElement((Element) content);
            }
            if (content instanceof Text) {
                return serializeText((Text) content);
            }
            return null;
        }

        static JsonPrimitive serializeText(Text text) {
            return new JsonPrimitive(text.getText());
        }

        static JsonObject serializeElement(Element element) {
            JsonObject json = new JsonObject();

            // text
            String text = element.getText();
            if (text != null && text.trim().length() > 0) {
                json.addProperty("$text", text);
            }

            // attributes
            element.getAttributes().forEach(attr -> json.addProperty(getName(attr), attr.getValue()));

            // namespaces
            element.getAdditionalNamespaces().forEach(ns -> json.addProperty(getName(ns), ns.getURI()));

            // children
            // - build child map of <name,namespace> pair with their respective elements
            Map<Pair<String, Namespace>, List<Element>> childContentMap = new HashMap<>();
            for (Element child : element.getChildren()) {
                Pair<String, Namespace> key = new Pair<>(child.getName(), child.getNamespace());
                List<Element> contentList = childContentMap.computeIfAbsent(key, k -> new ArrayList<>());
                contentList.add(child);
            }
            // - run through the map and serialize
            for (Map.Entry<Pair<String, Namespace>, List<Element>> entry : childContentMap.entrySet()) {
                Pair<String, Namespace> key = entry.getKey();
                List<Element> contentList = entry.getValue();
                String name = getName(key.x, key.y);
                if (entry.getValue().size() == 1) {
                    json.add(name, serializeElement(contentList.get(0)));
                } else if (contentList.size() >= 2) {
                    JsonArray arr = new JsonArray();
                    contentList.forEach(child -> arr.add(serialize(child)));
                    json.add(name, arr);
                } else {
                    throw new MCRException(
                        "Unexcpected error while parsing children of element '" + element.getName() + "'");
                }
            }
            return json;
        }

        private static String getName(Namespace ns) {
            return "_xmlns:" + getCononicalizedPrefix(ns);
        }

        private static String getName(Attribute attribute) {
            return "_" + getName(attribute.getName(), attribute.getNamespace());
        }

        private static String getName(String name, Namespace namespace) {
            if (namespace != null && !namespace.getURI().equals("")) {
                return getCononicalizedPrefix(namespace) + ":" + name;
            }
            return name;
        }

        private static String getCononicalizedPrefix(Namespace namespace) {
            return MCRConstants
                .getStandardNamespaces()
                .parallelStream()
                .filter(namespace::equals)
                .findAny()
                .map(Namespace::getPrefix)
                .orElse(namespace.getPrefix());
        }

        private static class Pair<X, Y> {
            public final X x;

            public final Y y;

            Pair(X x, Y y) {
                this.x = x;
                this.y = y;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                Pair<?, ?> pair = (Pair<?, ?>) o;
                return Objects.equals(x, pair.x) &&
                    Objects.equals(y, pair.y);
            }

            @Override
            public int hashCode() {
                return Objects.hash(x, y);
            }
        }

    }
}
