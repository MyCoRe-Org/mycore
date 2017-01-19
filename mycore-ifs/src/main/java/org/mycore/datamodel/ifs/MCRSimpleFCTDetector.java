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

package org.mycore.datamodel.ifs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfigurationException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A simple implementation of an MCRFileContentTypeDetector, detects the file
 * type based on the filename extension and a magic bytes pattern at some offset
 * in the header of the file's content. The rules for detecting each file type
 * are embedded in the &lt;rules&gt; element of the file content types
 * definition XML file.
 * 
 * @see MCRFileContentTypeDetector
 * @see MCRFileContentType
 * @see MCRFileContentTypeFactory
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRSimpleFCTDetector implements MCRFileContentTypeDetector {
    /** List of file content types we have rules for */
    private List typesList = new Vector();

    /** Keys are file content types, values are vectors of MCRDetectionRule */
    private Hashtable rulesTable = new Hashtable();

    private static Logger logger = LogManager.getLogger(MCRSimpleFCTDetector.class);

    /** Creates a new detector */
    public MCRSimpleFCTDetector() {
    }

    /**
     * Adds a detection rule from the file content type definition XML file. The
     * detector parses the &lt;rules&gt; element provided with each content type
     * in the file content types XML definition.
     * 
     * @param type
     *            the file content type the rule is for
     * @param xRules
     *            the rules XML element containing the rules for detecting that
     *            type
     */
    public void addRule(MCRFileContentType type, Element xRules) {
        Vector rules = new Vector();
        rulesTable.put(type, rules);
        typesList.add(type);

        try {
            List extensions = xRules.getChildren("extension");

            for (Object extension : extensions) {
                Element elem = (Element) extension;

                double score = elem.getAttribute("score").getDoubleValue();
                String ext = elem.getTextTrim();

                rules.addElement(new MCRExtensionRule(ext, score));
            }

            List patterns = xRules.getChildren("pattern");

            for (Object pattern1 : patterns) {
                Element elem = (Element) pattern1;

                double score = elem.getAttribute("score").getDoubleValue();
                int offset = elem.getAttribute("offset").getIntValue();
                String format = elem.getAttributeValue("format");
                String pattern = elem.getTextTrim();

                rules.addElement(new MCRPatternRule(pattern, format, offset, score));
            }

            List doctypes = xRules.getChildren("doctype");

            for (Object doctype1 : doctypes) {
                Element elem = (Element) doctype1;

                double score = elem.getAttribute("score").getDoubleValue();
                String doctype = elem.getTextTrim();

                rules.addElement(new MCRDoctypeRule(doctype, score));
            }

            List strings = xRules.getChildren("string");

            for (Object string1 : strings) {
                Element elem = (Element) string1;

                double score = elem.getAttribute("score").getDoubleValue();
                String string = elem.getTextTrim();

                rules.addElement(new MCRStringRule(string, score));
            }
        } catch (Exception exc) {
            String msg = "Error parsing detection rules for file content type " + type.getLabel();
            throw new MCRConfigurationException(msg, exc);
        }
    }

    public MCRFileContentType detectType(String filename, byte[] header) {
        double maxScore = 0.0;
        MCRFileContentType detected = null;

        for (int i = 0; i < typesList.size() && maxScore < 1.0; i++) {
            MCRFileContentType type = (MCRFileContentType) typesList.get(i);
            Vector rules = (Vector) rulesTable.get(type);

            double score = 0.0;

            for (int j = 0; j < rules.size() && score < 1.0; j++) {
                MCRDetectionRule rule = (MCRDetectionRule) rules.elementAt(j);
                score += rule.getScore(filename, header);
                score = Math.min(1.0, score);
            }

            if (score > maxScore) {
                maxScore = score;
                detected = type;
            }
        }

        return detected;
    }

    /** Common superclass of different kinds of detection rules */
    abstract class MCRDetectionRule {
        /** The score for matching this rule, a value between 0.0 and 1.0 */
        protected double score;

        /**
         * Creates a new detection rule
         * 
         * @param score
         *            The score for matching this rule, a value between 0.0 and
         *            1.0
         */
        protected MCRDetectionRule(double score) {
            this.score = Math.min(score, 1.0);
            this.score = Math.max(score, 0.0);
        }

        /**
         * Returns the score if filename and/or header matches this rule, or 0.0
         * 
         * @param filename
         *            the name of the file to detect the content type of
         * @param header
         *            the first bytes of the file content
         * 
         * @return the score between 0.0 and 1.0 for matching this rule
         */
        abstract double getScore(String filename, byte[] header);
    }

    /** A rule that decides based on the file extension */
    class MCRExtensionRule extends MCRDetectionRule {
        /** The lowercase file name extension that a file must match */
        protected String extension;

        /**
         * Creates a new rule based on a match of the file extension
         * 
         * @param extension
         *            lowercase file name extension that a file must match
         * @param score
         *            the score for matching this rule, a value between 0.0 and
         *            1.0
         */
        MCRExtensionRule(String extension, double score) {
            super(score);
            this.extension = extension.toLowerCase(Locale.ROOT);
        }

        @Override
        double getScore(String filename, byte[] header) {
            if (filename.toLowerCase(Locale.ROOT).endsWith(extension)) {
                return score;
            }
            return 0.0;
        }
    }

    /**
     * A rule that decides based on a magic bytes pattern that has to occur in
     * the file header at a given offset
     */
    class MCRPatternRule extends MCRDetectionRule {
        /** The byte pattern (magic bytes) */
        protected byte[] pattern;

        /** The offset where the magic bytes are located in the file header */
        protected int offset;

        /**
         * Creates a new rule for a match based on a magic bytes pattern at a
         * given offset
         * 
         * @param pattern
         *            the magic bytes pattern this rule matches
         * @param format
         *            the format in which the pattern is given, text | hex |
         *            bytes
         * @param offset
         *            the position where the pattern occurs in the file header
         * @param score
         *            the score for matching this rule, a value between 0.0 and
         *            1.0
         */
        MCRPatternRule(String pattern, String format, int offset, double score) {
            super(score);

            if (format.equals("text")) {
                this.pattern = pattern.getBytes(StandardCharsets.ISO_8859_1);
            } else if (format.equals("hex")) {
                this.pattern = new byte[pattern.length() / 2];

                for (int i = 0; i < pattern.length(); i += 2) {
                    String hex = pattern.substring(i, i + 2);
                    this.pattern[i / 2] = (byte) Integer.parseInt(hex, 16);
                }
            } else if (format.equals("bytes")) {
                StringTokenizer st = new StringTokenizer(pattern, " ,:;\t");
                this.pattern = new byte[st.countTokens()];

                for (int i = 0; st.hasMoreTokens(); i++) {
                    this.pattern[i] = (byte) Integer.parseInt(st.nextToken(), 10);
                }
            } else {
                String msg = "Unsupported pattern format in content type detection rule: " + format;
                throw new MCRConfigurationException(msg);
            }

            this.offset = offset;
        }

        @Override
        double getScore(String filename, byte[] header) {
            boolean matches = header.length >= pattern.length + offset;

            for (int i = 0; matches && i < pattern.length; i++) {
                matches = header[offset + i] == pattern[i];
            }

            return matches ? score : 0;
        }
    }

    /** A rule that decides based on the doctype of a xml file */
    class MCRDoctypeRule extends MCRDetectionRule {
        /** The doctype of the file */
        protected String doctype;

        /**
         * Creates a new rule based on a match of the doctype of a xml file
         * 
         * @param doctype
         *            the doctype the file must match
         * @param score
         *            the score for matching this rule, a value between 0.0 and
         *            1.0
         */
        MCRDoctypeRule(String doctype, double score) {
            super(score);
            this.doctype = doctype;
        }

        @Override
        double getScore(String filename, byte[] header) {
            try {
                String type = MCRUtils.parseDocumentType(new ByteArrayInputStream(header));

                if (type.equals(doctype)) {
                    return score;
                }
                return 0;
            } catch (Exception exc) {
                return 0;
            }
        }
    }

    /** A rule that decides based on a String at any position in the head of the file */
    class MCRStringRule extends MCRDetectionRule {
        protected String string;

        /**
         * Creates a new rule based on a match of a String at any position in the head of the file
         * 
         * @param string
         *            the string in the head of the file must match
         * @param score
         *            the score for matching this rule, a value between 0.0 and
         *            1.0
         */
        MCRStringRule(String string, double score) {
            super(score);
            this.string = string;
        }

        @Override
        double getScore(String filename, byte[] header) {
            String head = new String(header, StandardCharsets.ISO_8859_1);
            if (head.contains(string)) {
                return score;
            }
            return 0;
        }
    }

    /**
     * Copy from MCRLayoutServlet, messages changed from MCRLayoutServlet to
     * MCRSimpleFCTDetector Try to detect doctype of xml data
     * 
     * @param in
     *            xml data
     * 
     * @return detected doctype
     */
    protected String parseDocumentType(InputStream in) {
        SAXParser parser = null;

        try {
            parser = SAXParserFactory.newInstance().newSAXParser();
        } catch (Exception ex) {
            String msg = "Could not build a SAX Parser for processing XML input";
            throw new MCRConfigurationException(msg, ex);
        }

        final Properties detected = new Properties();
        final String forcedInterrupt = "mcr.forced.interrupt";

        DefaultHandler handler = new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                logger.debug("MCRSimpleFCTDetector detected root element = " + qName);
                detected.setProperty("docType", qName);
                throw new MCRException(forcedInterrupt);
            }
        };

        try {
            parser.parse(new InputSource(in), handler);
        } catch (Exception ex) {
            if (!forcedInterrupt.equals(ex.getMessage())) {
                String msg = "Error while detecting XML document type from input source";
                throw new MCRException(msg, ex);
            }
        }

        return detected.getProperty("docType");
    }
}
