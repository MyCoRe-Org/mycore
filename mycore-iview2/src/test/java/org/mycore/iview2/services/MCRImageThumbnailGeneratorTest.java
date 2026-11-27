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
package org.mycore.iview2.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRImageThumbnailGeneratorTest {

    private static final int THUMBNAIL_SIZE = 100;

    @TempDir
    Path imageTempDir;

    @TempDir
    Path tileTempDir;

    private Path tiledFile;

    private MCRCountingImageReaderSpi countingSpi;

    @BeforeEach
    public void setUp() throws Exception {
        tiledFile = MCRTiledTestImage.create(imageTempDir, tileTempDir, "image.png", 600, 500);
        countingSpi = MCRCountingImageReaderSpi.register();
    }

    @AfterEach
    public void tearDown() {
        countingSpi.deregister();
    }

    @Test
    public void testGetThumbnailDisposesImageReader() throws Exception {
        BufferedImage thumbnail = new MCRImageThumbnailGenerator().getThumbnail(tiledFile, THUMBNAIL_SIZE);

        assertEquals(THUMBNAIL_SIZE, thumbnail.getHeight());
        assertTrue(countingSpi.getTilesRead() > 0, "No tile was read.");
        assertTrue(countingSpi.getCreated() > 0, "No ImageReader was created.");
        assertEquals(countingSpi.getCreated(), countingSpi.getDisposed(), "Every ImageReader has to be disposed.");
    }

    @Test
    public void testGetThumbnailDisposesImageReaderOnError() throws Exception {
        MCRTiledTestImage.corruptAllTiles(tiledFile);

        assertThrows(IOException.class,
            () -> new MCRImageThumbnailGenerator().getThumbnail(tiledFile, THUMBNAIL_SIZE));

        assertTrue(countingSpi.getCreated() > 0, "No ImageReader was created.");
        assertEquals(countingSpi.getCreated(), countingSpi.getDisposed(), "Every ImageReader has to be disposed.");
    }

}
