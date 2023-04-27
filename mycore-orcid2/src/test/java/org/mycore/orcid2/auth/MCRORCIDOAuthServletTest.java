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

package org.mycore.orcid2.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.content.MCRContent;
import org.mycore.orcid2.auth.MCRORCIDOAuthServlet.MCRORCIDOAuthResponse;

public class MCRORCIDOAuthServletTest extends MCRTestCase {

    @Test
    public void testMarshalOAuthResponse() throws Exception {
        final MCRContent content = MCRORCIDOAuthServlet.marshalOAuthResponse(new MCRORCIDOAuthResponse("foo", "bar")); 
        final Element root = content.asXML().detachRootElement();
        assertNotNull(root);
        assertEquals("ORCIDOAuthResponse", root.getName());
        final String errorName = "error";
        final Element error = getElement(root, errorName);
        assertNotNull(error);
        assertEquals(errorName, error.getName());
        final String errorDescriptionName = "errorDescription";
        final Element errorDescription = getElement(root, errorDescriptionName);
        assertNotNull(errorDescription);
        assertEquals(errorDescriptionName, errorDescription.getName());
    }

    private Element getElement(Element input, String xPath) throws JDOMException {
        final XPathExpression<Element> expression = XPathFactory.instance().compile(xPath, Filters.element());
        return expression.evaluateFirst(input);
    }
}
