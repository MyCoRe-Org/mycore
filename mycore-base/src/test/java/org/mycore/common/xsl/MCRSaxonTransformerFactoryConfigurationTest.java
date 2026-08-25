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

package org.mycore.common.xsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import javax.xml.transform.TransformerFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mycore.common.config.MCRConfigurationDir;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRSaxonTransformerFactoryConfigurationTest {

    private static final String ALLOW_EXTERNAL_FUNCTIONS_ATTRIBUTE =
        "http://saxon.sf.net/feature/allow-external-functions";

    @Test
    public void appliesConfigurationFileRelativeToConfigurationDirectory() throws IOException {
        Path relativeConfigurationFile = Path.of("test", getClass().getSimpleName(), "saxon-config.xml");
        Path configurationFile = Objects.requireNonNull(MCRConfigurationDir.getConfigurationDirectory()).toPath()
            .resolve(relativeConfigurationFile);
        Files.createDirectories(configurationFile.getParent());
        writeSaxonConfiguration(configurationFile);
        MCRSaxonTransformerFactoryConfiguration configuration = new MCRSaxonTransformerFactoryConfiguration();
        configuration.setFile(relativeConfigurationFile.toString());
        TransformerFactory saxonFactory = new net.sf.saxon.TransformerFactoryImpl();

        configuration.accept(saxonFactory);

        assertEquals(Boolean.FALSE, saxonFactory.getAttribute(ALLOW_EXTERNAL_FUNCTIONS_ATTRIBUTE));
    }

    @Test
    public void rejectsMissingConfigurationFile(@TempDir Path temporaryDirectory) {
        MCRSaxonTransformerFactoryConfiguration configuration = new MCRSaxonTransformerFactoryConfiguration();
        configuration.setFile(temporaryDirectory.resolve("missing.xml").toString());
        TransformerFactory saxonFactory = new net.sf.saxon.TransformerFactoryImpl();

        assertThrows(MCRConfigurationException.class, () -> configuration.accept(saxonFactory));
    }

    @Test
    public void reportsUnsupportedFactoryWithoutClaimingThatTheFileCouldNotBeLoaded(@TempDir Path temporaryDirectory)
        throws IOException {
        Path configurationFile = writeSaxonConfiguration(temporaryDirectory.resolve("saxon-config.xml"));
        MCRSaxonTransformerFactoryConfiguration configuration = new MCRSaxonTransformerFactoryConfiguration();
        configuration.setFile(configurationFile.toString());
        TransformerFactory xalanFactory = new MCRXalanTransformerFactory();

        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> configuration.accept(xalanFactory));

        assertTrue(exception.getMessage().contains("Could not apply Saxon configuration file"));
        assertTrue(exception.getMessage().contains(xalanFactory.getClass().getName()));
    }

    private Path writeSaxonConfiguration(Path configurationFile) throws IOException {
        return Files.writeString(configurationFile, """
            <?xml version="1.0" encoding="UTF-8"?>
            <configuration xmlns="http://saxon.sf.net/ns/configuration" edition="HE">
              <global allowExternalFunctions="false"/>
            </configuration>
            """);
    }

}
