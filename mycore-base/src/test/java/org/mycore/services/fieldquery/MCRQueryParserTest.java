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

import org.apache.logging.log4j.LogManager;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryParser;

import java.io.StringReader;
import org.jdom2.input.SAXBuilder;


/**
 * @author Christoph Neidahl (OPNA2608)
 */
public class MCRQueryParserTest extends MCRTestCase {
    
    private final MCRQueryParser queryParser = new MCRQueryParser();
    
    /**
     * Test method for
     * {@link org.mycore.services.fieldquery.MCRQueryParser#parse(java.lang.String)} and
     * {@link org.mycore.services.fieldquery.MCRQueryParser#parse(org.jdom2.Element)}
     * .
     */
        
    @Test
    public final void testQueryAsString() {
        
        //String sampleQueryML = "(((title contains \"test\") OR (releaseDate < \"2000\")) AND (not (content contains \"Gericht Amt\"))) AND (content contains \"Finanzen\")";
        //String sampleQueryML = "((title) contains \"Amt\")";
        String sampleQueryML = "((title) contains \"Amt (Test)\")";
        
        /*
         * not sure what's expected here
         * passing a query in xml format breaks with "not a query"
         * commented out for now
        // Element
        MCRCondition<Void> queryElementParsed = queryParser.parse(sampleQueryXMLDoc);
        MCRQuery queryElement = new MCRQuery(queryElementParsed);
        */

        System.out.println("############################################################################");
        // String
        MCRCondition<Void> queryStringParsed = queryParser.parse(sampleQueryML);
        System.out.println(queryStringParsed.toString());
        MCRQuery queryString = new MCRQuery(queryStringParsed);
        System.out.println("############################################################################");
        //assertEquals("test to fail", "successfully run!");
    }
    
}
