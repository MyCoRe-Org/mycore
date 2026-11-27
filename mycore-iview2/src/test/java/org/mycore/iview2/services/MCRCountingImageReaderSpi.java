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
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;

/**
 * Wraps the JPEG {@link ImageReaderSpi} to count created and disposed {@link ImageReader} instances.
 * Use {@link #register()} in test setup and {@link #deregister()} in test teardown.
 */
public final class MCRCountingImageReaderSpi extends ImageReaderSpi {

    private final AtomicInteger created = new AtomicInteger();

    private final AtomicInteger disposed = new AtomicInteger();

    private final AtomicInteger tilesRead = new AtomicInteger();

    private final ImageReaderSpi delegate;

    private MCRCountingImageReaderSpi(ImageReaderSpi delegate) {
        this.delegate = delegate;
        this.vendorName = "MyCoRe";
        this.version = "test";
        this.names = delegate.getFormatNames();
        this.suffixes = delegate.getFileSuffixes();
        this.MIMETypes = delegate.getMIMETypes();
        this.pluginClassName = CountingImageReader.class.getName();
        this.inputTypes = delegate.getInputTypes();
    }

    /**
     * Registers a counting JPEG reader that takes precedence over the default JPEG reader.
     */
    public static MCRCountingImageReaderSpi register() {
        IIORegistry registry = IIORegistry.getDefaultInstance();
        ImageReaderSpi jpegSpi = getJpegReaderSpi();
        MCRCountingImageReaderSpi countingSpi = new MCRCountingImageReaderSpi(jpegSpi);
        registry.registerServiceProvider(countingSpi, ImageReaderSpi.class);
        registry.setOrdering(ImageReaderSpi.class, countingSpi, jpegSpi);
        return countingSpi;
    }

    public void deregister() {
        IIORegistry.getDefaultInstance().deregisterServiceProvider(this, ImageReaderSpi.class);
    }

    public int getCreated() {
        return created.get();
    }

    public int getDisposed() {
        return disposed.get();
    }

    public int getTilesRead() {
        return tilesRead.get();
    }

    private static ImageReaderSpi getJpegReaderSpi() {
        Iterator<ImageReaderSpi> providers = IIORegistry.getDefaultInstance()
            .getServiceProviders(ImageReaderSpi.class, true);
        while (providers.hasNext()) {
            ImageReaderSpi spi = providers.next();
            String[] mimeTypes = spi.getMIMETypes();
            if (!(spi instanceof MCRCountingImageReaderSpi) && mimeTypes != null
                && Arrays.asList(mimeTypes).contains("image/jpeg")) {
                return spi;
            }
        }
        throw new IllegalStateException("No JPEG ImageReader available.");
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

    private static final class CountingImageReader extends ImageReader {

        private final MCRCountingImageReaderSpi spi;

        private final ImageReader delegate;

        CountingImageReader(MCRCountingImageReaderSpi spi, ImageReader delegate) {
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
