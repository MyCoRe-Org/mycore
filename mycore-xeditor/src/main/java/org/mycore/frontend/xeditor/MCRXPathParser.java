/*
 * $Revision$ 
 * $Date$
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

package org.mycore.frontend.xeditor;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Namespace;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXPathParser {

    private final static Logger LOGGER = Logger.getLogger(MCRXPathParser.class);

    public static MCRXPath parse(String xPath) throws ParseException {
        LOGGER.debug("parsing " + xPath);
        return new MCRXPathParser(xPath).parse();
    }

    private int currentParsePosition;

    private String xPathExpression;

    private MCRXPathParser(String xPathExpression) {
        this.xPathExpression = xPathExpression;
    }

    private MCRXPath parse() throws ParseException {
        return new MCRXPath();
    }

    private boolean thereIsMore() {
        return currentParsePosition < xPathExpression.length();
    }

    private void expect(char c) throws ParseException {
        if ((xPathExpression.charAt(currentParsePosition)) == c)
            currentParsePosition++;
        else
            throw new ParseException("Expected: " + c, currentParsePosition);
    }

    private boolean isNextChar(char c) {
        return thereIsMore() && (c == currentChar());
    }

    private char currentChar() {
        return xPathExpression.charAt(currentParsePosition);
    }

    class MCRXPath implements MCRAssignedValue {

        private List<MCRLocationStep> locationSteps = new ArrayList<MCRLocationStep>();

        MCRXPath() throws ParseException {
            locationSteps.add(new MCRLocationStep());
            while (isNextChar('/')) {
                expect('/');
                locationSteps.add(new MCRLocationStep());
            }
        }

        public List<MCRLocationStep> getLocationSteps() {
            return locationSteps;
        }

        public String toString() {
            return buildXPathExpression(locationSteps.size() - 1);
        }

        public String buildXPathExpression(int depth) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i <= depth; i++)
                sb.append(locationSteps.get(i)).append('/');
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }
        
        public String getValue() {
            return toString();
        }
    }

    class MCRLocationStep {

        private boolean isAttribute;

        private String prefix;

        private String name;

        private MCRAssignedValue assignedValue;

        private List<MCRXPath> predicates = new ArrayList<MCRXPath>();

        MCRLocationStep() throws ParseException {
            parseName();
            while (isNextChar('['))
                parsePredicate();
            if (isNextChar('='))
                parseValue();
        }

        public boolean isAttribute() {
            return isAttribute;
        }

        public String getQualifiedName() {
            return (prefix == null ? name : prefix + ":" + name);
        }

        public String getNamespacePrefix() {
            return prefix;
        }

        public Namespace getNamespace() {
            return prefix == null ? null : MCRUsedNamespaces.getNamespace(prefix);
        }

        public String getLocalName() {
            return name;
        }

        public void setValue(MCRLiteral value) {
            this.assignedValue = value;
        }

        public MCRAssignedValue getAssignedValue() {
            return assignedValue;
        }

        public List<MCRXPath> getPredicates() {
            return predicates;
        }

        private void parseName() {
            if (thereIsMore() && (currentChar() == '@')) {
                currentParsePosition++;
                isAttribute = true;
            }

            int begin = currentParsePosition;
            while (thereIsMore() && (Character.isLetterOrDigit(currentChar()) || "@:#$.,*()_-'\"| ".contains(String.valueOf(currentChar()))))
                currentParsePosition++;

            name = xPathExpression.substring(begin, currentParsePosition);
            LOGGER.debug("parsed location step " + name);

            int pos = name.indexOf(":");
            if (pos != -1) {
                prefix = name.substring(0, pos);
                name = name.substring(pos + 1);
            }
        }

        private void parsePredicate() throws ParseException {
            LOGGER.debug("begin parsing predicate...");
            expect('[');
            MCRXPath path = new MCRXPath();
            LOGGER.debug("parsed predicate " + path);
            predicates.add(path);
            expect(']');
        }

        private void parseValue() throws ParseException {
            expect('=');
            char current = currentChar();
            if ((current == '"') || (current == '\'')) {
                assignedValue = new MCRLiteral();
            } else {
                assignedValue = new MCRXPath();
            }
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            if (isAttribute())
                sb.append("@");
            if (prefix != null)
                sb.append(prefix).append(":");
            sb.append(name);
            for (MCRXPath xPath : predicates)
                sb.append('[').append(xPath).append(']');
            if (assignedValue != null)
                sb.append('=').append(assignedValue);
            return sb.toString();
        }
    }

    interface MCRAssignedValue {
        public String getValue();
    }

    class MCRLiteral implements MCRAssignedValue {

        private char delimiter;

        private String value;

        MCRLiteral() throws ParseException {
            delimiter = currentChar();
            expect(delimiter);
            int begin = currentParsePosition;
            do {
                ++currentParsePosition;
            } while (delimiter != currentChar());
            this.value = xPathExpression.substring(begin, currentParsePosition);
            LOGGER.debug("value = " + value);
            expect(delimiter);
        }

        public String getValue() {
            return value;
        }

        public char getDelimiter() {
            return delimiter;
        }

        public String toString() {
            return delimiter + value + delimiter;
        }
    }
}
