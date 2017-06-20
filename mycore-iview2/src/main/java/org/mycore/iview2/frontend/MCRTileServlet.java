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

package org.mycore.iview2.frontend;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.imagetiler.MCRImage;
import org.mycore.imagetiler.MCRTiledPictureProps;
import org.mycore.iview2.services.MCRIView2Tools;

/**
 * Get a specific tile of an image.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRTileServlet extends HttpServlet {
    /**
     * how long should a tile be cached by the client
     */
    static final int MAX_AGE = 60 * 60 * 24 * 365; // one year

    private static final long serialVersionUID = 3805114872438336791L;

    private static final Logger LOGGER = LogManager.getLogger(MCRTileServlet.class);

    /**
     * Extracts tile or image properties from iview2 file and transmits it.
     * 
     * Uses {@link HttpServletRequest#getPathInfo()} (see {@link #getTileInfo(String)}) to get tile attributes.
     * Also uses {@link #MAX_AGE} to tell the client how long it could cache the information.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final TileInfo tileInfo = getTileInfo(getPathInfo(req));
        Path iviewFile = getTileFile(tileInfo);
        if (!Files.exists(iviewFile)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File does not exist: " + iviewFile.toString());
            return;
        }
        try (FileSystem iviewFS = MCRIView2Tools.getFileSystem(iviewFile)) {
            Path root = iviewFS.getRootDirectories().iterator().next();
            Path tilePath = root.resolve(tileInfo.tile);
            BasicFileAttributes fileAttributes;
            try {
                fileAttributes = Files.readAttributes(tilePath, BasicFileAttributes.class);
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Tile not found: " + tileInfo);
                return;
            }
            resp.setHeader("Cache-Control", "max-age=" + MAX_AGE);
            resp.setDateHeader("Last-Modified", fileAttributes.lastModifiedTime().toMillis());
            if (tileInfo.tile.endsWith("xml")) {
                resp.setContentType("text/xml");
            } else {
                resp.setContentType("image/jpeg");
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Extracting " + tilePath + " size " + fileAttributes.size());
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
     * Returns at which time the specified tile (see {@link #doGet(HttpServletRequest, HttpServletResponse)} was last modified.
     */
    @Override
    protected long getLastModified(final HttpServletRequest req) {
        final TileInfo tileInfo = getTileInfo(getPathInfo(req));
        try {
            return Files.getLastModifiedTime(getTileFile(tileInfo)).toMillis();
        } catch (IOException e) {
            LOGGER.warn("Could not get lastmodified time.", e);
            return -1;
        }
    }

    /**
     * returns PathInfo from request including ";"
     * @param request
     * @return
     */
    private static String getPathInfo(final HttpServletRequest request) {
        return request.getPathInfo();
    }

    /**
     * returns a {@link TileInfo} for this <code>pathInfo</code>.
     * The format of <code>pathInfo</code> is
     * <code>/{derivateID}/{absoluteImagePath}/{tileCoordinate}</code>
     * where <code>tileCoordinate</code> is either {@value MCRTiledPictureProps#IMAGEINFO_XML} or <code>{z}/{y}/{x}</code> as zoomLevel and x-y-coordinates.
     * @param pathInfo of the described format
     * @return a {@link TileInfo} instance for <code>pathInfo</code>
     */
    static TileInfo getTileInfo(final String pathInfo) {
        LOGGER.debug("Starting MCRTileServlet: " + pathInfo);
        String path = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        final String derivate = path.substring(0, path.indexOf('/'));
        String imagePath = path.substring(derivate.length());
        String tile;
        if (imagePath.endsWith(".xml")) {
            tile = imagePath.substring(imagePath.lastIndexOf('/') + 1);
            imagePath = imagePath.substring(0, imagePath.length() - tile.length() - 1);
        } else {
            int pos = imagePath.length();
            int cnt = 0;
            while (--pos > 0 && cnt < 3) {
                switch (imagePath.charAt(pos)) {
                    case '/':
                        cnt++;
                        break;
                    default:
                }
            }
            tile = imagePath.substring(pos + 2);
            imagePath = imagePath.substring(0, ++pos);
        }
        final TileInfo tileInfo = new TileInfo(derivate, imagePath, tile);
        return tileInfo;
    }

    private static Path getTileFile(TileInfo tileInfo) {
        return MCRImage.getTiledFile(MCRIView2Tools.getTileDir(), tileInfo.derivate, tileInfo.imagePath);
    }

    /**
     * Holds all attributes for a specific tile.
     * @author Thomas Scheffler (yagee)
     *
     */
    static class TileInfo {
        String derivate, imagePath, tile;

        public TileInfo(final String derivate, final String imagePath, final String tile) {
            this.derivate = derivate;
            this.imagePath = imagePath;
            this.tile = tile;
        }

        /**
         * returns "TileInfo [derivate=" + derivate + ", imagePath=" + imagePath + ", tile=" + tile + "]"
         */
        @Override
        public String toString() {
            return "TileInfo [derivate=" + derivate + ", imagePath=" + imagePath + ", tile=" + tile + "]";
        }
    }

}
