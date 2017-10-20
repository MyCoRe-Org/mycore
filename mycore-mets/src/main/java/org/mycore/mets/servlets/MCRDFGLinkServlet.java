/*
 * $Id: MCRDFGLinkServlet.java 29335 2014-03-17 08:53:10Z mcrsroeh $ $Revision:
 * 20489 $ $Date: 2014-03-17 09:53:10 +0100 (Mo, 17. Mär 2014) $
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

package org.mycore.mets.servlets;

import java.net.URLEncoder;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRPathContent;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.mets.model.MCRMETSGeneratorFactory;
import org.mycore.mets.model.Mets;
import org.mycore.mets.model.files.FLocat;
import org.mycore.mets.model.files.File;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.struct.Fptr;
import org.mycore.mets.model.struct.PhysicalDiv;
import org.mycore.mets.model.struct.PhysicalStructMap;
import org.mycore.mets.model.struct.PhysicalSubDiv;

/**
 * This servlet redirects to the DFG-viewer and 
 * sets all parameters for specific images automatically if needed
 * 
 * parameters:
 * deriv = the MyCoReID of the derivate (needed)
 * file = the Filename of the image that had to be shown in the DFG-Viewer (optional)
 * 
 * @author Sebastian Röher (basti890)
 */
public class MCRDFGLinkServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger(MCRDFGLinkServlet.class);

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        String filePath = request.getParameter("file") == null ? "" : request.getParameter("file");
        String derivateID = request.getParameter("deriv") == null ? "" : request.getParameter("deriv");

        if (derivateID.equals("")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Derivate is not set");
        }

        String encodedMetsURL = URLEncoder.encode(MCRServlet.getServletBaseURL() + "MCRMETSServlet/" + derivateID
            + "?XSL.Style=dfg", "UTF-8");
        LOGGER.info(request.getPathInfo());

        MCRPath rootPath = MCRPath.getPath(derivateID, "/");

        if (!Files.isDirectory(rootPath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                MessageFormat.format("Derivate {0} does not exist.", derivateID));
            return;
        }
        request.setAttribute("XSL.derivateID", derivateID);
        Collection<String> linkList = MCRLinkTableManager.instance().getSourceOf(derivateID);
        if (linkList.isEmpty()) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageFormat.format(
                "Derivate {0} is not linked with a MCRObject. Please contact an administrator.", derivateID));
            return;
        }

        // TODO: this seems very very wrong
        if (filePath == "") {
            MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(derivateID));
            filePath = derivate.getDerivate().getInternals().getMainDoc();
        }

        MCRPath metsPath = (MCRPath) rootPath.resolve("mets.xml");
        int imageNumber = -2;
        if (Files.exists(metsPath)) {
            imageNumber = getOrderNumber(new MCRPathContent(metsPath).asXML(), filePath);
        } else {
            MCRContent metsContent = getMetsSource(job, useExistingMets(request), derivateID);
            imageNumber = getOrderNumber(metsContent.asXML(), filePath);
        }

        String dfgURL = "";
        switch (imageNumber) {
            case -1:
                response.sendError(HttpServletResponse.SC_CONFLICT, MessageFormat.format(
                    "Image \"{0}\" not found in the MCRDerivate. Please contact an administrator.", filePath));
                return;
            case -2:
                dfgURL = "http://dfg-viewer.de/show/?set[mets]=" + encodedMetsURL;
                break;
            default:
                dfgURL = "http://dfg-viewer.de/show/?set[mets]=" + encodedMetsURL + "&set[image]=" + imageNumber;
                break;
        }

        response.sendRedirect(dfgURL);
    }

    private static int getOrderNumber(Document metsDoc, String fileHref) {
        int orderNumber = -1;
        String fileID = null;

        try {
            Mets mets = new Mets(metsDoc);
            List<FileGrp> fileGroups = mets.getFileSec().getFileGroups();
            for (FileGrp fileGrp : fileGroups) {
                List<File> fileList = fileGrp.getFileList();
                for (File file : fileList) {
                    FLocat fLocat = file.getFLocat();
                    if (fLocat.getHref().equals(MCRXMLFunctions.encodeURIPath(fileHref)))
                        fileID = file.getId();
                }
            }

            if (fileID != null) {
                PhysicalStructMap structMap = (PhysicalStructMap) mets.getStructMap(PhysicalStructMap.TYPE);
                PhysicalDiv rootDiv = structMap.getDivContainer();
                List<PhysicalSubDiv> children = rootDiv.getChildren();

                for (int index = 0; index < children.size(); index++) {
                    PhysicalSubDiv physicalSubDiv = children.get(index);

                    List<Fptr> fptrList = physicalSubDiv.getChildren();
                    for (Fptr fptr : fptrList) {
                        if (fptr.getFileId().equals(fileID))
                            orderNumber = index + 1;
                    }
                }
            }
        } catch (Exception e) {
            throw new MCRPersistenceException("could not parse mets.xml", e);
        }

        return orderNumber;
    }

    /**
     * Returns the mets document wrapped in a {@link MCRContent} object.
     * 
     * @param job
     * @param useExistingMets
     * @return
     * @throws Exception
     */
    private static MCRContent getMetsSource(MCRServletJob job, boolean useExistingMets, String derivate)
        throws Exception {

        MCRPath metsFile = MCRPath.getPath(derivate, "/mets.xml");

        try {
            job.getRequest().setAttribute("XSL.derivateID", derivate);
            job.getRequest().setAttribute("XSL.objectID",
                MCRLinkTableManager.instance().getSourceOf(derivate).iterator().next());
        } catch (Exception x) {
            LOGGER.warn("Unable to set \"XSL.objectID\" attribute to current request", x);
        }

        boolean metsExists = Files.exists(metsFile);
        if (metsExists && useExistingMets) {
            MCRContent content = new MCRPathContent(metsFile);
            content.setDocType("mets");
            return content;
        } else {
            Document mets = MCRMETSGeneratorFactory.create(metsFile.getParent()).generate().asDocument();
            return new MCRJDOMContent(mets);
        }
    }

    private boolean useExistingMets(HttpServletRequest request) {
        String useExistingMetsParam = request.getParameter("useExistingMets");
        if (useExistingMetsParam == null)
            return true;
        return Boolean.valueOf(useExistingMetsParam);
    }

}
