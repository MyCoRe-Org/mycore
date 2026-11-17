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

import java.awt.image.BufferedImage;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.iiif.image.impl.MCRIIIFImageImpl;
import org.mycore.iiif.image.model.MCRIIIFImageInformation;
import org.mycore.iiif.image.model.MCRIIIFImageProfile;
import org.mycore.iiif.image.model.MCRIIIFImageQuality;
import org.mycore.iiif.image.model.MCRIIIFImageSourceRegion;
import org.mycore.iiif.image.model.MCRIIIFImageTargetRotation;
import org.mycore.iiif.image.model.MCRIIIFImageTargetSize;
import org.mycore.iiif.model.MCRIIIFBase;

import jakarta.ws.rs.core.Response;

public class MCRIIIFImageResourceTest extends MCRTestCase {

    private static final String IMPL = "ResourceTest";

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.baseurl", "http://localhost/");
        testProperties.put("MCR.IIIFImage.Default", IMPL);
        testProperties.put("MCR.IIIFImage." + IMPL, TestImageImpl.class.getName());
        return testProperties;
    }

    @Test
    public void testImageMediaType() {
        Assert.assertEquals("image/jpeg", getImageMediaType("jpg"));
        Assert.assertEquals("image/jpeg", getImageMediaType("jpeg"));
        Assert.assertEquals("image/tiff", getImageMediaType("tif"));
        Assert.assertEquals("image/tiff", getImageMediaType("tiff"));
        Assert.assertEquals("image/png", getImageMediaType("png"));
        Assert.assertEquals("image/gif", getImageMediaType("gif"));
    }

    private static String getImageMediaType(String format) {
        try (Response response = new MCRIIIFImageResource()
            .getImage(IMPL, "image", "full", "full", "0", "default", format)) {
            Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            return response.getMediaType().toString();
        }
    }

    public static class TestImageImpl extends MCRIIIFImageImpl {

        public TestImageImpl(String implName) {
            super(implName);
        }

        @Override
        public BufferedImage provide(String identifier, MCRIIIFImageSourceRegion region,
            MCRIIIFImageTargetSize targetSize, MCRIIIFImageTargetRotation rotation,
            MCRIIIFImageQuality imageQuality, String format) {
            return new BufferedImage(targetSize.width(), targetSize.height(), BufferedImage.TYPE_3BYTE_BGR);
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
