/*
 * $Revision$ $Date$
 * 
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.services.zipper;

import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.access.MCRAccessManager.PERMISSION_READ;
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.transform.JDOMSource;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.common.xsl.MCRTemplatesSource;
import org.mycore.common.xsl.MCRXSLTransformerFactory;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet delivers the contents of MycoreObjects to the client as
 * zip-Files. There are three modes a) if id=mycoreobjectID (delivers the
 * metadata, including all derivates) b) if id=derivateID (delivers all files of
 * the derivate) c) if id=derivateID/directoryPath (delivers all files of a
 * special directory of one derivate)
 * 
 * @author Heiko Helmbrecht
 * @version $Revision$ $Date: 2011-05-20 14:25:25 +0200 (Fri, 20 May
 *          2011) $
 */
public class MCRZipServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    // The Log4J logger
    private static Logger LOGGER = Logger.getLogger(MCRZipServlet.class);

    private static String accessErrorPage = MCRConfiguration.instance().getString("MCR.Access.Page.Error");

    protected MCRXMLMetadataManager xmlMetaMgr = null;

    protected String stylesheet;

    /**
     * Initializes the servlet and reads the transforming stylesheet from the
     * configuration.
     */
    @Override
    public void init() throws MCRConfigurationException, ServletException {
        super.init();
        xmlMetaMgr = MCRXMLMetadataManager.instance();
        stylesheet = MCRConfiguration.instance().getString("MCR.zip.metadata.transformer");
    }

    /**
     * Handles the HTTP request
     */
    @Override
    public void doGetPost(MCRServletJob job) throws IOException {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();

        // get Parameter
        String paramid = getProperty(req, "id");

        if (!MCRAccessManager.checkPermission(MCRObjectID.getInstance(paramid), PERMISSION_WRITE)) {
            String ip = MCRSessionMgr.getCurrentSession().getCurrentIP();
            String userId = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
            String msg = "Unsufficient privileges to read content of object \"" + paramid + "\"";
            LOGGER.warn(msg + "[user=" + userId + ", ip=" + ip + "]");
            job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            return;
        }

        String id, path;
        ZipOutputStream out = null;

        Matcher ma = Pattern.compile("\\A([\\w]+)/([\\w/]+)\\z").matcher(paramid);

        if (ma.find()) {
            id = ma.group(1);
            path = ma.group(2);
        } else {
            id = paramid;
            path = null;
        }

        MCRObjectID mcrid = null;

        try {
            mcrid = MCRObjectID.getInstance(id);
        } catch (MCRException e1) {
            String msg = "Error: HTTP request id is not in the allowed id-list";
            LOGGER.error(msg + ":" + id);
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }

        try {
            if (id.indexOf("_derivate_") > 0) {
                if (!MCRAccessManager.checkPermissionForReadingDerivate(id)) {
                    LOGGER.info("MCRFileNodeServlet: AccessForbidden to " + id);
                    res.sendRedirect(res.encodeRedirectURL(getBaseURL() + accessErrorPage));
                    return;
                }
                out = buildZipOutputStream(res, id, path);
                sendDerivate(id, path, out);
                out.close();
            } else {
                Document jdom = xmlMetaMgr.retrieveXML(mcrid);
                if (!MCRAccessManager.checkPermission(id, PERMISSION_READ)) {
                    LOGGER.info("MCRFileNodeServlet: AccessForbidden to " + id);
                    res.sendRedirect(res.encodeRedirectURL(getBaseURL() + accessErrorPage));
                    return;
                }
                out = buildZipOutputStream(res, id, path);
                sendObject(jdom, req, out);
                out.close();
            }
        } catch (Exception e) {
            String msg = "The ZIP file could not be build. Please check the parameters.";
            res.reset();
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }
    }

    /**
     * sendZipped adds a single File to the ZipOutputStream,
     * 
     * @param file
     *            MCRFile, that has to be zipped
     */
    protected void sendZipped(MCRFile file, ZipOutputStream out) throws IOException {
        LOGGER.debug("zipping " + file.getPath());
        ZipEntry ze = new ZipEntry(file.getPath());
        ze.setTime(file.getLastModified().getTime().getTime());
        out.putNextEntry(ze);
        file.getContentTo(out);
        out.closeEntry();
    }

    /**
     * sendZipped adds a whole Directory of an Derivate to the ZipOutputStream
     * 
     * @param directory
     *            MCRDirectory, that has to be zipped
     */
    protected void sendZipped(MCRDirectory directory, ZipOutputStream out) throws IOException {
        MCRFilesystemNode[] nodeArray;
        nodeArray = directory.getChildren();

        for (MCRFilesystemNode element : nodeArray) {
            if (element instanceof MCRFile) {
                sendZipped((MCRFile) element, out);
            } else {
                sendZipped((MCRDirectory) element, out);
            }
        }
    }

    /**
     * sendZipped zips a File with the Metadata of the Object, makes a
     * XSL-Transformation with the Metadata
     * 
     * @param jdom
     *            MycoreObject-ResultContainer as org.jdom2.Document
     * @param parameters
     *            Parameters, that can be needed in the transforming
     *            XSL-Stylesheet
     * @throws JDOMException
     * @throws TransformerException 
     */
    protected void sendZipped(Document jdom, MCRParameterCollector parameters, ZipOutputStream out) throws IOException, JDOMException,
            TransformerException {
        ZipEntry ze = new ZipEntry("metadata.xml");
        ze.setTime(new Date().getTime());
        out.putNextEntry(ze);

        MCRTemplatesSource templates = new MCRTemplatesSource("xsl/" + this.stylesheet);
        Transformer transformer = MCRXSLTransformerFactory.getTransformer(templates);
        if (parameters != null)
            parameters.setParametersTo(transformer);
        StreamResult result = new StreamResult(out);
        transformer.transform(new JDOMSource(jdom), result);
        out.closeEntry();

        return;
    }

    /**
     * sendDerivate sends all files of a derivate or of a special folder of the
     * derivate
     * 
     * @param ownerID
     *            the derivateID that should be zipped
     * @param dirpath
     *            relative path zu a special folder, if given, only this folder
     *            is zipped
     */
    protected void sendDerivate(String ownerID, String dirpath, ZipOutputStream out) throws IOException {

        MCRFilesystemNode root;
        MCRDirectory rootdirectory;
        MCRFilesystemNode zipdirectory;
        root = MCRFilesystemNode.getRootNode(ownerID);

        if (root == null) {
            String msg = "Error: No root node found for owner ID " + ownerID;
            LOGGER.error(msg);

            return;
        }

        if (root instanceof MCRFile) {
            sendZipped((MCRFile) root, out);
            LOGGER.debug("file " + root.getName() + " zipped");

            return;
        }
        // root is a directory
        if (dirpath == null || dirpath.equals("")) {
            sendZipped((MCRDirectory) root, out);
            LOGGER.debug("directory " + root.getName() + " zipped");

            return;
        }
        rootdirectory = (MCRDirectory) root;
        zipdirectory = rootdirectory.getChildByPath(dirpath);

        if (zipdirectory == null) {
            String msg = "Error: No such file or directory " + dirpath;
            LOGGER.error(msg);

            return;
        } else if (zipdirectory instanceof MCRFile) {
            sendZipped((MCRFile) zipdirectory, out);
        } else if (zipdirectory instanceof MCRDirectory) {
            sendZipped((MCRDirectory) zipdirectory, out);
        } else {
            String msg = "Error: could not found the dir: " + dirpath;
            LOGGER.error(msg);

            return;
        }

        return;
    }

    /**
     * sendObject: zips all derivates of a Object and a metadata-xml file, that
     * was built via a given xsl-stylesheet
     * 
     * @param jdom
     *            the JDOM of the given MycoreObject
     * @throws JDOMException
     * @throws TransformerException 
     */
    protected void sendObject(Document jdom, HttpServletRequest req, ZipOutputStream out) throws IOException, JDOMException,
            TransformerException {
        // zip the object's Metadata
        MCRParameterCollector parameters = new MCRParameterCollector(req);
        sendZipped(jdom, parameters, out);

        // zip all derivates
        List<Element> li = jdom.getRootElement().getChild("structure").getChild("derobjects").getChildren("derobject");

        for (Element el : li) {
            LOGGER.debug(el.getName());

            if (el.getAttributeValue("inherited").equals("0")) {
                String ownerID = el.getAttributeValue("href", XLINK_NAMESPACE);
                // here the access check is tested only against the derivate
                if (MCRAccessManager.checkPermission(ownerID, PERMISSION_READ)) {
                    String dir = null;
                    sendDerivate(ownerID, dir, out);
                }
            }
        }

        return;
    }

    /**
     * buildZipOutputStream sets the contenttype and name of the zip-file
     * Returns the ZipOutputStream
     * 
     * @param id
     *            the id of the object or derivate that is zipped, builds the
     *            name
     * @param dirpath
     *            if given, it is concatenated to the name of the zip-file
     */
    protected ZipOutputStream buildZipOutputStream(HttpServletResponse res, String id, String dirpath) throws IOException {
        String filename = dirpath == null || dirpath.equals("") ? id + ".zip" : id + "-" + dirpath.replaceAll("/", "-") + ".zip";
        res.setContentType("multipart/x-zip");
        res.addHeader("Content-Disposition", "atachment; filename=\"" + filename + "\"");

        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(res.getOutputStream()));
        out.setLevel(Deflater.BEST_COMPRESSION);

        return out;
    }
}
