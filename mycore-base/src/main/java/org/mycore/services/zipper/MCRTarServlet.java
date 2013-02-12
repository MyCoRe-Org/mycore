package org.mycore.services.zipper;
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet delivers all files of a derivate packed in a tar File
 * 
 * @author sebastian
 * @author shermann
 */
public class MCRTarServlet extends MCRServlet {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRTarServlet.class);

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        HttpServletResponse res = job.getResponse();
        String derivateId = getProperty(job.getRequest(), "id");

        if (derivateId == null || derivateId.length() == 0) {
            job.getResponse().sendError(HttpServletResponse.SC_PRECONDITION_FAILED, "You must provide the id of the derivate");
            return;
        }

        LOGGER.info("Creating tar archive for derivate " + derivateId);
        if (!MCRAccessManager.checkPermission(derivateId, PERMISSION_WRITE)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access forbidden to " + derivateId);
            return;
        }

        TarArchiveOutputStream tos = buildTarOutputStream(res, derivateId);
        putAllFiles(derivateId, tos);
        tos.close();
    }

    /**
     * Puts all files of a derivate to a TarOutputStream.
     * 
     * @param id
     *            the id of the derivate
     * @param tos
     *            the taroutputstream
     * @throws IOException
     */
    private void putAllFiles(String id, TarArchiveOutputStream tos) throws IOException {
        MCRFilesystemNode node = MCRFilesystemNode.getRootNode(id);
        if (node instanceof MCRDirectory) {
            putNode(node, tos);
        }
    }

    /**
     * Puts all childs of a MCRFilesystemNode to a TarOutputStream (works
     * recursive)
     * 
     * @param node
     *            the MCRFilesystemNode that has the childs.
     * @param tos
     *            the TarOutputStream were the childs should be written to.
     * @throws IOException
     */
    private void putNode(MCRFilesystemNode node, TarArchiveOutputStream tos) throws IOException {
        MCRDirectory dir = (MCRDirectory) node;
        MCRFilesystemNode[] children = dir.getChildren();
        for (MCRFilesystemNode child : children) {
            if (child instanceof MCRDirectory) {
                putNode(child, tos);

            }
            if (child instanceof MCRFile) {
                MCRFile mcrFile = (MCRFile) child;
                LOGGER.info("Adding file : " + mcrFile.getPath());
                TarArchiveEntry te = new TarArchiveEntry(mcrFile.getPath());
                te.setModTime(new Date().getTime());
                te.setSize(mcrFile.getSize());

                tos.putArchiveEntry(te);
                mcrFile.getContentTo(tos);
                tos.closeArchiveEntry();
            }
        }
    }

    /**
     * Builds a TarOutputstream from a HttpServletResponse
     * 
     * @param res
     * @param filename
     *            the Name of the downloaded file
     * @return
     * @throws IOException
     */
    private TarArchiveOutputStream buildTarOutputStream(HttpServletResponse res, String filename) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(res.getOutputStream());
        res.setContentType("application/x-tar");
        res.addHeader("Content-Disposition", "atachment; filename=\"" + filename + ".tar\"");

        return new TarArchiveOutputStream(bos);
    }
}
