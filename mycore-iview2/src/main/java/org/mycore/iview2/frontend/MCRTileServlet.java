/*
 * $Id$
 * $Revision: 5697 $ $Date: 22.10.2009 $
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.common.MCRUtils;
import org.mycore.imagetiler.MCRImage;
import org.mycore.iview2.services.MCRIView2Tools;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRTileServlet extends HttpServlet {

    private static final long serialVersionUID = 3805114872438336791L;

    final static int MAX_AGE = 60 * 60 * 24 * 365; // one year

    private final static Logger LOGGER = Logger.getLogger(MCRTileServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final TileInfo tileInfo = getTileInfo(getPathInfo(req));
        File iviewFile = getTileFile(tileInfo);
        if (!iviewFile.exists()) {
            LOGGER.warn("File does not exist: " + iviewFile.getAbsolutePath());
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            ZipFile zipFile = new ZipFile(iviewFile);
            ZipEntry ze = zipFile.getEntry(tileInfo.tile);
            if (ze != null) {
                resp.setHeader("Cache-Control", "max-age=" + MAX_AGE);
                resp.setDateHeader("Last-Modified", iviewFile.lastModified());
                if (tileInfo.tile.endsWith("xml"))
                    resp.setContentType("text/xml");
                else
                    resp.setContentType("image/jpeg");
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Extracting " + ze.getName() + " size " + ze.getSize());
                //size of a tile or imageinfo.xml file is always smaller than Integer.MAX_VALUE
                resp.setContentLength((int) ze.getSize());
                ServletOutputStream out = resp.getOutputStream();
                InputStream zin = zipFile.getInputStream(ze);
                try {
                    MCRUtils.copyStream(zin, out);
                } finally {
                    zin.close();
                    out.close();
                }
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            LOGGER.debug("Ending MCRTileServlet");
        } catch (Exception e) {
            LOGGER.warn("Error while processing IView2 file", e);
            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            if (e instanceof IOException)
                throw (IOException) e;
            if (e instanceof ServletException)
                throw (ServletException) e;
            throw new RuntimeException(e);
        }
    }

    @Override
    protected long getLastModified(HttpServletRequest req) {
        final TileInfo tileInfo = getTileInfo(getPathInfo(req));
        return getTileFile(tileInfo).lastModified();
    }

    /**
     * returns PathInfo from request including ";"
     * @param request
     * @return
     */
    private String getPathInfo(HttpServletRequest request) {
        return request.getPathInfo();
    }

    static TileInfo getTileInfo(String pathInfo) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Starting MCRTileServlet: " + pathInfo);
        if (pathInfo.startsWith("/"))
            pathInfo = pathInfo.substring(1);
        final String derivate = pathInfo.substring(0, pathInfo.indexOf('/'));
        String imagePath = pathInfo.substring(derivate.length());
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

    private static File getTileFile(TileInfo tileInfo) {
        return MCRImage.getTiledFile(MCRIView2Tools.getTileDir(), tileInfo.derivate, tileInfo.imagePath);
    }

    static class TileInfo {
        String derivate, imagePath, tile;

        public TileInfo(String derivate, String imagePath, String tile) {
            this.derivate = derivate;
            this.imagePath = imagePath;
            this.tile = tile;
        }
    }

}
