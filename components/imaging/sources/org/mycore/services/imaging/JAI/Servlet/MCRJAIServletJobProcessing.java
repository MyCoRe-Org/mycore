package org.mycore.services.imaging.JAI.Servlet;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.services.imaging.JAI.MCRJAIManipBean;
import org.mycore.services.imaging.servlet.MCRServletJobProcessing;

public class MCRJAIServletJobProcessing implements MCRServletJobProcessing {
    private static Logger LOGGER = Logger.getLogger(MCRJAIServletJobProcessing.class);

    private static HashMap<String, MCRJAIServletParam> paramMap = new HashMap<String, MCRJAIServletParam>();

    public MCRJAIServletJobProcessing() {
        MCRConfiguration mcrConfig = MCRConfiguration.instance();
        Properties properties = mcrConfig.getProperties("MCR.Imaging.Servlet.Parameter.");
        Set<String> stringPropertyNames = properties.stringPropertyNames();

        for (String property : stringPropertyNames) {
            MCRJAIServletParam param = (MCRJAIServletParam) mcrConfig.getInstanceOf(property);
            paramMap.put(param.getParamName(), param);
        }
    }

    public void process(HttpServletRequest request, HttpServletResponse response) {
        MCRJAIManipBean imageBean = new MCRJAIManipBean();
        Enumeration parameterNames = request.getParameterNames();

        while (parameterNames.hasMoreElements()) {
            String paramName = (String) parameterNames.nextElement();

            MCRJAIServletParam servletParam = paramMap.get(paramName);

            if (servletParam != null)
                servletParam.initParam(request, imageBean);

        }

        try {
            response.setContentType("image/jpeg");
            String pathInfo = request.getPathInfo();
            MCRFile image = getFileNode(pathInfo);

            ServletOutputStream out = response.getOutputStream();

            imageBean.manipAndPost(image.getContentAsInputStream(), out);
        } catch (Exception e) {
            e.printStackTrace();
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
