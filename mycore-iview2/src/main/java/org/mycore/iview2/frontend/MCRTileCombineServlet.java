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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRClassTools;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.imagetiler.MCRImage;
import org.mycore.imagetiler.MCRTiledPictureProps;
import org.mycore.iview2.services.MCRIView2Tools;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Combines tiles of an image in specific resolutions.
 *
 * @author Thomas Scheffler (yagee)
 */
@Deprecated
public class MCRTileCombineServlet extends MCRServlet {

    /** key of request attribute for {@link BufferedImage}. */
    protected static final String IMAGE_KEY = MCRTileCombineServlet.class.getName() + ".image";

    /** key of request attribute for iview2-{@link File}. */
    protected static final String THUMBNAIL_KEY = MCRTileCombineServlet.class.getName() + ".thumb";

    @Serial
    private static final long serialVersionUID = 1L;

    private static final float QUALITY = 0.75f;

    private static final Logger LOGGER = LogManager.getLogger();

    private transient ThreadLocal<ImageWriter> imageWriter = ThreadLocal.withInitial(
        () -> ImageIO.getImageWritersBySuffix("jpeg").next());

    private transient JPEGImageWriteParam imageWriteParam;

    private MCRFooterInterface footerImpl;

    /**
     * Initializes this instance.
     * <p>
     * Use parameter <code>org.mycore.iview2.frontend.MCRFooterInterface</code> to specify implementation of
     * {@link MCRFooterInterface} (can be omitted).
     */
    @Override
    public void init() throws ServletException {
        super.init();
        imageWriteParam = new JPEGImageWriteParam(Locale.getDefault());
        try {
            imageWriteParam.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
        } catch (UnsupportedOperationException e) {
            LOGGER.warn("Your JPEG encoder does not support progressive JPEGs.");
        }
        imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        imageWriteParam.setCompressionQuality(QUALITY);
        String footerClassName = getInitParameter(MCRFooterInterface.class.getName());
        if (footerClassName != null) {
            try {
                footerImpl = (MCRFooterInterface) MCRClassTools.forName(footerClassName).getDeclaredConstructor()
                    .newInstance();
            } catch (Exception e) {
                throw new ServletException("Could not initialize MCRFooterInterface", e);
            }
        }
    }

