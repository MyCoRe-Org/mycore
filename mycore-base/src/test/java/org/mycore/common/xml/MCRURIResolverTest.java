package org.mycore.common.xml;

import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.transform.Source;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationDir;

public class MCRURIResolverTest extends MCRTestCase {

    public static final String TEST_DEVELOPER_FOLDER_STRING = "/home/workspace";

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.Developer.Resource.Override", string = TEST_DEVELOPER_FOLDER_STRING)
    })
    public void testGetParentDirectoryResourceURI() {
        Map<String, String> testData = new LinkedHashMap<>();

        testData.put("jar:file:///root/.m2/repository/some/directory/some.jar!/xsl/directory/myfile.xsl",
            "resource:xsl/directory/");
        testData.put("jar:file:///root/.m2/repository/some/directory/some.jar!/xsl/myfile.xsl",
            "resource:xsl/");

        String configurationXSLDirectory
            = MCRConfigurationDir.getConfigurationDirectory().toPath()
                .resolve("resources")
                .resolve("xsl")
                .toFile()
                .toURI()
                .toString();
        testData.put(configurationXSLDirectory + "/mir-accesskey-utils.xsl", "resource:xsl/");

        testData.put(configurationXSLDirectory + "/directory/mir-accesskey-utils.xsl",
            "resource:xsl/directory/");

        testData.put(Paths.get(TEST_DEVELOPER_FOLDER_STRING).toAbsolutePath().toFile().toURI()
            + "/directory/mir-accesskey-utils.xsl", "resource:directory/");

        for (Map.Entry<String, String> entry : testData.entrySet()) {
            String result = MCRURIResolver.getParentDirectoryResourceURI(entry.getKey(), false);
            Assert.assertEquals(entry.getValue(), result);
        }
    }

    @Test
    public void testImportFromSameDirectory() throws Exception {
        MCRConfiguration2.set("MCR.URIResolver.xslImports.xsl-import", "functions/xsl-1.xsl,functions/xsl-2.xsl");

        Source resolved = MCRURIResolver.instance()
            .resolve("xslImport:xsl-import:functions/xsl-2.xsl", "some.jar!/xsl/xsl/functions/xsl-2.xsl");
        Assert.assertNotNull(resolved);
        Assert.assertTrue(StringUtils.endsWith(resolved.getSystemId(), "/xsl/functions/xsl-1.xsl"));

        resolved = MCRURIResolver.instance()
            .resolve("xslImport:xsl-import:functions/xsl-2.xsl", "some.jar!/xslt/functions/xsl-2.xsl");
        Assert.assertNotNull(resolved);
        Assert.assertTrue(StringUtils.endsWith(resolved.getSystemId(), "/xslt/functions/xsl-1.xsl"));
    }
}
