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

import org.jdom2.Element;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;

@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Layout.Transformer.Factory.XSLFolder", string = "xslt-test"),
    @MCRTestProperty(key = "MCR.LayoutService.TransformerFactoryClass", string = "%SAXON%")
})
public class MCRURIResolverSaxonTest extends MCRTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
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

        Assert.assertEquals("xslt-test", id);
        Assert.assertEquals("3.0", version);
        Assert.assertEquals("Saxonica", vendor);
        Assert.assertEquals("http://www.saxonica.com/", vendorUrl);

    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        MCRURIResolver.obtainInstance().reinitialize();
    }

}
