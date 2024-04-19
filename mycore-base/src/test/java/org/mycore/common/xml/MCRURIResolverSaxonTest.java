package org.mycore.common.xml;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;

@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Layout.Transformer.Factory.XSLFolder", string = "xslt-test"),
    @MCRTestProperty(key = "MCR.LayoutService.TransformerFactoryClass", string = "%SAXON%")
})
public class MCRURIResolverSaxonTest extends MCRTestCase {

    @Test
    public void testSaxonAvailability() {

        Element resolved = MCRURIResolver.instance().resolve("xslStyle:reflection:buildxml:_rootName_=test");

        String id = resolved.getAttributeValue("id");
        String version = resolved.getAttributeValue("version");
        String vendor = resolved.getAttributeValue("vendor");
        String vendorUrl = resolved.getAttributeValue("vendor-url");

        Assert.assertEquals("xslt-test", id);
        Assert.assertEquals("3.0", version);
        Assert.assertEquals("Saxonica", vendor);
        Assert.assertEquals("http://www.saxonica.com/", vendorUrl);

    }

}
