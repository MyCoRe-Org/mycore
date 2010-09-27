/*
 * $Id$
 * $Revision: 5697 $ $Date: 12.01.2010 $
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

package org.mycore.iview2.frontend;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.common.MCRUtils;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.iview2.services.MCRIView2Tools;
import org.mycore.services.iview2.MCRImage;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRTileCombineServlet extends MCRServlet {
    private static final long serialVersionUID = 7924934677622546958L;

    private ThreadLocal<ImageWriter> imageWriter = new ThreadLocal<ImageWriter>() {

        @Override
        protected ImageWriter initialValue() {
            return ImageIO.getImageWritersBySuffix("jpeg").next();
        }

    };

    private JPEGImageWriteParam imageWriteParam;

    private static Logger LOGGER = Logger.getLogger(MCRTileCombineServlet.class);

    private MCRFooterInterface footerImpl = null;

    private static final String IMAGE_KEY = MCRTileCombineServlet.class.getName() + ".image";

    private static final String THUMBNAIL_KEY = MCRTileCombineServlet.class.getName() + ".thumb";

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
        imageWriteParam.setCompressionQuality(0.75f);
        String footerClassName = getInitParameter(MCRFooterInterface.class.getName());
        if (footerClassName != null) {
            try {
                footerImpl = (MCRFooterInterface) Class.forName(footerClassName).newInstance();
            } catch (Exception e) {
                throw new ServletException("Could not initialize MCRFooterInterface", e);
            }
        }
    }

    @Override
    protected void think(MCRServletJob job) throws Exception {
        try {
            String pathInfo = job.getRequest().getPathInfo();
            if (pathInfo.startsWith("/")) {
                pathInfo = pathInfo.substring(1);
            }
            final String zoomAlias = pathInfo.substring(0, pathInfo.indexOf('/'));
            pathInfo = pathInfo.substring(zoomAlias.length() + 1);
            final String derivate = pathInfo.substring(0, pathInfo.indexOf('/'));
            String imagePath = pathInfo.substring(derivate.length());
            LOGGER.info("Zoom-Level: " + zoomAlias + ", derivate: " + derivate + ", image: " + imagePath);
            File iviewFile = MCRImage.getTiledFile(MCRIView2Tools.getTileDir(), derivate, imagePath);
            job.getRequest().setAttribute(THUMBNAIL_KEY, iviewFile);
            LOGGER.info("IView2 file: " + iviewFile.getAbsolutePath());
            int zoomLevel = 0;
            if (zoomAlias.equals("MIN")) {
                zoomLevel = 1;
            }
            if (zoomAlias.equals("MID")) {
                zoomLevel = 2;
            }
            if (zoomAlias.equals("MAX")) {
                zoomLevel = 3;
            }
            if (zoomLevel == 0 && footerImpl == null) {
                //we're done, sendThumbnail is called in render phase
                return;
            } else {
                BufferedImage combinedImage = MCRIView2Tools.getZoomLevel(iviewFile, zoomLevel);
                if (combinedImage != null) {
                    if (footerImpl != null) {
                        BufferedImage footer = footerImpl.getFooter(combinedImage.getWidth(), derivate, imagePath);
                        combinedImage = attachFooter(combinedImage, footer);
                    }
                    job.getRequest().setAttribute(IMAGE_KEY, combinedImage);
                } else {
                    job.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
        } finally {
            LOGGER.info("Finished sending " + job.getRequest().getPathInfo());
        }
    }

    @Override
    protected void render(MCRServletJob job, Exception ex) throws Exception {
        if (job.getResponse().isCommitted()) {
            return;
        }
        if (ex != null) {
            generateErrorPage(job.getRequest(), job.getResponse(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while aquiring image", ex, false);
            return;
        }
        //check for thumnail
        File iviewFile = (File) job.getRequest().getAttribute(THUMBNAIL_KEY);
        BufferedImage combinedImage = (BufferedImage) job.getRequest().getAttribute(IMAGE_KEY);
        if (iviewFile != null && combinedImage == null) {
            sendThumbnail(iviewFile, job.getResponse());
            return;
        }
        //send combined image
        job.getResponse().setHeader("Cache-Control", "max-age=" + MCRTileServlet.MAX_AGE);
        job.getResponse().setContentType("image/jpeg");
        job.getResponse().setDateHeader("Last-Modified", iviewFile.lastModified());
        Date expires = new Date(System.currentTimeMillis() + MCRTileServlet.MAX_AGE * 1000);
        LOGGER.info("Last-Modified: " + new Date(iviewFile.lastModified()) + ", expire on: " + expires);
        job.getResponse().setDateHeader("Expires", expires.getTime());
        ServletOutputStream sout = job.getResponse().getOutputStream();
        try {
            ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(sout);
            try {
                imageWriter.get().setOutput(imageOutputStream);
                IIOImage iioImage = new IIOImage(combinedImage, null, null);
                imageWriter.get().write(null, iioImage, imageWriteParam);
            } finally {
                imageWriter.get().reset();
                imageOutputStream.close();
            }
        } finally {
            sout.close();
        }
    }

    private static BufferedImage attachFooter(BufferedImage combinedImage, BufferedImage footer) {
        BufferedImage resultImage = new BufferedImage(combinedImage.getWidth(), combinedImage.getHeight() + footer.getHeight(), combinedImage.getType());
        Graphics2D graphics = resultImage.createGraphics();
        try {
            graphics.drawImage(combinedImage, 0, 0, null);
            graphics.drawImage(footer, 0, combinedImage.getHeight(), null);
            return resultImage;
        } finally {
            graphics.dispose();
        }
    }

    private void sendThumbnail(File iviewFile, HttpServletResponse response) throws IOException {
        ZipFile zipFile = new ZipFile(iviewFile);
        try {
            ZipEntry ze = zipFile.getEntry("0/0/0.jpg");
            if (ze != null) {
                response.setHeader("Cache-Control", "max-age=" + MCRTileServlet.MAX_AGE);
                response.setContentType("image/jpeg");
                response.setContentLength((int) ze.getSize());
                ServletOutputStream out = response.getOutputStream();
                InputStream zin = zipFile.getInputStream(ze);
                try {
                    MCRUtils.copyStream(zin, out);
                } finally {
                    zin.close();
                    out.close();
                }
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        } finally {
            zipFile.close();
        }
    }

}
