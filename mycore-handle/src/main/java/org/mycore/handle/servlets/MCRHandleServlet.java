/**
 * 
 */
package org.mycore.handle.servlets;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.handle.MCRHandleManager;

/**
 * @author shermann
 *
 */
public class MCRHandleServlet extends MCRServlet {
    private static final Logger LOGGER = Logger.getLogger(MCRHandleServlet.class);

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        if (!MCRAccessManager.checkPermission("request-handle")) {
            job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN,
                "Sorry but you do not have sufficient permissions.");
            return;
        }

        String derivate = job.getRequest().getParameter("derivate");
        MCRObjectID derID = MCRObjectID.getInstance(derivate);
        if ((derivate == null || derivate.length() == 0) && !MCRMetadataManager.exists(derID)) {
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        MCRPath rootPath = MCRPath.getPath(derID.toString(), "/");

        final MCRDerivate derivateObject = MCRMetadataManager.retrieveMCRDerivate(derID);

        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                MCRPath mcrPath = MCRPath.toMCRPath(file);
                try {
                    if (MCRHandleManager.isHandleRequested(derivateObject, mcrPath.getOwnerRelativePath())
                        || file.getFileName().toString().equalsIgnoreCase("mets.xml")) {
                        LOGGER.info(MessageFormat.format("Handle already set for \"{0}\"", file));
                        return super.visitFile(file, attrs);
                    }
                    LOGGER.info(MessageFormat.format("Requesting handle for {0}", file));
                    MCRHandleManager.requestHandle(mcrPath);
                } catch (Throwable e) {
                    LOGGER.error("Could not request handle for file " + file);
                }
                return super.visitFile(file, attrs);
            }

        });

        toReferrer(job.getRequest(), job.getResponse());
    }
}