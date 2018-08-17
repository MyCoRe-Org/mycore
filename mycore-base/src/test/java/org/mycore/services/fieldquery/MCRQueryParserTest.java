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

package org.mycore.services.fieldquery;

import static org.junit.Assert.assertEquals;

import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRQueryParser;

/**
 * This class is a JUnit test case for org.mycore.MCRBooleanClausParser.
 * 
 * @author Jens Kupferschmidt
 * @author Christoph Neidahl (OPNA2608)
 */
public class MCRQueryParserTest extends MCRTestCase {

    private final MCRQueryParser queryParser = new MCRQueryParser();

    /**
     * Test method for
     * {@link org.mycore.services.fieldquery.MCRQueryParser#parse(org.jdom2.Element) MCRQueryParser.parse} for XML
     */

    @Test
    public final void testQueryAsXML() {
        Element c1 = new Element("condition");
        c1.setAttribute("field", "title");
        c1.setAttribute("operator", "contains");
        c1.setAttribute("value", "Amt und Würde");
        MCRCondition<Void> queryStringParsed = queryParser.parse(c1);
        System.out.println("XML query parser test 1 :"+ queryStringParsed.toString());
        assertEquals("Returned value is not", "(title contains \"Amt\") AND (title contains \"und\") AND (title contains \"Würde\")", queryStringParsed.toString());

        Element c2 = new Element("condition");
        c2.setAttribute("field", "title");
        c2.setAttribute("operator", "contains");
        c2.setAttribute("value", "Amt 'und Würde'");
        queryStringParsed = queryParser.parse(c2);
        System.out.println("XML query parser test 2 :"+ queryStringParsed.toString());
        assertEquals("Returned value is not", "(title contains \"Amt\") AND (title phrase \"und Würde\")", queryStringParsed.toString());

        Element c3 = new Element("condition");
        c3.setAttribute("field", "title");
        c3.setAttribute("operator", "contains");
        c3.setAttribute("value", "Amt und (Würde)");
        queryStringParsed = queryParser.parse(c3);
        System.out.println("XML query parser test 3 :"+ queryStringParsed.toString());
        assertEquals("Returned value is not", "(title contains \"Amt\") AND (title contains \"und\") AND (title contains \"(Würde)\")", queryStringParsed.toString());

        Element bool = new Element("boolean");
        bool.setAttribute("operator", "oR");
        bool.addContent(c1);
        bool.addContent(c2);
        bool.addContent(c3);
        queryStringParsed = queryParser.parse(bool);
        System.out.println("XML query parser test 4 :"+ queryStringParsed.toString());
        assertEquals("Returned value is not", "((title contains \"Amt\") AND (title contains \"und\") AND (title contains \"Würde\")) OR ((title contains \"Amt\") AND (title phrase \"und Würde\")) OR ((title contains \"Amt\") AND (title contains \"und\") AND (title contains \"(Würde)\"))", queryStringParsed.toString());
    }

    /**
     * Test method for
     * {@link org.mycore.services.fieldquery.MCRQueryParser#parse(java.lang.String) MCRQueryParser.parse} for String
     */

    @Test
    public final void testQueryAsString() {

        String query_string = "()";
        MCRCondition<Void> queryStringParsed = queryParser.parse(query_string);
        System.out.println("String query parser test 1 :" + queryStringParsed.toString());
        assertEquals("Returned value is not", "true", queryStringParsed.toString());

        query_string = "((true) or (false))";
        queryStringParsed = queryParser.parse(query_string);
        System.out.println("String query parser test 2 :" + queryStringParsed.toString());
        assertEquals("Returned value is not", "(true) OR (false)", queryStringParsed.toString());

        query_string = " ( (true)) ";
        queryStringParsed = queryParser.parse(query_string);
        System.out.println("String query parser test 3 :" + queryStringParsed.toString());
        assertEquals("Returned value is not", "true", queryStringParsed.toString());

        query_string = "(group = admin ) OR (group = authorgroup ) OR (group = readergroup )";
        queryStringParsed = queryParser.parse(query_string);
        System.out.println("String query parser test 4 :" + queryStringParsed.toString());
        assertEquals("Returned value is not", "(group = \"admin\") OR (group = \"authorgroup\") OR (group = \"readergroup\")", queryStringParsed.toString());

        query_string = "title contains \"Amt\"";
        queryStringParsed = queryParser.parse(query_string);
        System.out.println("String query parser test 5 :" + queryStringParsed.toString());
        assertEquals("Returned value is not", "title contains \"Amt\"", queryStringParsed.toString());

        query_string = "title contains \"Amt und Würde\"";
        queryStringParsed = queryParser.parse(query_string);
        System.out.println("String query parser test 6 :" + queryStringParsed.toString());
        assertEquals("Returned value is not",
            "(title contains \"Amt\") AND (title contains \"und\") AND (title contains \"Würde\")",
            queryStringParsed.toString());

        query_string = "title contains \"Amt \'und Würde\'\"";
        queryStringParsed = queryParser.parse(query_string);
        System.out.println("String query parser test 7 :" + queryStringParsed.toString());
        assertEquals("Returned value is not", "(title contains \"Amt\") AND (title phrase \"und Würde\")",
            queryStringParsed.toString());

        query_string = "title contains \"Amt (und Würde)\"";
        queryStringParsed = queryParser.parse(query_string);
        System.out.println("String query parser test 8 :" + queryStringParsed.toString());
        assertEquals("Returned value is not",
            "(title contains \"Amt\") AND (title contains \"(und\") AND (title contains \"Würde)\")",
            queryStringParsed.toString());
    }

}
