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

package org.mycore.parser.bool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.jdom2.Element;
import org.junit.Test;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRBooleanClauseParser;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRFalseCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.parsers.bool.MCRParseException;
import org.mycore.parsers.bool.MCRTrueCondition;

/**
 * This class is a JUnit test case for org.mycore.MCRBooleanClausParser.
 * 
 * @author Jens Kupferschmidt
 */
public class MCRBooleanClauseParserTest {

    MCRBooleanClauseParser<Object> p = new MCRBooleanClauseParser<>();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testSingleStrings() {
        MCROrCondition c01 = new MCROrCondition(new MCRTrueCondition(),
            new MCRFalseCondition(), new MCRTrueCondition());
        System.out.println("Boolean claus test 1 --> " + c01.toString());
        assertEquals("Returned value is not", c01.toString(), p.parse("true or false or true").toString());
        assertEquals("Returned value is not", c01.toString(), p.parse("(true) or (false) or (true)").toString());
        
        MCROrCondition c02 = new MCROrCondition(new MCRTrueCondition(),
            new MCROrCondition(new MCRFalseCondition(), new MCRTrueCondition()));
        assertEquals("Returned value is not", c02.toString(), p.parse("true or (false or true)").toString());
        assertEquals("Returned value is not", c02.toString(), p.parse("(true ) or  ( ((false) or (true)))").toString());

        MCRAndCondition c03 = new MCRAndCondition(new MCRTrueCondition(),
            new MCRFalseCondition(), new MCRTrueCondition());
        System.out.println("Boolean claus test 3 --> " + c03.toString());
        assertEquals("Returned value is not", c03.toString(), p.parse("true and false and true").toString());
        
        MCROrCondition c04 = new MCROrCondition(new MCRTrueCondition(),new MCRAndCondition(
            new MCRFalseCondition(), new MCRTrueCondition()));
        System.out.println("Boolean claus test 4 --> " + c04.toString());
        assertEquals("Returned value is not", c04.toString(), p.parse("true or false and true").toString());
        
        MCRCondition c05 = new MCRTrueCondition();
        System.out.println("Boolean claus test 5 --> " + c05.toString());
        assertEquals("Returned value is not", c05.toString(), p.parse("true").toString());
        assertEquals("Returned value is not", c05.toString(), p.parse("(true)").toString());
        assertEquals("Returned value is not", c05.toString(), p.parse("(true )").toString());
        
        try {
            p.parse("(true").toString();
            fail("Should have thrown MCRParseException but did not!");
        } catch (MCRParseException e) {
            final String msg = "Syntax error: missing bracket in \"(true\"";
            assertEquals(msg, e.getMessage());            
        }
        try {
            p.parse("(true) or false) or (true)").toString();
            fail("Should have thrown MCRParseException but did not!");
        } catch (MCRParseException e) {
            final String msg = "Syntax error: missing bracket in \"@<0>@ or false) or @<1>@\"";
            assertEquals(msg, e.getMessage());            
        }
        try {
            p.parse("true and (false and true").toString();
            fail("Should have thrown MCRParseException but did not!");
        } catch (MCRParseException e) {
            final String msg = "Syntax error: missing bracket in \"true and (false and true\"";
            assertEquals(msg, e.getMessage());            
        }
        try {
            p.parse("((((true or false))))) and true)").toString();
            fail("Should have thrown MCRParseException but did not!");
        } catch (MCRParseException e) {
            final String msg = "Syntax error: missing bracket in \"@<3>@) and true)\"";
            assertEquals(msg, e.getMessage());            
        }
        try {
            p.parse("true or true or (true or true))))").toString();
            fail("Should have thrown MCRParseException but did not!");
        } catch (MCRParseException e) {
            final String msg = "Syntax error: missing bracket in \"true or true or @<0>@)))\"";
            assertEquals(msg, e.getMessage());            
        }
        try {
            p.parse("(true ) or  ((((((((( ((false) or (true))").toString();
            fail("Should have thrown MCRParseException but did not!");
        } catch (MCRParseException e) {
            System.out.println("##"+e.getMessage()+"##");
            final String msg = "Syntax error: missing bracket in \"@<0>@ or  ((((((((( @<3>@\"";
            assertEquals(msg, e.getMessage());            
        }
    }

    @Test
    public void testSingleElements() {
        Element bool01 = new Element("boolean");
        bool01.setAttribute("operator", "true");
        try {
            p.parse(bool01);
        } catch (MCRParseException e) {
            fail("Should not thorwn MCRParseException!");
        }
        Element bool02 = new Element("boolean");
        bool02.setAttribute("operator", "truhe");
        try {
            p.parse(bool02);
            fail("Should have thrown MCRParseException but did not!");
        } catch (MCRParseException e) {
            System.out.println("##"+e.getMessage()+"##");
            final String msg = "Syntax error: <truhe>";
            assertEquals(msg, e.getMessage());            
        }
        Element bool03 = new Element("boolean");
        bool03.setAttribute("operators", "true");
        try {
            p.parse(bool03);
            fail("Should have thrown MCRParseException but did not!");
        } catch (MCRParseException e) {
            System.out.println("##"+e.getMessage()+"##");
            final String msg = "Syntax error: attribute operator not found";
            assertEquals(msg, e.getMessage());            
        }
    }
}
