/*
 * $Id$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.media.frontend;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.datamodel.ifs2.MCRFile;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.media.services.MCRMediaIFSTools;

/**
 * Thumbnail Servlet for MediaObject's.
 * Based on the great IView2 Thumbnail Servlet.
 * 
 * @author Thomas Scheffler (yagee)
 * @author René Adler (Eagle)
 */
public class MCRMediaThumbnailServlet extends MCRServlet {
    private static final long serialVersionUID = 7824368246846287648L;

    private ImageWriteParam imageWriteParam;

    private ConcurrentLinkedQueue<ImageWriter> imageWriters = new ConcurrentLinkedQueue<ImageWriter>();

    private static Logger LOGGER = LogManager.getLogger(MCRMediaThumbnailServlet.class);

    private int thumbnailSize = 128;

    /**
     * how long should a tile be cached by the client
     */
    final static int MAX_AGE = 60 * 60 * 24 * 365; // one year

    @Override
    public void init() throws ServletException {
        super.init();
        imageWriters = new ConcurrentLinkedQueue<ImageWriter>();
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
        LOGGER.info(getServletName() + ": setting thumbnail size to " + thumbnailSize);
    }

    @Override
    public void destroy() {
        for (ImageWriter imageWriter : imageWriters) {
            imageWriter.dispose();
        }
        super.destroy();
    }

    @Override
    protected void render(MCRServletJob job, Exception ex) throws Exception {
        try {
            String pathInfo = job.getRequest().getPathInfo();
            if (pathInfo.startsWith("/"))
                pathInfo = pathInfo.substring(1);
            final String derivate = pathInfo.substring(0, pathInfo.indexOf('/'));
            String imagePath = pathInfo.substring(derivate.length());
            LOGGER.info("derivate: " + derivate + ", image: " + imagePath);
            MCRFile thumbFile = MCRMediaIFSTools.getThumbnailFromStore(derivate, imagePath);
            LOGGER.info("Thumbnail file: " + thumbFile.getPath());
            BufferedImage thumbnail = getThumbnail(thumbFile);
            if (thumbnail != null) {
                thumbnail = centerThumbnail(thumbnail);
                job.getResponse().setHeader("Cache-Control", "max-age=" + MAX_AGE);
                job.getResponse().setContentType("image/png");
                job.getResponse().setDateHeader("Last-Modified", thumbFile.getLastModified().getTime());
                Date expires = new Date(System.currentTimeMillis() + MAX_AGE * 1000);
                LOGGER.info("Last-Modified: " + thumbFile.getLastModified() + ", expire on: " + expires);
                job.getResponse().setDateHeader("Expires", expires.getTime());
                ServletOutputStream sout = job.getResponse().getOutputStream();
                try {
                    ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(sout);
                    ImageWriter imageWriter = getImageWriter();
                    try {
                        imageWriter.setOutput(imageOutputStream);
                        IIOImage iioImage = new IIOImage(thumbnail, null, null);
                        imageWriter.write(null, iioImage, imageWriteParam);
                    } finally {
                        imageWriter.reset();
                        imageWriters.add(imageWriter);
                        imageOutputStream.close();
                    }
                } finally {
                    sout.close();
                }
            } else {
                job.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        } finally {
            LOGGER.info("Finished sending " + job.getRequest().getPathInfo());
        }
    }

    private BufferedImage getThumbnail(MCRFile thumbFile) throws IOException, JDOMException {
        ImageReader reader = getImageReader();
        BufferedImage level1Image = readThumb(thumbFile, reader);
        final double width = level1Image.getWidth();
        final double height = level1Image.getHeight();
        final int newWidth = width < height ? (int) Math.ceil(thumbnailSize * width / height) : thumbnailSize;
        final int newHeight = width < height ? thumbnailSize : (int) Math.ceil(thumbnailSize * height / width);
        int imageType = level1Image.getType();
        if (imageType == BufferedImage.TYPE_CUSTOM) {
            imageType = BufferedImage.TYPE_INT_RGB;
        }
        final BufferedImage bicubic = new BufferedImage(newWidth, newHeight, imageType);
        final Graphics2D bg = bicubic.createGraphics();
        bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        bg.scale(newWidth / width, newHeight / height);
        bg.drawImage(level1Image, 0, 0, null);
        bg.dispose();
        return bicubic;
    }

    private BufferedImage readThumb(MCRFile thumbFile, ImageReader imageReader) throws IOException {
        try {
            InputStream zin = thumbFile.getContent().getInputStream();
            try {
                ImageInputStream iis = ImageIO.createImageInputStream(zin);
                imageReader.setInput(iis, false);
                BufferedImage image = imageReader.read(0);
                imageReader.reset();
                iis.close();
                return image;
            } finally {
                zin.close();
            }
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    private static ImageReader getImageReader() {
        return ImageIO.getImageReadersByMIMEType("image/png").next();
    }

    private ImageWriter getImageWriter() {
        ImageWriter imageWriter = imageWriters.poll();
        if (imageWriter == null) {
            imageWriter = ImageIO.getImageWritersBySuffix("png").next();
        }
        return imageWriter;
    }

    private BufferedImage centerThumbnail(BufferedImage thumbnail) {
        BufferedImage centered = new BufferedImage(thumbnailSize, thumbnailSize, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = centered.getGraphics();
        int x = (thumbnailSize - thumbnail.getWidth()) / 2;
        int y = (thumbnailSize - thumbnail.getHeight()) / 2;
        graphics.drawImage(thumbnail, x, y, null);
        return centered;
    }

}
