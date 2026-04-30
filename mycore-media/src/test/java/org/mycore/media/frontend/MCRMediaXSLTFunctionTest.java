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

package org.mycore.media.frontend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mycore.common.util.MCRTestCaseXSLTUtil.prepareTestDocument;
import static org.mycore.common.util.MCRTestCaseXSLTUtil.transform;

import java.util.Map;

import org.jdom2.Element;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.URIResolver.ModuleResolver.mediasources", classNameOf = MCRMockMediaSourcesURIResolver.class)
})
public class MCRMediaXSLTFunctionTest {

    private static final String XSL = "/xslt/functions/mediaTest.xsl";

    @Test
    void testGetSources() throws Exception {
        Element result = transform(prepareTestDocument("get-sources"), XSL,
            Map.of("derivateId", "mir_derivate_00000001", "path", "media/main.mp4", "userAgent", "JUnit Browser"))
            .getRootElement();

        assertEquals(2, result.getChildren("source").size());
        assertEquals("https://example.org/mock.mpd", result.getChildren("source").getFirst().getAttributeValue("src"));
        assertEquals("application/dash+xml", result.getChildren("source").getFirst().getAttributeValue("type"));
        assertEquals("https://example.org/mock.mp4", result.getChildren("source").get(1).getAttributeValue("src"));
        assertEquals("video/mp4", result.getChildren("source").get(1).getAttributeValue("type"));
    }
}
