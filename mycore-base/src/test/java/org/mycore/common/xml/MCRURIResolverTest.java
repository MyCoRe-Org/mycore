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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.transform.Source;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfigurationDir;
import org.mycore.resource.MCRResourceHelper;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRURIResolverTest {

    @Test
    public void testGetParentDirectoryResourceURI() throws IOException {

        // obtain resource URLs
        URL myFileResourceUrl = MCRResourceHelper.getResourceUrl("/xsl/myfile.xsl");
        URL nestedMyFileResourceUrl = MCRResourceHelper.getResourceUrl("/xsl/directory/myfile.xsl");

        // check resource URLs
        checkParentDirectoryResourceUri(myFileResourceUrl.toString(), "resource:xsl/");
        checkParentDirectoryResourceUri(nestedMyFileResourceUrl.toString(), "resource:xsl/directory/");

        // obtain test directories (${configDir}/resources/xsl[/directory])
        Path configurationXslDirectory = getConfigurationXslDirectory();
        Path nestedConfigurationXslDirectory = configurationXslDirectory.resolve("directory");
        Files.createDirectories(nestedConfigurationXslDirectory);
        boolean dirsCreated = Files.exists(nestedConfigurationXslDirectory);
        assert dirsCreated;

        // obtain files in test directories
        Path myFileFile = configurationXslDirectory.resolve("myfile.xsl");
        Path nestedMyFileFile = nestedConfigurationXslDirectory.resolve("myfile.xsl");

        // create actual files in test directories

        try (InputStream is = myFileResourceUrl.openStream();
            InputStream nestedIs = nestedMyFileResourceUrl.openStream()) {
            long bytesCopied = Files.copy(is, myFileFile);
            long nestedBytesCopied = Files.copy(nestedIs, nestedMyFileFile);
            assert bytesCopied != 0 && nestedBytesCopied != 0;
        }

        // check file URLs
        checkParentDirectoryResourceUri(myFileFile.toUri().toString(), "resource:xsl/");
        checkParentDirectoryResourceUri(nestedMyFileFile.toUri().toString(), "resource:xsl/directory/");
    }

    private void checkParentDirectoryResourceUri(String uri, String expectedParentDirectoryResourceUri) {
        String actualParentDirectoryResourceURI = MCRURIResolver.getParentDirectoryResourceURI(uri);
        assertEquals(expectedParentDirectoryResourceUri, actualParentDirectoryResourceURI);
    }

    private static Path getConfigurationXslDirectory() {
        return MCRConfigurationDir.getConfigurationDirectory().toPath()
            .resolve("resources")
            .resolve("xsl");
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(
            key = "MCR.URIResolver.xslImports.xsl-import", string = "functions/xsl-1.xsl,functions/xsl-2.xsl")
    })
    public void testImportFromSameDirectory() throws Exception {

        String xslResourceUrl = MCRResourceHelper.getResourceUrl("/xsl/functions/xsl-2.xsl").toString();
        Source xslSource = MCRURIResolver.obtainInstance()
            .resolve("xslImport:xsl-import:functions/xsl-2.xsl", xslResourceUrl);
        assertNotNull(xslSource);
        assertTrue(StringUtils.endsWith(xslSource.getSystemId(), "/xsl/functions/xsl-1.xsl"));

        String xsltResourceUrl = MCRResourceHelper.getResourceUrl("/xslt/functions/xsl-2.xsl").toString();
        Source xsltSource = MCRURIResolver.obtainInstance()
            .resolve("xslImport:xsl-import:functions/xsl-2.xsl", xsltResourceUrl);
        assertNotNull(xsltSource);
        assertTrue(StringUtils.endsWith(xsltSource.getSystemId(), "/xslt/functions/xsl-1.xsl"));

    }

}
