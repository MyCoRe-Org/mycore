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
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRURIResolverFlavorTest extends MCRTestCase {

    @Test
    public void testSaxonFlavor() {

        Element resolved = MCRURIResolver.instance().resolve("xslStyle:reflection#xslt:buildxml:_rootName_=test");
        assert resolved != null;

        String id = resolved.getAttributeValue("id");
        String version = resolved.getAttributeValue("version");
        String vendor = resolved.getAttributeValue("vendor");
        String vendorUrl = resolved.getAttributeValue("vendor-url");

        Assert.assertEquals("xslt", id);
        Assert.assertEquals("3.0", version);
        Assert.assertEquals("Saxonica", vendor);
        Assert.assertEquals("http://www.saxonica.com/", vendorUrl);

    }

    @Test
    public void testXalanFlavor() {

        Element resolved = MCRURIResolver.instance().resolve("xslStyle:reflection#xsl:buildxml:_rootName_=test");
        assert resolved != null;

        String id = resolved.getAttributeValue("id");
        String version = resolved.getAttributeValue("version");
        String vendor = resolved.getAttributeValue("vendor");
        String vendorUrl = resolved.getAttributeValue("vendor-url");

        Assert.assertEquals("xsl", id);
        Assert.assertEquals("1.0", version);
        Assert.assertEquals("Apache Software Foundation", vendor);
        Assert.assertEquals("http://xml.apache.org/xalan-j", vendorUrl);

    }

}
