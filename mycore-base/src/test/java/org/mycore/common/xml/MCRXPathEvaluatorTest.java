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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXPathEvaluatorTest extends MCRTestCase {

    private MCRXPathEvaluator evaluator;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        String builder = "document[name/@id='n1'][note/@href='#n1'][location/@href='#n1'][name[@id='n2']][location[@href='#n2']]";
        Element document = new MCRNodeBuilder().buildElement(builder, null, null);
        new Document(document);
        Map<String, Object> variables = new HashMap<>();
        variables.put("CurrentUser", "gast");
        evaluator = new MCRXPathEvaluator(variables, document);
    }

    @Test
    public void testEvaluator() throws JaxenException, JDOMException {
        assertEquals("n1", evaluator.replaceXPathOrI18n("name[1]/@id"));
        assertEquals("n1", evaluator.replaceXPathOrI18n("/document/name[1]/@id"));
    }
}
