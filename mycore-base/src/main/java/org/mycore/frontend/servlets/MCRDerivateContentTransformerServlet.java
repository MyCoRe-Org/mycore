package org.mycore.frontend.servlets;

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