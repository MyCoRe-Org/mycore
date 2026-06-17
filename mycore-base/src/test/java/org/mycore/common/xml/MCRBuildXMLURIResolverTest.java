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

package org.mycore.common.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.xml.transform.Source;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRConstants;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRBuildXMLURIResolverTest {

    /**
     * Regression test for MCR-3726: the scheme prefix must be stripped before the query is parsed,
     * otherwise {@code _rootName_} and all parameters are lost.
     */
    @Test
    public void resolveBuildsRootWithNamespacedChildAndAttribute() {
        Source source = new MCRBuildXMLURIResolver().resolve(
            "buildxml:_rootName_=mods:mods&mods:identifier=10.1000/xyz&mods:identifier/@type=doi", null);

        Element root = (Element) ((JDOMSource) source).getNodes().getFirst();

        assertNotNull(root);
        assertEquals("mods", root.getName());
        assertEquals(MCRConstants.MODS_NAMESPACE, root.getNamespace());

        Element identifier = root.getChild("identifier", MCRConstants.MODS_NAMESPACE);
        assertNotNull(identifier);
        assertEquals("10.1000/xyz", identifier.getText());
        assertEquals("doi", identifier.getAttributeValue("type"));
    }

}
