package org.mycore.common.xml;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.xml.transform.Source;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfigurationDir;
import org.mycore.resource.MCRResourceHelper;

public class MCRURIResolverTest extends MCRTestCase {

    @Test
    public void testGetParentDirectoryResourceURI() throws IOException {

        // obtain resource URLs
        URL myFileResourceUrl = MCRResourceHelper.getResourceUrl("/xsl/myfile.xsl");
        URL nestedMyFileResourceUrl = MCRResourceHelper.getResourceUrl("/xsl/directory/myfile.xsl");

        // check resource URLs
        checkParentDirectoryResourceUri(myFileResourceUrl.toString(), "resource:xsl/");
        checkParentDirectoryResourceUri(nestedMyFileResourceUrl.toString(), "resource:xsl/directory/");

        // obtain test directories (${configDir}/resources/xsl[/directory])
        File configurationXslDirectory = getConfigurationXslDirectory();
        File nestedConfigurationXslDirectory = new File(configurationXslDirectory, "directory");
        boolean dirsCreated = nestedConfigurationXslDirectory.mkdirs();
        assert dirsCreated;

        // obtain files in test directories
        File myFileFile = new File(configurationXslDirectory ,"myfile.xsl");
        File nestedMyFileFile = new File(nestedConfigurationXslDirectory, "myfile.xsl");

        // create actual files in test directories
        long bytesCopied = IOUtils.copy(myFileResourceUrl, myFileFile);
        long nestedBytesCopied = IOUtils.copy(nestedMyFileResourceUrl, nestedMyFileFile);
        assert bytesCopied != 0 && nestedBytesCopied != 0;

        // check file URLs
        checkParentDirectoryResourceUri(myFileFile.toURI().toString(), "resource:xsl/");
        checkParentDirectoryResourceUri(nestedMyFileFile.toURI().toString(), "resource:xsl/directory/");

    }

    private void checkParentDirectoryResourceUri(String uri, String expectedParentDirectoryResourceUri) {
        String actualParentDirectoryResourceURI = MCRURIResolver.getParentDirectoryResourceURI(uri);
        Assert.assertEquals(expectedParentDirectoryResourceUri, actualParentDirectoryResourceURI);
    }

    private static File getConfigurationXslDirectory() {
        return MCRConfigurationDir.getConfigurationDirectory().toPath()
            .resolve("resources")
            .resolve("xsl")
            .toFile();
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(
            key = "MCR.URIResolver.xslImports.xsl-import", string = "functions/xsl-1.xsl,functions/xsl-2.xsl")
    })
    public void testImportFromSameDirectory() throws Exception {

        String xslResourceUrl = MCRResourceHelper.getResourceUrl("/xsl/functions/xsl-2.xsl").toString();
        Source xslSource = MCRURIResolver.instance()
            .resolve("xslImport:xsl-import:functions/xsl-2.xsl", xslResourceUrl);
        Assert.assertNotNull(xslSource);
        Assert.assertTrue(StringUtils.endsWith(xslSource.getSystemId(), "/xsl/functions/xsl-1.xsl"));

        String xsltResourceUrl = MCRResourceHelper.getResourceUrl("/xslt/functions/xsl-2.xsl").toString();
        Source xsltSource = MCRURIResolver.instance()
            .resolve("xslImport:xsl-import:functions/xsl-2.xsl", xsltResourceUrl);
        Assert.assertNotNull(xsltSource);
        Assert.assertTrue(StringUtils.endsWith(xsltSource.getSystemId(), "/xslt/functions/xsl-1.xsl"));

    }

}
