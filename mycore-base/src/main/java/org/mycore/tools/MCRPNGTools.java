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

    private ConcurrentLinkedQueue<ImageWriter> imageWriters = new ConcurrentLinkedQueue<>();

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
