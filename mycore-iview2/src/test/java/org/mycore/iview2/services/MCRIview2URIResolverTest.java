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

package org.mycore.iview2.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mycore.common.util.MCRTestCaseXSLTUtil.prepareTestDocument;
import static org.mycore.common.util.MCRTestCaseXSLTUtil.transform;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import javax.xml.transform.TransformerException;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.iview2.backend.MCRTileFileProvider;
import org.mycore.iview2.backend.MCRTileInfo;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.IIIFImage.Iview.TileFileProvider.Class",
        classNameOf = MCRIview2URIResolverTest.TestTileFileProvider.class)
})
class MCRIview2URIResolverTest {

    private static final String DERIVATE = "test_derivate_00000001";

    private static final String VIDEO = "/folder/Grüße + 100% #1: clip.mp4";

    @TempDir
    Path directory;

    @Test
    void xsltChecksExistingAndMissingTilesWithEncodedPaths() throws Exception {
        TestTileFileProvider.tileFile = directory.resolve("frame.iview2");
        assertEquals("false", hasTiles(VIDEO));
        Files.createFile(TestTileFileProvider.tileFile);
        assertEquals("true", hasTiles(VIDEO));
        assertEquals("false", hasTiles("/another.mp4"));
        Files.delete(TestTileFileProvider.tileFile);
        Files.createDirectory(TestTileFileProvider.tileFile);
        assertEquals("false", hasTiles(VIDEO));
    }

    @Test
    void rejectsMalformedRequests() {
        MCRIview2URIResolver resolver = new MCRIview2URIResolver();
        for (String uri : new String[] { "iview2:hasTiles", "iview2:hasTiles:" + DERIVATE,
            "iview2:hasTiles:" + DERIVATE + "/", "iview2:hasTiles:/video.mp4",
            "iview2:hasTiles:" + DERIVATE + "/bad%path", "iview2:unknown:value" }) {
            assertThrows(TransformerException.class, () -> resolver.resolve(uri, null), uri);
        }
    }

    @Test
    void preservesFileSupportQueries() throws Exception {
        JDOMSource result = (JDOMSource) new MCRIview2URIResolver().resolve("iview2:isFileSupported:image.png", null);
        assertEquals("true", ((Element) result.getNodes().getFirst()).getName());
    }

    private String hasTiles(String path) throws Exception {
        return transform(prepareTestDocument("has-tiles"), "/xslt/functions/iview2Test.xsl",
            Map.of("derivateId", DERIVATE, "path", path)).getRootElement().getText();
    }

    public static class TestTileFileProvider implements MCRTileFileProvider {

        static Path tileFile;

        @Override
        public Optional<Path> getTileFile(MCRTileInfo tileInfo) {
            assertEquals(DERIVATE, tileInfo.derivate());
            return VIDEO.equals(tileInfo.imagePath()) ? Optional.of(tileFile) : Optional.empty();
        }
    }
}
