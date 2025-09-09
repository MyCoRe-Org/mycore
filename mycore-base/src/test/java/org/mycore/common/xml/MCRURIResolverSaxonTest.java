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

import org.jdom2.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Layout.Transformer.Factory.XSLFolder", string = "xslt-test"),
    @MCRTestProperty(key = "MCR.LayoutService.TransformerFactoryClass", string = "%SAXON%")
})
public class MCRURIResolverSaxonTest {

    @BeforeEach
    public void setUp() throws Exception {
        MCRURIResolver.obtainInstance().reinitialize();
    }

    @Test
    public void testSaxonAvailability() {

        Element resolved = MCRURIResolver.obtainInstance().resolve("xslStyle:reflection:buildxml:_rootName_=test");
        assert resolved != null;

        String id = resolved.getAttributeValue("id");
        String version = resolved.getAttributeValue("version");
        String vendor = resolved.getAttributeValue("vendor");
        String vendorUrl = resolved.getAttributeValue("vendor-url");

        assertEquals("xslt-test", id);
        assertEquals("3.0", version);
        assertEquals("Saxonica", vendor);
        assertEquals("http://www.saxonica.com/", vendorUrl);

    }

    @AfterEach
    public void tearDown() throws Exception {
        MCRURIResolver.obtainInstance().reinitialize();
    }

}
