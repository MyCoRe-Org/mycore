package org.mycore.services.zipper;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;
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
 * 
 */
public class MCRTarDerivateServlet extends MCRServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger
			.getLogger(MCRTarDerivateServlet.class);

	@Override
	protected void doGetPost(MCRServletJob job) throws Exception {
		HttpServletResponse res = job.getResponse();
		String derivateId = getProperty(job.getRequest(), "id");
		LOGGER.info("TarDerivate :" + derivateId);
		if (!MCRAccessManager.checkPermission(derivateId, "writedb")) {
			res.sendError(HttpServletResponse.SC_FORBIDDEN,
					"FileNodeServlet: access forbidden to " + derivateId);
			return;
		}

		TarOutputStream tos = buildTarOutputStream(res, derivateId);
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
	private void putAllFiles(String id, TarOutputStream tos) throws IOException {
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
	private void putNode(MCRFilesystemNode node, TarOutputStream tos)
			throws IOException {
		MCRDirectory dir = (MCRDirectory) node;
		MCRFilesystemNode[] children = dir.getChildren();
		for (MCRFilesystemNode child : children) {
			if (child instanceof MCRDirectory) {
				putNode(child, tos);

			}
			if (child instanceof MCRFile) {
				MCRFile mcrFile = (MCRFile) child;
				LOGGER.info("Put File : " + mcrFile.getPath());
				TarEntry te = new TarEntry(mcrFile.getPath());
				te.setModTime(new Date().getTime());
				te.setSize(mcrFile.getSize());

				tos.putNextEntry(te);

				mcrFile.getContentTo(tos);

				tos.closeEntry();
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
	private TarOutputStream buildTarOutputStream(HttpServletResponse res,
			String filename) throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(
				res.getOutputStream());
		res.setContentType("application/x-tar");
		res.addHeader("Content-Disposition", "atachment; filename=\""
				+ filename + ".tar\"");

		return new TarOutputStream(bos);
	}
}
