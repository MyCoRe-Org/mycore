/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.mets.servlets;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

        if (Objects.equals(derivateID, "")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Derivate is not set");
        }

        String encodedMetsURL = URLEncoder.encode(MCRServlet.getServletBaseURL() + "MCRMETSServlet/" + derivateID
            + "?XSL.Style=dfg", StandardCharsets.UTF_8);
        LOGGER.info(request.getPathInfo());

        MCRPath rootPath = MCRPath.getPath(derivateID, "/");

        if (!Files.isDirectory(rootPath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                String.format(Locale.ENGLISH, "Derivate %s does not exist.", derivateID));
            return;
        }
        request.setAttribute("XSL.derivateID", derivateID);
        Collection<String> linkList = MCRLinkTableManager.instance().getSourceOf(derivateID);
        if (linkList.isEmpty()) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format(Locale.ENGLISH,
                "Derivate %s is not linked with a MCRObject. Please contact an administrator.", derivateID));
            return;
        }

        if (filePath.isEmpty()) {
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

        switch (imageNumber) {
            case -1 -> response.sendError(HttpServletResponse.SC_CONFLICT, String.format(Locale.ENGLISH,
                    "Image \"%s\" not found in the MCRDerivate. Please contact an administrator.", filePath));
            case -2 -> response.sendRedirect("https://dfg-viewer.de/show/?tx_dlf[id]=" + encodedMetsURL);
            default -> response.sendRedirect(
                "https://dfg-viewer.de/show/?tx_dlf[id]=" + encodedMetsURL + "&set[image]=" + imageNumber);
        }
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
                    if (fLocat.getHref().equals(MCRXMLFunctions.encodeURIPath(fileHref, true))) {
                        fileID = file.getId();
                    }
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
                        if (fptr.getFileId().equals(fileID)) {
                            orderNumber = index + 1;
                        }
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
     */
    private static MCRContent getMetsSource(MCRServletJob job, boolean useExistingMets, String derivate) {

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
        if (useExistingMetsParam == null) {
            return true;
        }
        return Boolean.parseBoolean(useExistingMetsParam);
    }

}
