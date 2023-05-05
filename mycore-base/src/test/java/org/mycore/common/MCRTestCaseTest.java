package org.mycore.common;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.config.MCRConfiguration2;

@MCRTestConfiguration(properties = {
    //overwrite property of MCRTestCase
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "false"),
    //overwrite property of config/mycore.properties
    @MCRTestProperty(key = "MCR.NameOfProject", string = MCRTestCaseTest.PROJECT_NAME)
})
public final class MCRTestCaseTest extends MCRTestCase {

    final static String PROJECT_NAME = "MyCoRe Test";

    @Test
    public void testConfigAnnotationOverwrite() {
        Assert.assertFalse(MCRConfiguration2.getBoolean("MCR.Metadata.Type.test").get());
    }

    @Test
    public void testConfigPropertiesOverwrite() {
        Assert.assertEquals(PROJECT_NAME, MCRConfiguration2.getStringOrThrow("MCR.NameOfProject"));
    }

    @Test
    public void testConfigProperties() {
        Assert.assertNotNull(MCRConfiguration2.getStringOrThrow("MCR.CommandLineInterface.SystemName"));
    }
}
