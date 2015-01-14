package org.mycore.coma.frontend;

import org.apache.log4j.Logger;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRPathContent;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.servlets.MCRContentServlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;

public class MCRDerivateContentTransformerServlet extends MCRContentServlet {

    private static final Logger LOGGER = Logger.getLogger(MCRDerivateContentTransformerServlet.class);
    public static final int CACHE_TIME = 24 * 60 * 60;

    @Override
    /**
     * Gets content from a derivate
     */
    public MCRContent getContent(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo.startsWith("/")) {
            pathInfo = pathInfo.substring(1);
        }

        String[] pathTokens = pathInfo.split("/");
        String derivate = pathTokens[0];
        String path = pathInfo.substring(derivate.length());

        LOGGER.info("derivate : " + derivate);
        LOGGER.info("path : " + path);

        MCRPath mcrPath = MCRPath.getPath(derivate, path);
        MCRContent pc = new MCRPathContent(mcrPath);

        FileTime lastModifiedTime = Files.getLastModifiedTime(mcrPath);

        writeCacheHeaders(resp, CACHE_TIME, lastModifiedTime.toMillis(), true);
        try {
            pc = getLayoutService().getTransformedContent(req, resp, pc);
        } catch (TransformerException e) {
            LOGGER.error("error while doing layout!", e);
        } finally {
            return pc;
        }
    }

}
