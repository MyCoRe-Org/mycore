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
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.mets.model.MCRMETSGenerator;
import org.mycore.mets.model.Mets;
import org.mycore.mets.model.files.FLocat;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.struct.Fptr;
import org.mycore.mets.model.struct.PhysicalDiv;
import org.mycore.mets.model.struct.PhysicalStructMap;
import org.mycore.mets.model.struct.PhysicalSubDiv;

/**
 * This servlet redirect to the DFG-viewer and 
 * sets all parameters for specific images automatically if needed
 * 
 * parameters:
 * deriv = the MyCoReID of the derivate
 * file = the Filename of the image
 * 
 * @author Sebastian Röher (basti890)
 */
public class MCRDFGLinkServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRDFGLinkServlet.class);

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        String filePath = job.getRequest().getParameter("file");
        String derivate = job.getRequest().getParameter("deriv");
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        int imageNumber = -1;

        String encodedMetsURL = URLEncoder.encode(MCRServlet.getServletBaseURL() + "MCRMETSServlet/" + derivate + "?XSL.Style=dfg", "UTF-8");
        LOGGER.info(request.getPathInfo());

        MCRDirectory dir = MCRDirectory.getRootDirectory(derivate);

        if (dir == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, MessageFormat.format("Derivate {0} does not exist.", derivate));
            return;
        }
        request.setAttribute("XSL.derivateID", derivate);
        Collection<String> linkList = MCRLinkTableManager.instance().getSourceOf(derivate);
        if (linkList.isEmpty()) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    MessageFormat.format("Derivate {0} is not linked with a MCRObject. Please contact an administrator.", derivate));
            return;
        }

        if (filePath == null) {
            String dfgURL = "http://dfg-viewer.de/show/?set[mets]=" + encodedMetsURL;
            response.sendRedirect(dfgURL);
            return;
        }

        MCRFile metsFile = (MCRFile) dir.getChildByPath("mets.xml");
        if (metsFile != null) {
            imageNumber = getOrderNumber(metsFile.getContent().asXML(), filePath);
        } else {
            MCRContent metsContent = getMetsSource(job, useExistingMets(request), derivate);
            imageNumber = getOrderNumber(metsContent.asXML(), filePath);
        }
        if (imageNumber == -1) {
            response.sendError(HttpServletResponse.SC_CONFLICT,
                    MessageFormat.format("Image {0} not found in the MCRDerivate. Please contact an administrator.", filePath));
            return;
        }

        String dfgURL = "http://dfg-viewer.de/show/?set[mets]=" + encodedMetsURL + "&set[image]=" + imageNumber;
        response.sendRedirect(dfgURL);
    }

    private static int getOrderNumber(Document metsDoc, String fileHref) {
        int orderNumber = -1;
        String fileID = null;

        try {
            Mets mets = new Mets(metsDoc);
            List<FileGrp> fileGroups = mets.getFileSec().getFileGroups();
            for (FileGrp fileGrp : fileGroups) {
                List<org.mycore.mets.model.files.File> fileList = fileGrp.getFileList();
                for (org.mycore.mets.model.files.File file : fileList) {
                    FLocat fLocat = file.getFLocat();
                    if (fLocat.getHref().equals(fileHref))
                        fileID = file.getId();
                }
            }

            if (fileID != null) {
                PhysicalStructMap structMap = (PhysicalStructMap) mets.getStructMap(PhysicalStructMap.TYPE);
                PhysicalDiv rootDiv = structMap.getDivContainer();
                List<PhysicalSubDiv> children = rootDiv.getChildren();
                for (PhysicalSubDiv physicalSubDiv : children) {
                    List<Fptr> fptrList = physicalSubDiv.getChildren();
                    for (Fptr fptr : fptrList) {
                        if (fptr.getFileId().equals(fileID))
                            orderNumber = physicalSubDiv.getOrder();
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
    static MCRContent getMetsSource(MCRServletJob job, boolean useExistingMets, String derivate) throws Exception {
        MCRDirectory dir = MCRDirectory.getRootDirectory(derivate);

        MCRFilesystemNode metsFile = dir.getChildByPath("mets.xml");

        try {
            job.getRequest().setAttribute("XSL.derivateID", derivate);
            job.getRequest().setAttribute("XSL.objectID", MCRLinkTableManager.instance().getSourceOf(derivate).iterator().next());
        } catch (Exception x) {
            LOGGER.warn("Unable to set \"XSL.objectID\" attribute to current request", x);
        }

        if (metsFile != null && useExistingMets) {
            MCRContent content = ((MCRFile) metsFile).getContent();
            content.setDocType("mets");
            return content;
        } else {
            HashSet<MCRFilesystemNode> ignoreNodes = new HashSet<MCRFilesystemNode>();
            if (metsFile != null)
                ignoreNodes.add(metsFile);
            Document mets = MCRMETSGenerator.getGenerator().getMETS(dir, ignoreNodes).asDocument();

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
