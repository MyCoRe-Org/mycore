/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.mods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.jdom2.Element;
import org.jdom2.transform.JDOMResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRConstants;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRMODSExtentURIResolverTest {

    private final MCRMODSExtentURIResolver resolver = new MCRMODSExtentURIResolver();

    @Test
    @DisplayName("modsExtent: start and end page")
    public void testStartEnd() throws Exception {
        Element extent = transform(resolver.resolve("modsExtent:pp. 3-4", null));
        assertEquals("3", extent.getChildText("start", MCRConstants.MODS_NAMESPACE));
        assertEquals("4", extent.getChildText("end", MCRConstants.MODS_NAMESPACE));
    }

    @Test
    @DisplayName("modsExtent: incomplete end page is completed")
    public void testEndPageCompleted() throws Exception {
        Element extent = transform(resolver.resolve("modsExtent:S. 3845 - 53", null));
        assertEquals("3845", extent.getChildText("start", MCRConstants.MODS_NAMESPACE));
        assertEquals("3853", extent.getChildText("end", MCRConstants.MODS_NAMESPACE));
    }

    @Test
    @DisplayName("modsExtent: total pages only")
    public void testTotalOnly() throws Exception {
        Element extent = transform(resolver.resolve("modsExtent:123 pages", null));
        assertEquals("123", extent.getChildText("total", MCRConstants.MODS_NAMESPACE));
    }

    @Test
    @DisplayName("modsExtent: invalid syntax throws exception")
    public void testInvalidSyntax() {
        assertThrows(TransformerException.class, () -> resolver.resolve("modsExtent", null));
    }

    private Element transform(Source source) throws Exception {
        JDOMResult result = new JDOMResult();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(source, result);
        Element root = result.getDocument().getRootElement();
        assertEquals("extent", root.getName());
        assertEquals(MCRConstants.MODS_NAMESPACE, root.getNamespace());
        assertEquals("pages", root.getAttributeValue("unit"));
        return root;
    }
}
