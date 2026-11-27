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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
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
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.IIIFImage." + MCRIVIEWIIIFImageImplTest.IMPL,
        classNameOf = MCRIVIEWIIIFImageImpl.class),
    @MCRTestProperty(key = "MCR.IIIFImage." + MCRIVIEWIIIFImageImplTest.IMPL + ".TileFileProvider",
        classNameOf = MCRIVIEWIIIFImageImplTest.TestTileFileProvider.class),
    @MCRTestProperty(key = "MCR.IIIFImage." + MCRIVIEWIIIFImageImplTest.IMPL + ".TransparentFormats",
        string = "png"),
    @MCRTestProperty(key = "MCR.IIIFImage." + MCRIVIEWIIIFImageImplTest.IMPL + ".MaxImageBytes",
        string = "100000000")
})
public class MCRIVIEWIIIFImageImplTest {

    static final String IMPL = "ImplTest";

    private static final String IMAGE_PATH = "image.png";

    private static final int IMAGE_WIDTH = 600;

    private static final int IMAGE_HEIGHT = 500;

    private static Path tileDir;

    @TempDir
    Path tileTempDir;

    @TempDir
    Path imageTempDir;

    private Path tiledFile;

    private MCRCountingImageReaderSpi countingSpi;

    @BeforeEach
    public void setUp() throws Exception {
        tileDir = tileTempDir;
        tiledFile = MCRTiledTestImage.create(imageTempDir, tileDir, IMAGE_PATH, IMAGE_WIDTH, IMAGE_HEIGHT);
        countingSpi = MCRCountingImageReaderSpi.register();
    }

    @AfterEach
    public void tearDown() {
        countingSpi.deregister();
    }

    @Test
    public void testProvideDisposesImageReaders() throws Exception {
        BufferedImage image = provideFullImage();

        assertEquals(IMAGE_WIDTH, image.getWidth());
        assertEquals(IMAGE_HEIGHT, image.getHeight());
        assertTrue(countingSpi.getTilesRead() > 1, "No tile was read.");
        assertTrue(countingSpi.getCreated() > 0, "No ImageReader was created.");
        assertEquals(countingSpi.getCreated(), countingSpi.getDisposed(), "Every ImageReader has to be disposed.");
    }

    @Test
    public void testProvideDisposesImageReadersOnError() throws Exception {
        MCRTiledTestImage.corruptAllTiles(tiledFile);

        assertThrows(MCRIIIFImageProvidingException.class, this::provideFullImage);

        assertTrue(countingSpi.getCreated() > 0, "No ImageReader was created.");
        assertEquals(countingSpi.getCreated(), countingSpi.getDisposed(), "Every ImageReader has to be disposed.");
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
