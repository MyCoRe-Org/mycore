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
    @MCRTestProperty(key = "MCR.Layout.Transformer.Factory.XSLFolder", string = "xsl-test"),
    @MCRTestProperty(key = "MCR.LayoutService.TransformerFactoryClass", string = "%XALAN%")
})
public class MCRURIResolverXalanTest extends MCRTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MCRURIResolver.reInit();
    }

    @Test
    public void testXalanAvailability() {

        Element resolved = MCRURIResolver.instance().resolve("xslStyle:reflection:buildxml:_rootName_=test");
        assert resolved != null;

        String id = resolved.getAttributeValue("id");
        String version = resolved.getAttributeValue("version");
        String vendor = resolved.getAttributeValue("vendor");
        String vendorUrl = resolved.getAttributeValue("vendor-url");

        Assert.assertEquals("xsl-test", id);
        Assert.assertEquals("1.0", version);
        Assert.assertEquals("Apache Software Foundation", vendor);
        Assert.assertEquals("http://xml.apache.org/xalan-j", vendorUrl);

    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        MCRURIResolver.reInit();
    }

}