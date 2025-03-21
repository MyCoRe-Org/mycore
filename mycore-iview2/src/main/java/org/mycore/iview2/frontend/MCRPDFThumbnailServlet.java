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

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.servlets.MCRContentServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@Deprecated
public class MCRPDFThumbnailServlet extends MCRContentServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger();

    private int thumbnailSize = 256;

    static final int MAX_AGE = 60 * 60 * 24 * 365; // one year

    private transient MCRPDFTools pdfTools;

    /* (non-Javadoc)
     * @see org.mycore.frontend.servlets.MCRContentServlet#getContent(jakarta.servlet.http.HttpServletRequest,
      * jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    public MCRContent getContent(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ThumnailInfo thumbnailInfo = getThumbnailInfo(req.getPathInfo());
            MCRPath pdfFile = MCRPath.getPath(thumbnailInfo.derivate, thumbnailInfo.filePath);
            LOGGER.info("PDF file: {}", pdfFile);
            if (Files.notExists(pdfFile)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, String.format(Locale.ENGLISH,
                    "Could not find pdf file for %s%s", thumbnailInfo.derivate, thumbnailInfo.filePath));
                return null;
            }
            String centerThumb = req.getParameter("centerThumb");
            String imgSize = req.getParameter("ts");
            //defaults to "yes"
            boolean centered = !Objects.equals(centerThumb, "no");
            int thumbnailSize = imgSize == null ? this.thumbnailSize : Integer.parseInt(imgSize);
            BasicFileAttributes attrs = Files.readAttributes(pdfFile, BasicFileAttributes.class);
            MCRContent imageContent = pdfTools.getThumnail(pdfFile, attrs, thumbnailSize, centered);
            if (imageContent != null) {
                resp.setHeader("Cache-Control", "max-age=" + MAX_AGE);
                Date expires = new Date(System.currentTimeMillis() + MAX_AGE * 1000);
                LOGGER.debug("Last-Modified: {}, expire on: {}", () -> new Date(attrs.lastModifiedTime().toMillis()),
                    () -> expires);
                resp.setDateHeader("Expires", expires.getTime());
            }
            return imageContent;
        } finally {
            LOGGER.debug("Finished sending {}", req::getPathInfo);
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();
        String thSize = getInitParameter("thumbnailSize");
        if (thSize != null) {
            thumbnailSize = Integer.parseInt(thSize);
        }
        pdfTools = new MCRPDFTools();
        LOGGER.info("{}: setting thumbnail size to {}", this::getServletName, () -> thumbnailSize);
    }

    @Override
    public void destroy() {
        try {
            pdfTools.close();
        } catch (Exception e) {
            LOGGER.error("Error while closing PDF tools.", e);
        }
        super.destroy();
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

    private static class ThumnailInfo {
        String derivate;
        String filePath;

        ThumnailInfo(final String derivate, final String imagePath) {
            this.derivate = derivate;
            this.filePath = imagePath;
        }

        @Override
        public String toString() {
            return "TileInfo [derivate=" + derivate + ", filePath=" + filePath + "]";
        }
    }

}
