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

package org.mycore.frontend.servlets;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRContent;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.tools.MCRPNGTools;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * @author Thomas Scheffler(yagee)
 *
 */
public class MCRQRCodeServlet extends MCRContentServlet {

    private static final long serialVersionUID = 1L;

    private static final long CACHE_TIME = TimeUnit.SECONDS.convert(1000L, TimeUnit.DAYS);

    private static final Logger LOGGER = LogManager.getLogger(MCRQRCodeServlet.class);

    private static final Pattern REQUEST_PATTERN = Pattern.compile("/(\\d*)/(.*)");

    private MCRPNGTools pngTools;

    @Override
    public void init() throws ServletException {
        super.init();
        this.pngTools = new MCRPNGTools();
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            this.pngTools.close();
        } catch (Exception e) {
            LOGGER.error("Error while closing PNG tools.", e);
        }
    }

    @Override
    public MCRContent getContent(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        Matcher matcher = REQUEST_PATTERN.matcher(pathInfo);
        if (!matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Path info does not comply to " + REQUEST_PATTERN + ": "
                + pathInfo);
            return null;
        }
        int size = Integer.parseInt(matcher.group(1));
        String relativeURL = matcher.group(2);
        String queryString = req.getQueryString();
        String url = MCRFrontendUtil.getBaseURL() + relativeURL;
        if (queryString != null) {
            url += '?' + queryString;
        }
        LOGGER.info("Generating QR CODE: {}", url);
        MCRContent content = getPNGContent(url, size);
        content.setLastModified(0);
        if (!"HEAD".equals(req.getMethod())) {
            MCRFrontendUtil.writeCacheHeaders(resp, CACHE_TIME, (long) 0, true);
        }
        return content;
    }

    private MCRContent getPNGContent(final String url, final int size) throws IOException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix;
        try {
            matrix = writer.encode(url, BarcodeFormat.QR_CODE, size, size);
        } catch (WriterException e) {
            throw new IOException(e);
        }
        BufferedImage image = toBufferedImage(matrix);
        return pngTools.toPNGContent(image);
    }

    public static BufferedImage toBufferedImage(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        int onColor = Color.BLACK.getRGB();
        int offColor = Color.WHITE.getRGB();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? onColor : offColor);
            }
        }
        return image;
    }

}
