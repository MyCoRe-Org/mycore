package org.mycore.common.xml;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRURIResolverFlavorTest extends MCRTestCase {

    @Test
    public void testSaxonFlavor() {

        Element resolved = MCRURIResolver.instance().resolve("xslStyle:reflection#xslt:buildxml:_rootName_=test");

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
