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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import javax.xml.transform.TransformerException;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.junit.jupiter.api.Test;
import org.mycore.media.MCRMediaSourceType;
import org.mycore.media.video.MCRMediaSource;

class MCRMediaSourcesURIResolverTest {

    @Test
    void resolveReturnsSourceElements() throws Exception {
        MCRMediaSourcesURIResolver resolver = new MCRMediaSourcesURIResolver((derivateId, path, userAgent) -> {
            assertEquals("mir_derivate_00000001", derivateId);
            assertEquals("media/main.mp4", path);
            assertEquals(Optional.of("JUnit Browser"), userAgent);
            return List.of(new MCRMediaSource("https://example.org/video.mpd", MCRMediaSourceType.DASH_STREAM),
                new MCRMediaSource("https://example.org/video.mp4", MCRMediaSourceType.MP4));
        });

        JDOMSource source = (JDOMSource) resolver.resolve(
            "mediasources:getSources?derivateId=mir_derivate_00000001&path=media%2Fmain.mp4&userAgent=JUnit%20Browser",
            null);
        Element root = (Element) source.getNodes().getFirst();

        assertEquals("sources", root.getName());
        assertEquals(2, root.getChildren("source").size());
        assertEquals("https://example.org/video.mpd", root.getChildren("source").getFirst().getAttributeValue("src"));
        assertEquals("application/dash+xml", root.getChildren("source").getFirst().getAttributeValue("type"));
        assertEquals("https://example.org/video.mp4", root.getChildren("source").get(1).getAttributeValue("src"));
        assertEquals("video/mp4", root.getChildren("source").get(1).getAttributeValue("type"));
    }

    @Test
    void resolveRejectsMissingRequiredArguments() {
        MCRMediaSourcesURIResolver resolver = new MCRMediaSourcesURIResolver((derivateId, path, userAgent) ->
            List.of());

        assertThrows(TransformerException.class, () -> resolver.resolve(
            "mediasources:getSources?derivateId=mir_derivate_00000001", null));
    }

    @Test
    void resolveWrapsProviderErrors() {
        MCRMediaSourcesURIResolver resolver = new MCRMediaSourcesURIResolver((derivateId, path, userAgent) -> {
            throw new URISyntaxException(path, "boom");
        });

        assertThrows(TransformerException.class, () -> resolver.resolve(
            "mediasources:getSources?derivateId=mir_derivate_00000001&path=media%2Fmain.mp4", null));
    }
}
