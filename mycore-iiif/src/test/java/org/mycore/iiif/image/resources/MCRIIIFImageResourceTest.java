/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
package org.mycore.iiif.image.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.iiif.image.impl.MCRIIIFImageImpl;
import org.mycore.iiif.image.model.MCRIIIFImageInformation;
import org.mycore.iiif.image.model.MCRIIIFImageProfile;
import org.mycore.iiif.image.model.MCRIIIFImageQuality;
import org.mycore.iiif.image.model.MCRIIIFImageSourceRegion;
import org.mycore.iiif.image.model.MCRIIIFImageTargetRotation;
import org.mycore.iiif.image.model.MCRIIIFImageTargetSize;
import org.mycore.iiif.model.MCRIIIFBase;
import org.mycore.test.MyCoReTest;

import jakarta.ws.rs.core.Response;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.baseurl", string = "http://localhost/"),
    @MCRTestProperty(key = "MCR.IIIFImage.Default", string = MCRIIIFImageResourceTest.IMPL),
    @MCRTestProperty(key = "MCR.IIIFImage." + MCRIIIFImageResourceTest.IMPL,
        classNameOf = MCRIIIFImageResourceTest.TestImageImpl.class)
})
public class MCRIIIFImageResourceTest {

    static final String IMPL = "ResourceTest";

    @Test
    public void testImageMediaType() {
        assertEquals("image/jpeg", getImageMediaType("jpg"));
        assertEquals("image/jpeg", getImageMediaType("jpeg"));
        assertEquals("image/tiff", getImageMediaType("tif"));
        assertEquals("image/tiff", getImageMediaType("tiff"));
        assertEquals("image/png", getImageMediaType("png"));
        assertEquals("image/gif", getImageMediaType("gif"));
        assertEquals("image/bmp", getImageMediaType("bmp"));
        assertEquals("image/vnd.wap.wbmp", getImageMediaType("wbmp", "bitonal"));
    }

    @Test
    public void testSpecificationMediaTypes() {
        assertEquals("image/jp2", MCRIIIFImageResource.getMediaType("jp2"));
        assertEquals("application/pdf", MCRIIIFImageResource.getMediaType("pdf"));
        assertEquals("image/webp", MCRIIIFImageResource.getMediaType("webp"));
    }

    @Test
    public void testFormatWithoutImageWriter() {
        // no writer available: must not return an empty image with status 200
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), getImageStatus("pdf", "default"));
        // the JDK WBMP writer supports bitonal images only
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), getImageStatus("wbmp", "default"));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), getImageStatus("wbmp", "gray"));
    }

    private static String getImageMediaType(String format) {
        return getImageMediaType(format, "default");
    }

    private static String getImageMediaType(String format, String quality) {
        try (Response response = getImage(format, quality)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            return response.getMediaType().toString();
        }
    }

    private static int getImageStatus(String format, String quality) {
        try (Response response = getImage(format, quality)) {
            return response.getStatus();
        }
    }

    private static Response getImage(String format, String quality) {
        return new MCRIIIFImageResource().getImage(IMPL, "image", "full", "full", "0", quality, format);
    }

    public static class TestImageImpl extends MCRIIIFImageImpl {

        public TestImageImpl(String implName) {
            super(implName);
        }

        @Override
        public BufferedImage provide(String identifier, MCRIIIFImageSourceRegion region,
            MCRIIIFImageTargetSize targetSize, MCRIIIFImageTargetRotation rotation,
            MCRIIIFImageQuality imageQuality, String format) {
            int imageType = switch (imageQuality) {
                case BITONAL -> BufferedImage.TYPE_BYTE_BINARY;
                case GRAY -> BufferedImage.TYPE_BYTE_GRAY;
                default -> BufferedImage.TYPE_3BYTE_BGR;
            };
            return new BufferedImage(targetSize.width(), targetSize.height(), imageType);
        }

        @Override
        public MCRIIIFImageInformation getInformation(String identifier) {
            return new MCRIIIFImageInformation(MCRIIIFBase.API_IMAGE_2, identifier, "http://iiif.io/api/image", 8,
                8, 0);
        }

        @Override
        public MCRIIIFImageProfile getProfile() {
            return new MCRIIIFImageProfile();
        }

    }

}