    /**
     * prepares render process and gets IView2 file and combines tiles.
     * The image dimensions and path are determined from {@link HttpServletRequest#getPathInfo()}:
     * <code>/{zoomAlias}/{derivateID}/{absoluteImagePath}</code>
     * where <code>zoomAlias</code> is mapped like this:
     * <table>
     * <caption>Mapping of zoomAlias to actual zoom level</caption>
     * <tr><th>zoomAlias</th><th>zoom level</th></tr>
     * <tr><td>'MIN'</td><td>1</td></tr>
     * <tr><td>'MID'</td><td>2</td></tr>
     * <tr><td>'MAX'</td><td>3</td></tr>
     * <tr><td>default and all others</td><td>0</td></tr>
     * </table>
     *
     * See {@link #init()} how to attach a footer to every generated image.
     */
    @Override
    protected void think(final MCRServletJob job) throws IOException, URISyntaxException {
        final HttpServletRequest request = job.getRequest();
        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo.startsWith("/")) {
                pathInfo = pathInfo.substring(1);
            }
            String zoomAlias = pathInfo.substring(0, pathInfo.indexOf('/'));
            pathInfo = pathInfo.substring(zoomAlias.length() + 1);
            final String derivate = pathInfo.substring(0, pathInfo.indexOf('/'));
            String imagePath = pathInfo.substring(derivate.length());
            LOGGER.info("Zoom-Level: {}, derivate: {}, image: {}", zoomAlias, derivate, imagePath);
            final Path iviewFile = MCRImage.getTiledFile(MCRIView2Tools.getTileDir(), derivate, imagePath);
            try (FileSystem fs = MCRIView2Tools.getFileSystem(iviewFile)) {
                Path iviewFileRoot = fs.getRootDirectories().iterator().next();
                final MCRTiledPictureProps pictureProps = MCRTiledPictureProps.getInstanceFromDirectory(iviewFileRoot);
                final int maxZoomLevel = pictureProps.getZoomlevel();
                request.setAttribute(THUMBNAIL_KEY, iviewFile);
                LOGGER.info("IView2 file: {}", iviewFile);
                int zoomLevel = switch (zoomAlias) {
                    case "MIN" -> 1;
                    case "MID" -> 2;
                    case "MAX" -> 3;
                    default -> 0;
                };
                HttpServletResponse response = job.getResponse();
                if (zoomLevel > maxZoomLevel) {
                    redirectToMaxZoomLevelAvailable(maxZoomLevel, derivate, imagePath, request, response);
                    return;
                }
                if (zoomLevel == 0 && footerImpl == null) {
                    //we're done, sendThumbnail is called in render phase
                    return;
                }
                ImageReader reader = MCRIView2Tools.getTileImageReader();
                try {
                    BufferedImage combinedImage = MCRIView2Tools.getZoomLevel(iviewFileRoot, pictureProps, reader,
                        zoomLevel);
                    if (combinedImage != null) {
                        if (footerImpl != null) {
                            BufferedImage footer = footerImpl.getFooter(combinedImage.getWidth(), derivate, imagePath);
                            combinedImage = attachFooter(combinedImage, footer);
                        }
                        request.setAttribute(IMAGE_KEY, combinedImage);
                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    }
                } finally {
                    reader.dispose();
                }
            }

        } finally {
            LOGGER.info("Finished sending {}", request::getPathInfo);
        }
    }

    private static void redirectToMaxZoomLevelAvailable(int maxZoomLevel, String derivate, String imagePath,
        HttpServletRequest request, HttpServletResponse response) throws URISyntaxException, IOException {
        String zoomAlias;
        zoomAlias = switch (maxZoomLevel) {
            case 2 -> "MID";
            case 1 -> "MIN";
            default -> "THUMB";
        };
        String redirectURL = response.encodeRedirectURL(new MessageFormat("{0}{1}/{2}/{3}{4}", Locale.ROOT)
            .format(new Object[] { request.getContextPath(), request.getServletPath(), zoomAlias, derivate,
                MCRXMLFunctions.encodeURIPath(imagePath.charAt(0) == '/' ? imagePath : "/" + imagePath) }));
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", redirectURL);
        response.flushBuffer();
    }

    /**
     * Transmits combined file or sends thumbnail.
     * Uses {@link HttpServletRequest#getAttribute(String)} to retrieve information generated
     * by {@link #think(MCRServletJob)}.
     * <table>
     * <caption>description of {@link HttpServletRequest} attributes</caption>
     * <tr><th>keyName</th><th>type</th><th>description</th></tr>
     * <tr><td>{@link #THUMBNAIL_KEY}</td><td>{@link File}</td><td>.iview2 File with all tiles in it</td></tr>
     * <tr><td>{@link #IMAGE_KEY}</td><td>{@link BufferedImage}</td>
     * <td>generated image if <code>zoomLevel != 0</code> and no implementation
     * of {@link MCRFooterInterface} defined</td></tr>
     * </table>
     */
    @Override
    protected void render(final MCRServletJob job, final Exception ex) throws Exception {
        if (job.getResponse().isCommitted()) {
            return;
        }
        if (ex != null) {
            throw ex;
        }
        //check for thumnail
        final File iviewFile = ((Path) job.getRequest().getAttribute(THUMBNAIL_KEY)).toFile();
        final BufferedImage combinedImage = (BufferedImage) job.getRequest().getAttribute(IMAGE_KEY);
        if (iviewFile != null && combinedImage == null) {
            sendThumbnail(iviewFile, job.getResponse());
            return;
        }
        //send combined image
        job.getResponse().setHeader("Cache-Control", "max-age=" + MCRTileServlet.MAX_AGE);
        job.getResponse().setContentType("image/jpeg");
        job.getResponse().setDateHeader("Last-Modified", iviewFile.lastModified());
        final Instant expires = Instant.now().plus(MCRTileServlet.MAX_AGE, TimeUnit.SECONDS.toChronoUnit());
        LOGGER.info("Last-Modified: {}, expire on: {}", () -> Instant.ofEpochMilli(iviewFile.lastModified()),
            () -> expires);
        job.getResponse().setDateHeader("Expires", expires.toEpochMilli());

        final ImageWriter curImgWriter = imageWriter.get();
        try (ServletOutputStream sout = job.getResponse().getOutputStream();
            ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(sout)) {
            curImgWriter.setOutput(imageOutputStream);
            final IIOImage iioImage = new IIOImage(combinedImage, null, null);
            curImgWriter.write(null, iioImage, imageWriteParam);
        } finally {
            curImgWriter.reset();
        }
    }

    /**
     * attaches <code>footer</code> to <code>combinedImage</code>.
     * @param combinedImage image to attach footer to
     * @param footer image of same with as <code>combinedImage</code>
     * @return a {@link BufferedImage} with <code>footer</code> attached to <code>combinedImage</code>
     */
    protected static BufferedImage attachFooter(final BufferedImage combinedImage, final BufferedImage footer) {
        final BufferedImage resultImage = new BufferedImage(combinedImage.getWidth(), combinedImage.getHeight()
            + footer.getHeight(), combinedImage.getType());
        final Graphics2D graphics = resultImage.createGraphics();
        try {
            graphics.drawImage(combinedImage, 0, 0, null);
            graphics.drawImage(footer, 0, combinedImage.getHeight(), null);
            return resultImage;
        } finally {
            graphics.dispose();
        }
    }

    private void sendThumbnail(final File iviewFile, final HttpServletResponse response) throws IOException {
        try (ZipFile zipFile = new ZipFile(iviewFile)) {
            final ZipEntry ze = zipFile.getEntry("0/0/0.jpg");
            if (ze != null) {
                response.setHeader("Cache-Control", "max-age=" + MCRTileServlet.MAX_AGE);
                response.setContentType("image/jpeg");
                response.setContentLength((int) ze.getSize());
                try (ServletOutputStream out = response.getOutputStream();
                    InputStream zin = zipFile.getInputStream(ze)) {
                    zin.transferTo(out);
                }
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

}
