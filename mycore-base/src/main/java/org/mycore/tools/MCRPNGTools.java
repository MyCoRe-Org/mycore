package org.mycore.tools;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.streams.MCRByteArrayOutputStream;

public class MCRPNGTools implements AutoCloseable {
    private static Logger LOGGER = LogManager.getLogger(MCRPNGTools.class);

    private static AtomicInteger maxPngSize = new AtomicInteger(64 * 1024);

    private ImageWriteParam imageWriteParam;

    private ConcurrentLinkedQueue<ImageWriter> imageWriters = new ConcurrentLinkedQueue<ImageWriter>();

    public MCRPNGTools() {
        imageWriteParam = ImageIO.getImageWritersBySuffix("png").next().getDefaultWriteParam();
        try {
            imageWriteParam.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
        } catch (UnsupportedOperationException e) {
            LOGGER.warn("Your PNG encoder does not support progressive PNGs.");
        }
    }

    public MCRContent toPNGContent(BufferedImage thumbnail) throws IOException {
        if (thumbnail != null) {
            ImageWriter imageWriter = getImageWriter();
            try (MCRByteArrayOutputStream bout = new MCRByteArrayOutputStream(maxPngSize.get());
                ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(bout)) {
                imageWriter.setOutput(imageOutputStream);
                IIOImage iioImage = new IIOImage(thumbnail, null, null);
                imageWriter.write(null, iioImage, imageWriteParam);
                int contentLength = bout.size();
                maxPngSize.set(Math.max(maxPngSize.get(), contentLength));
                MCRByteContent imageContent = new MCRByteContent(bout.getBuffer(), 0, bout.size());
                imageContent.setMimeType("image/png");
                return imageContent;
            } finally {
                imageWriter.reset();
                imageWriters.add(imageWriter);
            }
        } else {
            return null;
        }
    }

    private ImageWriter getImageWriter() {
        ImageWriter imageWriter = imageWriters.poll();
        if (imageWriter == null) {
            imageWriter = ImageIO.getImageWritersBySuffix("png").next();
        }
        return imageWriter;
    }

    @Override
    public void close() throws Exception {
        for (ImageWriter imageWriter : imageWriters) {
            imageWriter.dispose();
        }
    }

}
