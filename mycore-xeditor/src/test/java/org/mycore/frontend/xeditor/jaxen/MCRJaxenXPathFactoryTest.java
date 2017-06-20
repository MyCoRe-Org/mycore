/*
 * $Revision: 28699 $ 
 * $Date: 2013-12-19 21:45:48 +0100 (Do, 19 Dez 2013) $
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

package org.mycore.frontend.xeditor.jaxen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.xml.MCRNodeBuilder;
import org.mycore.common.xml.MCRXPathEvaluator;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRJaxenXPathFactoryTest extends MCRTestCase {

    private MCRXPathEvaluator evaluator;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        String builder = "document[name/@id='n1'][note/@href='#n1'][location/@href='#n1'][name[@id='n2']][location[@href='#n2']]";
        Element document = new MCRNodeBuilder().buildElement(builder, null, null);
        new Document(document);
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("CurrentUser", "gast");
        evaluator = new MCRXPathEvaluator(variables, document);
    }

    @Test
    public void testGenerateID() throws JaxenException, JDOMException {
        String id = evaluator.replaceXPathOrI18n("xed:generate-id(/document)");
        assertEquals(id, evaluator.replaceXPathOrI18n("xed:generate-id(.)"));
        assertEquals(id, evaluator.replaceXPathOrI18n("xed:generate-id()"));
        assertFalse(id.equals(evaluator.replaceXPathOrI18n("xed:generate-id(/document/name[1])")));

        id = evaluator.replaceXPathOrI18n("xed:generate-id(/document/name[1])");
        assertEquals(id, evaluator.replaceXPathOrI18n("xed:generate-id(/document/name[1])"));
        assertEquals(id, evaluator.replaceXPathOrI18n("xed:generate-id(/document/name)"));
        assertFalse(id.equals(evaluator.replaceXPathOrI18n("xed:generate-id(/document/name[2])")));
    }

    @Test
    public void testJavaCall() throws JaxenException, JDOMException {
        String res = evaluator.replaceXPathOrI18n(
            "xed:call-java('org.mycore.frontend.xeditor.jaxen.MCRJaxenXPathFactoryTest','testNoArgs')");
        assertEquals(testNoArgs(), res);

        res = evaluator.replaceXPathOrI18n(
            "xed:call-java('org.mycore.frontend.xeditor.jaxen.MCRJaxenXPathFactoryTest','testOneArg',name[2])");
        assertEquals("n2", res);

        res = evaluator.replaceXPathOrI18n(
            "xed:call-java('org.mycore.frontend.xeditor.jaxen.MCRJaxenXPathFactoryTest','testTwoArgs',string(name[1]/@id),string(note[1]/@href))");
        assertEquals("true", res);

        res = evaluator.replaceXPathOrI18n(
            "xed:call-java('org.mycore.frontend.xeditor.jaxen.MCRJaxenXPathFactoryTest','testTwoArgs',string(name[2]/@id),string(note[1]/@href))");
        assertEquals("false", res);
    }

    public static String testNoArgs() {
        return "testNoArgs";
    }

    public static String testOneArg(List<Element> nodes) {
        return nodes.get(0).getAttributeValue("id");
    }

    public static boolean testTwoArgs(String id, String href) {
        return id.equals(href.substring(1));
    }

    @Test
    public void testExternalJavaTest() throws JaxenException, JDOMException {
        assertTrue(evaluator.test("xed:call-java('org.mycore.common.xml.MCRXMLFunctions','isCurrentUserGuestUser')"));
        assertFalse(evaluator.test("xed:call-java('org.mycore.common.xml.MCRXMLFunctions','isCurrentUserSuperUser')"));
        assertFalse(
            evaluator.test("xed:call-java('org.mycore.common.xml.MCRXMLFunctions','isCurrentUserInRole','admins')"));
    }
}
