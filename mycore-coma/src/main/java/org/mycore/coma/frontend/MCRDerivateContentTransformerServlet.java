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

package org.mycore.coma.frontend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRPathContent;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRContentServlet;
import org.xml.sax.SAXException;

/**
 * This servlet transforms and delivers xml content from a derivate.
 * usage: <code>servlet/derivate_id/path/to/file.xml</code>
 *
 * @author mcrshofm
 */
public class MCRDerivateContentTransformerServlet extends MCRContentServlet {

    private static final int CACHE_TIME = 24 * 60 * 60;

    private static final Logger LOGGER = LogManager.getLogger(MCRDerivateContentTransformerServlet.class);

    @Override
    public MCRContent getContent(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo.startsWith("/")) {
            pathInfo = pathInfo.substring(1);
        }

        String[] pathTokens = pathInfo.split("/");
        String derivate = pathTokens[0];
        String path = pathInfo.substring(derivate.length());

        LOGGER.debug("Derivate : {}", derivate);
        LOGGER.debug("Path : {}", path);

        MCRPath mcrPath = MCRPath.getPath(derivate, path);
        MCRContent pc = new MCRPathContent(mcrPath);

        FileTime lastModifiedTime = Files.getLastModifiedTime(mcrPath);

        MCRFrontendUtil.writeCacheHeaders(resp, (long) CACHE_TIME, lastModifiedTime.toMillis(), true);

        try {
            return getLayoutService().getTransformedContent(req, resp, pc);
        } catch (TransformerException | SAXException e) {
            throw new IOException("could not transform content", e);
        }
    }

}
