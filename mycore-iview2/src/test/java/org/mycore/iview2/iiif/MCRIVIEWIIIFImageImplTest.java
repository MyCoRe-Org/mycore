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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;

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

public class MCRIVIEWIIIFImageImplTest extends MCRTestCase {

    private static final String IMPL = "ImplTest";

    private static final String IMAGE_PATH = "image.png";

    private static final int IMAGE_WIDTH = 600;

    private static final int IMAGE_HEIGHT = 500;

    private static Path tileDir;

    private CountingImageReaderSpi countingSpi;

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
        Path sourceDir = junitFolder.newFolder().toPath();
        Path sourceImage = sourceDir.resolve(IMAGE_PATH);
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.ORANGE);
        graphics.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        graphics.dispose();
        ImageIO.write(image, "png", sourceImage.toFile());
        MCRImage tiler = MCRImage.getInstance(sourceImage, (String) null, IMAGE_PATH);
        tiler.setTileDir(tileDir);
        tiler.tile();

        IIORegistry registry = IIORegistry.getDefaultInstance();
        ImageReaderSpi jpegSpi = getJpegReaderSpi();
        countingSpi = new CountingImageReaderSpi(jpegSpi);
        registry.registerServiceProvider(countingSpi, ImageReaderSpi.class);
        registry.setOrdering(ImageReaderSpi.class, countingSpi, jpegSpi);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        IIORegistry.getDefaultInstance().deregisterServiceProvider(countingSpi, ImageReaderSpi.class);
        super.tearDown();
    }

    @Test
    public void testProvideDisposesImageReaders() throws Exception {
        MCRIVIEWIIIFImageImpl impl = new MCRIVIEWIIIFImageImpl(IMPL);

        BufferedImage image = impl.provide(IMAGE_PATH,
            new MCRIIIFImageSourceRegion(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT),
            new MCRIIIFImageTargetSize(IMAGE_WIDTH, IMAGE_HEIGHT),
            new MCRIIIFImageTargetRotation(false, 0), MCRIIIFImageQuality.color, "jpg");

        Assert.assertEquals(IMAGE_WIDTH, image.getWidth());
        Assert.assertEquals(IMAGE_HEIGHT, image.getHeight());
        Assert.assertTrue("No tile was read.", countingSpi.tilesRead.get() > 1);
        Assert.assertTrue("No ImageReader was created.", countingSpi.created.get() > 0);
        Assert.assertEquals("Every ImageReader has to be disposed.", countingSpi.created.get(),
            countingSpi.disposed.get());
    }

    @Test
    public void testProvideDisposesImageReadersOnError() throws Exception {
        corruptAllTiles();
        MCRIVIEWIIIFImageImpl impl = new MCRIVIEWIIIFImageImpl(IMPL);

        Assert.assertThrows(MCRIIIFImageProvidingException.class, () -> impl.provide(IMAGE_PATH,
            new MCRIIIFImageSourceRegion(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT),
            new MCRIIIFImageTargetSize(IMAGE_WIDTH, IMAGE_HEIGHT),
            new MCRIIIFImageTargetRotation(false, 0), MCRIIIFImageQuality.color, "jpg"));

        Assert.assertTrue("No ImageReader was created.", countingSpi.created.get() > 0);
        Assert.assertEquals("Every ImageReader has to be disposed.", countingSpi.created.get(),
            countingSpi.disposed.get());
    }

    private static void corruptAllTiles() throws IOException {
        Path tiledFile = MCRImage.getTiledFile(tileDir, null, IMAGE_PATH);
        try (FileSystem zipFileSystem = FileSystems.newFileSystem(tiledFile);
            Stream<Path> files = Files.walk(zipFileSystem.getPath("/"))) {
            for (Path tile : files.filter(path -> path.toString().endsWith(".jpg")).toList()) {
                Files.write(tile, "this is not a JPEG file".getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private static ImageReaderSpi getJpegReaderSpi() {
        Iterator<ImageReaderSpi> providers = IIORegistry.getDefaultInstance()
            .getServiceProviders(ImageReaderSpi.class, true);
        while (providers.hasNext()) {
            ImageReaderSpi spi = providers.next();
            String[] mimeTypes = spi.getMIMETypes();
            if (mimeTypes != null && Arrays.asList(mimeTypes).contains("image/jpeg")) {
                return spi;
            }
        }
        throw new IllegalStateException("No JPEG ImageReader available.");
    }

    public static class TestTileFileProvider implements MCRTileFileProvider {

        @Override
        public Optional<Path> getTileFile(MCRTileInfo tileInfo) {
            return Optional.of(MCRImage.getTiledFile(tileDir, tileInfo.getDerivate(), tileInfo.getImagePath()));
        }

    }

    /**
     * Wraps the JPEG {@link ImageReaderSpi} to count created and disposed {@link ImageReader} instances.
     */
    private static final class CountingImageReaderSpi extends ImageReaderSpi {

        final AtomicInteger created = new AtomicInteger();

        final AtomicInteger disposed = new AtomicInteger();

        final AtomicInteger tilesRead = new AtomicInteger();

        private final ImageReaderSpi delegate;

        CountingImageReaderSpi(ImageReaderSpi delegate) {
            this.delegate = delegate;
            this.vendorName = "MyCoRe";
            this.version = "test";
            this.names = delegate.getFormatNames();
            this.suffixes = delegate.getFileSuffixes();
            this.MIMETypes = delegate.getMIMETypes();
            this.pluginClassName = CountingImageReader.class.getName();
            this.inputTypes = delegate.getInputTypes();
        }

        @Override
        public boolean canDecodeInput(Object source) throws IOException {
            return delegate.canDecodeInput(source);
        }

        @Override
        public ImageReader createReaderInstance(Object extension) throws IOException {
            created.incrementAndGet();
            return new CountingImageReader(this, delegate.createReaderInstance(extension));
        }

        @Override
        public String getDescription(Locale locale) {
            return "counting " + delegate.getDescription(locale);
        }

    }

    private static final class CountingImageReader extends ImageReader {

        private final CountingImageReaderSpi spi;

        private final ImageReader delegate;

        CountingImageReader(CountingImageReaderSpi spi, ImageReader delegate) {
            super(spi);
            this.spi = spi;
            this.delegate = delegate;
        }

        @Override
        public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
            super.setInput(input, seekForwardOnly, ignoreMetadata);
            delegate.setInput(input, seekForwardOnly, ignoreMetadata);
        }

        @Override
        public void reset() {
            super.reset();
            delegate.reset();
        }

        @Override
        public void dispose() {
            spi.disposed.incrementAndGet();
            delegate.dispose();
        }

        @Override
        public int getNumImages(boolean allowSearch) throws IOException {
            return delegate.getNumImages(allowSearch);
        }

        @Override
        public int getWidth(int imageIndex) throws IOException {
            return delegate.getWidth(imageIndex);
        }

        @Override
        public int getHeight(int imageIndex) throws IOException {
            return delegate.getHeight(imageIndex);
        }

        @Override
        public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
            return delegate.getImageTypes(imageIndex);
        }

        @Override
        public IIOMetadata getStreamMetadata() throws IOException {
            return delegate.getStreamMetadata();
        }

        @Override
        public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
            return delegate.getImageMetadata(imageIndex);
        }

        @Override
        public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
            spi.tilesRead.incrementAndGet();
            return delegate.read(imageIndex, param);
        }

    }

}
