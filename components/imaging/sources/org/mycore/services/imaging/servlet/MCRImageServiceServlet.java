package org.mycore.services.imaging.servlet;

import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileNodeServlet;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.services.imaging.MCRJAIManipBean;

public class MCRImageServiceServlet extends MCRServlet {
    private static Logger LOGGER = Logger.getLogger(MCRImageServiceServlet.class);

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();

        sendImage(request, response);
    }

    private void sendImage(HttpServletRequest request, HttpServletResponse response) {
        double magFactor = 0.0;
        double rotAngle = 0.0;

        String img = request.getParameter("img");
        String rot = request.getParameter("rotAngle");
        String mag = request.getParameter("magFactor");

        if (rot != null) {
            rotAngle = Math.toRadians(Double.valueOf(rot).doubleValue());
        }

        if (mag != null) {
            magFactor = Math.toRadians(Double.valueOf(mag).doubleValue());
        }

        try {
            response.setContentType("image/jpeg");
            ServletContext context = getServletContext();
            String pathInfo = request.getPathInfo();
            MCRFile image = getFileNode(pathInfo);

            MCRJAIManipBean imageBean = new MCRJAIManipBean();
            imageBean.setMagFactor(magFactor);
            imageBean.setRotAngle(rotAngle);
            ServletOutputStream out = response.getOutputStream();

            imageBean.manipAndPost(image.getContentAsInputStream(), out);
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    private MCRFile getFileNode(String path) {
        String ownerID = getOwnerID(path);
        // local node to be retrieved
        MCRFilesystemNode root;

        try {
            root = MCRFilesystemNode.getRootNode(ownerID);
        } catch (org.mycore.common.MCRPersistenceException e) {
            // Could not get value from JDBC result set
            LOGGER.error("MCRFileNodeServlet: Error while getting root node!", e);
            root = null;
        }

        if (root == null) {
            String msg = "Error: No root node found for owner ID " + ownerID;
            LOGGER.error(msg);

            return null;
        }

        if (root instanceof MCRFile) {
            if (path.length() > ownerID.length() + 1) {
                // request path is too long
                String msg = "Error: No such file or directory " + path;
                LOGGER.error(msg);
                return null;
            }

            return (MCRFile) root;
        }

        // root node is a directory
        int pos = ownerID.length() + 1;
        StringBuffer subPath = new StringBuffer(path.substring(pos));
        if ((subPath.charAt(subPath.length() - 1) == '/') && subPath.length() > 1) {
            subPath.deleteCharAt(subPath.length() - 1);
        }
        MCRDirectory dir = (MCRDirectory) root;
        MCRFilesystemNode node = dir.getChildByPath(subPath.toString());

        if (node instanceof MCRFile)
            return (MCRFile) node;

        return null;
    }

    protected static String getOwnerID(String pI) {
        StringBuffer ownerID = new StringBuffer(pI.length());
        boolean running = true;
        for (int i = (pI.charAt(0) == '/') ? 1 : 0; (i < pI.length() && running); i++) {
            switch (pI.charAt(i)) {
            case '/':
                running = false;
                break;
            default:
                ownerID.append(pI.charAt(i));
                break;
            }
        }
        return ownerID.toString();
    }
}
