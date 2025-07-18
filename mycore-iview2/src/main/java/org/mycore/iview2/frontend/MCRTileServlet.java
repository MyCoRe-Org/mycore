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
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.iview2.backend.MCRDefaultTileFileProvider;
import org.mycore.iview2.backend.MCRTileFileProvider;
import org.mycore.iview2.backend.MCRTileInfo;
import org.mycore.iview2.services.MCRIView2Tools;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Get a specific tile of an image.
 * @author Thomas Scheffler (yagee)
 *
 */
@Deprecated
public class MCRTileServlet extends HttpServlet {
    /**
     * how long should a tile be cached by the client
     */
    static final int MAX_AGE = 60 * 60 * 24 * 365; // one year

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRTileFileProvider TFP = MCRConfiguration2.getInstanceOfOrThrow(
        MCRDefaultTileFileProvider.class, "MCR.IIIFImage.Iview.TileFileProvider");

    /**
     * Extracts tile or image properties from iview2 file and transmits it.
     * <p>
     * Uses {@link HttpServletRequest#getPathInfo()} (see {@link #getTileInfo(String)}) to get tile attributes.
     * Also uses {@link #MAX_AGE} to tell the client how long it could cache the information.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final MCRTileInfo tileInfo = getTileInfo(getPathInfo(req));
        Path iviewFile = TFP.getTileFile(tileInfo).orElse(null);
        if (iviewFile == null) {
            LOGGER.info(() -> "TileFile not found: " + tileInfo);
            return;
        }
        if (!Files.exists(iviewFile)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File does not exist: " + iviewFile);
            return;
        }
        try (FileSystem iviewFS = MCRIView2Tools.getFileSystem(iviewFile)) {
            Path root = iviewFS.getRootDirectories().iterator().next();
            Path tilePath = root.resolve(tileInfo.tile());
            BasicFileAttributes fileAttributes;
            try {
                fileAttributes = Files.readAttributes(tilePath, BasicFileAttributes.class);
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Tile not found: " + tileInfo);
                return;
            }
            resp.setHeader("Cache-Control", "max-age=" + MAX_AGE);
            resp.setDateHeader("Last-Modified", fileAttributes.lastModifiedTime().toMillis());
            if (tileInfo.tile().endsWith("xml")) {
                resp.setContentType("text/xml");
            } else {
                resp.setContentType("image/jpeg");
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Extracting {} size {}", tilePath, fileAttributes.size());
            }
            //size of a tile or imageinfo.xml file is always smaller than Integer.MAX_VALUE
            resp.setContentLength((int) fileAttributes.size());
            try (ServletOutputStream out = resp.getOutputStream()) {
                Files.copy(tilePath, out);
            }

        }
        LOGGER.debug("Ending MCRTileServlet");
    }

    /**
     * Returns at which time the specified tile (see {@link #doGet(HttpServletRequest, HttpServletResponse)}
     * was last modified.
     */
    @Override
    protected long getLastModified(final HttpServletRequest req) {
        final MCRTileInfo tileInfo = getTileInfo(getPathInfo(req));
        try {
            return Files
                .getLastModifiedTime(
                    TFP.getTileFile(tileInfo).orElseThrow(() -> new IOException("Could not get file for " + tileInfo)))
                .toMillis();
        } catch (IOException e) {
            LOGGER.warn("Could not get lastmodified time.", e);
            return -1;
        }
    }

    /**
     * returns PathInfo from request including ";"
     */
    private static String getPathInfo(final HttpServletRequest request) {
        return request.getPathInfo();
    }

    /**
     * returns a {@link MCRTileInfo} for this <code>pathInfo</code>.
     * The format of <code>pathInfo</code> is
     * <code>/{derivateID}/{absoluteImagePath}/{tileCoordinate}</code>
     * where <code>tileCoordinate</code> is either {@value org.mycore.imagetiler.MCRTiledPictureProps#IMAGEINFO_XML}
     * or <code>{z}/{y}/{x}</code> as zoomLevel and x-y-coordinates.
     * @param pathInfo of the described format
     * @return a {@link MCRTileInfo} instance for <code>pathInfo</code>
     */
    static MCRTileInfo getTileInfo(final String pathInfo) {
        LOGGER.debug("Starting MCRTileServlet: {}", pathInfo);
        String path = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        final String derivate = path.contains("/") ? path.substring(0, path.indexOf('/')) : null;
        String imagePath = path.substring(derivate.length());
        String tile;
        if (imagePath.endsWith(".xml")) {
            tile = imagePath.substring(imagePath.lastIndexOf('/') + 1);
            imagePath = imagePath.substring(0, imagePath.length() - tile.length() - 1);
        } else {
            int pos = imagePath.length();
            int cnt = 0;
            if(pos > 0) {
                do {
                    pos--;
                    if (imagePath.charAt(pos) == '/') {
                        cnt++;
                    }
                } while(pos > 0 && cnt < 3);
            }
            tile = imagePath.substring(pos + 1);
            imagePath = imagePath.substring(0, pos);
        }
        return new MCRTileInfo(derivate, imagePath, tile);
    }

}
