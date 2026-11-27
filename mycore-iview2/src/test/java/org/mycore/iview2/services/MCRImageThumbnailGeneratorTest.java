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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRImageThumbnailGeneratorTest extends MCRTestCase {

    private static final int THUMBNAIL_SIZE = 100;

    private Path tiledFile;

    private MCRCountingImageReaderSpi countingSpi;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        tiledFile = MCRTiledTestImage.create(junitFolder.newFolder().toPath(), junitFolder.newFolder().toPath(),
            "image.png", 600, 500);
        countingSpi = MCRCountingImageReaderSpi.register();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        countingSpi.deregister();
        super.tearDown();
    }

    @Test
    public void testGetThumbnailDisposesImageReader() throws Exception {
        BufferedImage thumbnail = new MCRImageThumbnailGenerator().getThumbnail(tiledFile, THUMBNAIL_SIZE);

        Assert.assertEquals(THUMBNAIL_SIZE, thumbnail.getHeight());
        Assert.assertTrue("No tile was read.", countingSpi.getTilesRead() > 0);
        Assert.assertTrue("No ImageReader was created.", countingSpi.getCreated() > 0);
        Assert.assertEquals("Every ImageReader has to be disposed.", countingSpi.getCreated(),
            countingSpi.getDisposed());
    }

    @Test
    public void testGetThumbnailDisposesImageReaderOnError() throws Exception {
        MCRTiledTestImage.corruptAllTiles(tiledFile);

        Assert.assertThrows(IOException.class,
            () -> new MCRImageThumbnailGenerator().getThumbnail(tiledFile, THUMBNAIL_SIZE));

        Assert.assertTrue("No ImageReader was created.", countingSpi.getCreated() > 0);
        Assert.assertEquals("Every ImageReader has to be disposed.", countingSpi.getCreated(),
            countingSpi.getDisposed());
    }

}
