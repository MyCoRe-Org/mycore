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
package org.mycore.iview2.iiif;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.iiif.image.impl.MCRIIIFImageProvidingException;
import org.mycore.iiif.image.model.MCRIIIFImageQuality;
import org.mycore.iiif.image.model.MCRIIIFImageSourceRegion;
import org.mycore.iiif.image.model.MCRIIIFImageTargetRotation;
import org.mycore.iiif.image.model.MCRIIIFImageTargetSize;
import org.mycore.imagetiler.MCRImage;
import org.mycore.iview2.backend.MCRTileFileProvider;
import org.mycore.iview2.backend.MCRTileInfo;
import org.mycore.iview2.services.MCRCountingImageReaderSpi;
import org.mycore.iview2.services.MCRTiledTestImage;

public class MCRIVIEWIIIFImageImplTest extends MCRTestCase {

    private static final String IMPL = "ImplTest";

    private static final String IMAGE_PATH = "image.png";

    private static final int IMAGE_WIDTH = 600;

    private static final int IMAGE_HEIGHT = 500;

    private static Path tileDir;

    private Path tiledFile;

    private MCRCountingImageReaderSpi countingSpi;

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.IIIFImage." + IMPL, MCRIVIEWIIIFImageImpl.class.getName());
        testProperties.put("MCR.IIIFImage." + IMPL + ".TileFileProvider", TestTileFileProvider.class.getName());
        testProperties.put("MCR.IIIFImage." + IMPL + ".TransparentFormats", "png");
        testProperties.put("MCR.IIIFImage." + IMPL + ".MaxImageBytes", "100000000");
        return testProperties;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        tileDir = junitFolder.newFolder().toPath();
        tiledFile = MCRTiledTestImage.create(junitFolder.newFolder().toPath(), tileDir, IMAGE_PATH, IMAGE_WIDTH,
            IMAGE_HEIGHT);
        countingSpi = MCRCountingImageReaderSpi.register();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        countingSpi.deregister();
        super.tearDown();
    }

    @Test
    public void testProvideDisposesImageReaders() throws Exception {
        BufferedImage image = provideFullImage();

        Assert.assertEquals(IMAGE_WIDTH, image.getWidth());
        Assert.assertEquals(IMAGE_HEIGHT, image.getHeight());
        Assert.assertTrue("No tile was read.", countingSpi.getTilesRead() > 1);
        Assert.assertTrue("No ImageReader was created.", countingSpi.getCreated() > 0);
        Assert.assertEquals("Every ImageReader has to be disposed.", countingSpi.getCreated(),
            countingSpi.getDisposed());
    }

    @Test
    public void testProvideDisposesImageReadersOnError() throws Exception {
        MCRTiledTestImage.corruptAllTiles(tiledFile);

        Assert.assertThrows(MCRIIIFImageProvidingException.class, this::provideFullImage);

        Assert.assertTrue("No ImageReader was created.", countingSpi.getCreated() > 0);
        Assert.assertEquals("Every ImageReader has to be disposed.", countingSpi.getCreated(),
            countingSpi.getDisposed());
    }

    private BufferedImage provideFullImage() throws Exception {
        return new MCRIVIEWIIIFImageImpl(IMPL).provide(IMAGE_PATH,
            new MCRIIIFImageSourceRegion(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT),
            new MCRIIIFImageTargetSize(IMAGE_WIDTH, IMAGE_HEIGHT),
            new MCRIIIFImageTargetRotation(false, 0), MCRIIIFImageQuality.COLOR, "jpg");
    }

    public static class TestTileFileProvider implements MCRTileFileProvider {

        @Override
        public Optional<Path> getTileFile(MCRTileInfo tileInfo) {
            return Optional.of(MCRImage.getTiledFile(tileDir, tileInfo.derivate(), tileInfo.imagePath()));
        }

    }

}
