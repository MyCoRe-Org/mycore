/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.iview2.frontend;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.niofs.MCRPathUtils;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.imagetiler.MCRImage;
import org.mycore.imagetiler.MCRTiledPictureProps;
import org.mycore.iview2.services.MCRIView2Tools;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@Deprecated
public class MCRThumbnailServlet extends MCRServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    //stores max png size for byte array buffer of output
    private transient AtomicInteger maxPngSize = new AtomicInteger(64 * 1024);

    private transient ImageWriteParam imageWriteParam;

    private Queue<ImageWriter> imageWriters = new ConcurrentLinkedQueue<>();

    private static final Logger LOGGER = LogManager.getLogger();

    private int thumbnailSize = MCRImage.getTileSize();

    private static LoadingCache<String, Long> modifiedCache = CacheBuilder.newBuilder().maximumSize(5000)
        .expireAfterWrite(MCRTileServlet.MAX_AGE, TimeUnit.SECONDS).weakKeys().build(new CacheLoader<>() {
            @Override
            public Long load(String id) {
                ThumnailInfo thumbnailInfo = getThumbnailInfo(id);
                Path iviewFile = MCRImage.getTiledFile(MCRIView2Tools.getTileDir(), thumbnailInfo.derivate,
                    thumbnailInfo.imagePath);
                try {
                    return Files.readAttributes(iviewFile, BasicFileAttributes.class).lastModifiedTime().toMillis();
                } catch (IOException x) {
                    return -1L;
                }
            }
        });

    @Override
    public void init() throws ServletException {
        super.init();
        imageWriters = new ConcurrentLinkedQueue<>();
        imageWriteParam = ImageIO.getImageWritersBySuffix("png").next().getDefaultWriteParam();
        try {
            imageWriteParam.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
        } catch (UnsupportedOperationException e) {
            LOGGER.warn("Your PNG encoder does not support progressive PNGs.");
        }
        String thSize = getInitParameter("thumbnailSize");
        if (thSize != null) {
            thumbnailSize = Integer.parseInt(thSize);
        }
        LOGGER.info("{}: setting thumbnail size to {}", this::getServletName, () -> thumbnailSize);
    }

    @Override
    public void destroy() {
        for (ImageWriter imageWriter : imageWriters) {
            imageWriter.dispose();
        }
        super.destroy();
    }

    @Override
    protected long getLastModified(HttpServletRequest request) {
        return modifiedCache.getUnchecked(request.getPathInfo());
    }

    @Override
    protected void render(MCRServletJob job, Exception ex) throws IOException {
        try {
            ThumnailInfo thumbnailInfo = getThumbnailInfo(job.getRequest().getPathInfo());
            Path iviewFile = MCRImage.getTiledFile(MCRIView2Tools.getTileDir(), thumbnailInfo.derivate,
                thumbnailInfo.imagePath);
            LOGGER.info("IView2 file: {}", iviewFile);
            BasicFileAttributes fileAttributes = MCRPathUtils.getAttributes(iviewFile, BasicFileAttributes.class);
            if (fileAttributes == null) {
                job.getResponse().sendError(
                    HttpServletResponse.SC_NOT_FOUND,
                    new MessageFormat("Could not find iview2 file for {0}{1}", Locale.ROOT)
                        .format(new Object[] { thumbnailInfo.derivate, thumbnailInfo.imagePath }));
                return;
            }
            String centerThumb = job.getRequest().getParameter("centerThumb");
            //defaults to "yes"
            boolean centered = !Objects.equals(centerThumb, "no");
            BufferedImage thumbnail = getThumbnail(iviewFile, centered);

            if (thumbnail != null) {
                job.getResponse().setHeader("Cache-Control", "max-age=" + MCRTileServlet.MAX_AGE);
                job.getResponse().setContentType("image/png");
                job.getResponse().setDateHeader("Last-Modified", fileAttributes.lastModifiedTime().toMillis());
                Date expires = new Date(System.currentTimeMillis() + MCRTileServlet.MAX_AGE * 1000);
                LOGGER.debug("Last-Modified: {}, expire on: {}", fileAttributes::lastModifiedTime, () -> expires);
                job.getResponse().setDateHeader("Expires", expires.getTime());

                ImageWriter imageWriter = getImageWriter();
                try (ServletOutputStream sout = job.getResponse().getOutputStream();
                    ByteArrayOutputStream bout = new ByteArrayOutputStream(maxPngSize.get());
                    ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(bout)) {
                    imageWriter.setOutput(imageOutputStream);
                    //tile = addWatermark(scaleBufferedImage(tile));
                    IIOImage iioImage = new IIOImage(thumbnail, null, null);
                    imageWriter.write(null, iioImage, imageWriteParam);
                    int contentLength = bout.size();
                    maxPngSize.set(Math.max(maxPngSize.get(), contentLength));
                    job.getResponse().setContentLength(contentLength);
                    bout.writeTo(sout);
                } finally {
                    imageWriter.reset();
                    imageWriters.add(imageWriter);
                }
            } else {
                job.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } finally {
            LOGGER.debug("Finished sending {}", () -> job.getRequest().getPathInfo());
        }
    }

    private static ThumnailInfo getThumbnailInfo(String pathInfo) {
        String pInfo = pathInfo;
        if (pInfo.startsWith("/")) {
            pInfo = pInfo.substring(1);
        }
        final String derivate = pInfo.substring(0, pInfo.indexOf('/'));
        String imagePath = pInfo.substring(derivate.length());
        LOGGER.debug("derivate: {}, image: {}", derivate, imagePath);
        return new ThumnailInfo(derivate, imagePath);
    }

    private BufferedImage getThumbnail(Path iviewFile, boolean centered) throws IOException {
        BufferedImage level1Image = getImageInZoomLevel1(iviewFile);
        final double width = level1Image.getWidth();
        final double height = level1Image.getHeight();
        final int newWidth = width < height ? (int) Math.ceil(thumbnailSize * width / height) : thumbnailSize;
        final int newHeight = width < height ? thumbnailSize : (int) Math.ceil(thumbnailSize * height / width);
        if (centered) {
            //if centered make thumbnailSize x thumbnailSize, transparent image
            final BufferedImage bicubic = new BufferedImage(thumbnailSize, thumbnailSize, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D bg = bicubic.createGraphics();
            try {
                bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                int x = (thumbnailSize - newWidth) / 2;
                int y = (thumbnailSize - newHeight) / 2;
                if (x != 0 && y != 0) {
                    LOGGER.warn("Writing at position {},{}", x, y);
                }
                bg.drawImage(level1Image, x, y, x + newWidth, y + newHeight, 0, 0, (int) Math.ceil(width),
                    (int) Math.ceil(height), null);
            } finally {
                bg.dispose();
            }
            return bicubic;
        }
        //not centered
        final BufferedImage bicubic = new BufferedImage(newWidth, newHeight, MCRImage.getImageType(level1Image));
        final Graphics2D bg = bicubic.createGraphics();
        try {
            bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            bg.drawImage(level1Image, 0, 0, newWidth, newHeight, 0, 0, (int) Math.ceil(width),
                (int) Math.ceil(height), null);
        } finally {
            bg.dispose();
        }
        return bicubic;
    }

    private static BufferedImage getImageInZoomLevel1(Path iviewFile) throws IOException {
        try (FileSystem fs = MCRIView2Tools.getFileSystem(iviewFile)) {
            Path iviewFileRoot = fs.getRootDirectories().iterator().next();
            MCRTiledPictureProps props = MCRTiledPictureProps.getInstanceFromDirectory(iviewFileRoot);
            //get next bigger zoomLevel and scale image to THUMBNAIL_SIZE
            ImageReader reader = MCRIView2Tools.getTileImageReader();
            try {
                return MCRIView2Tools.getZoomLevel(iviewFileRoot, props, reader, Math.min(1, props.getZoomlevel()));
            } finally {
                reader.dispose();
            }
        }
    }

    private ImageWriter getImageWriter() {
        ImageWriter imageWriter = imageWriters.poll();
        if (imageWriter == null) {
            imageWriter = ImageIO.getImageWritersBySuffix("png").next();
        }
        return imageWriter;
    }

    private static class ThumnailInfo {
        String derivate;
        String imagePath;

        ThumnailInfo(final String derivate, final String imagePath) {
            this.derivate = derivate;
            this.imagePath = imagePath;
        }

        @Override
        public String toString() {
            return "TileInfo [derivate=" + derivate + ", imagePath=" + imagePath + "]";
        }
    }

}
