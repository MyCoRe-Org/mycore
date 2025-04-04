/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRPathContent;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.mets.model.MCRMETSGeneratorFactory;
import org.mycore.mets.tools.MCRMetsSave;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRMETSServlet extends MCRServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger();

    public static final boolean STORE_METS_ON_GENERATE = MCRConfiguration2
        .getOrThrow("MCR.Mets.storeMetsOnGenerate", Boolean::parseBoolean);

    private boolean useExpire;

    private static int cacheTime;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        LOGGER.info(request::getPathInfo);

        String derivate = getOwnerID(request.getPathInfo());
        MCRPath rootPath = MCRPath.getPath(derivate, "/");

        if (!Files.isDirectory(rootPath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                String.format(Locale.ENGLISH, "Derivate %s does not exist.", derivate));
            return;
        }
        request.setAttribute("XSL.derivateID", derivate);
        Collection<String> linkList = MCRLinkTableManager.getInstance().getSourceOf(derivate);
        if (linkList.isEmpty()) {
            MCRDerivate derivate2 = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(derivate));
            MCRObjectID ownerID = derivate2.getOwnerID();
            if (ownerID != null && ownerID.toString().length() != 0) {
                linkList.add(ownerID.toString());
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    String.format(Locale.ENGLISH,
                        "Derivate %s is not linked with a MCRObject. Please contact an administrator.", derivate));
                return;
            }
        }
        request.setAttribute("XSL.objectID", linkList.iterator().next());
        response.setContentType("text/xml");

        long lastModified = Files.getLastModifiedTime(rootPath).toMillis();

        MCRFrontendUtil.writeCacheHeaders(response, cacheTime, lastModified, useExpire);
        long start = System.currentTimeMillis();
        MCRContent metsContent = getMetsSource(job, useExistingMets(request), derivate);
        MCRLayoutService.obtainInstance().doLayout(request, response, metsContent);
        LOGGER.info("Generation of code by {} took {} ms",
            () -> this.getClass().getSimpleName(), () -> System.currentTimeMillis() - start);
    }

    /**
     * Returns the mets document wrapped in a {@link MCRContent} object.
     */
    static MCRContent getMetsSource(MCRServletJob job, boolean useExistingMets, String derivate) {
        MCRPath metsPath = MCRPath.getPath(derivate, "/mets.xml");

        try {
            job.getRequest().setAttribute("XSL.derivateID", derivate);
            String objectid = MCRLinkTableManager.getInstance().getSourceOf(derivate).iterator().next();

            if (objectid == null || objectid.length() == 0) {
                MCRDerivate derObj = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(derivate));
                MCRObjectID ownerID = derObj.getOwnerID();
                objectid = ownerID.toString();
            }

            job.getRequest().setAttribute("XSL.objectID", objectid);
        } catch (Exception x) {
            LOGGER.warn("Unable to set \"XSL.objectID\" attribute to current request", x);
        }

        boolean metsExists = Files.exists(metsPath);
        if (metsExists && useExistingMets) {
            MCRContent content = new MCRPathContent(metsPath);
            content.setDocType("mets");
            return content;
        } else {
            Document mets = MCRMETSGeneratorFactory.create(MCRPath.getPath(derivate, "/")).generate().asDocument();
            if (!metsExists && STORE_METS_ON_GENERATE) {
                MCRMetsSave.saveMets(mets, MCRObjectID.getInstance(derivate));
            }
            return new MCRJDOMContent(mets);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mycore.frontend.servlets.MCRServlet#init()
     */
    @Override
    public void init() throws ServletException {
        super.init();
        String cacheParam = getInitParameter("cacheTime");
        /* default is one day */
        cacheTime = cacheParam != null ? Integer.parseInt(cacheParam) : (60 * 60 * 24);
        useExpire = MCRConfiguration2.getBoolean("MCR.Component.MetsMods.Servlet.UseExpire").orElse(true);
    }

    private boolean useExistingMets(HttpServletRequest request) {
        String useExistingMetsParam = request.getParameter("useExistingMets");
        return useExistingMetsParam == null
            || Boolean.parseBoolean(useExistingMetsParam);
    }

    public static String getOwnerID(String pathInfo) {
        StringBuilder ownerID = new StringBuilder(pathInfo.length());
        boolean running = true;
        for (int i = (pathInfo.charAt(0) == '/') ? 1 : 0; (i < pathInfo.length() && running); i++) {
            if (pathInfo.charAt(i) == '/') {
                running = false;
            } else {
                ownerID.append(pathInfo.charAt(i));
            }
        }
        return ownerID.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.mycore.frontend.servlets.MCRServlet#getLastModified(jakarta.servlet
     * .http.HttpServletRequest)
     */
    @Override
    protected long getLastModified(HttpServletRequest request) {
        String ownerID = getOwnerID(request.getPathInfo());
        MCRSessionMgr.unlock();
        MCRSession session = MCRSessionMgr.getCurrentSession();
        MCRPath metsPath = MCRPath.getPath(ownerID, "/mets.xml");
        try {
            MCRTransactionManager.beginTransactions();
            try {
                if (Files.exists(metsPath)) {
                    return Files.getLastModifiedTime(metsPath).toMillis();
                } else if (Files.isDirectory(metsPath.getParent())) {
                    return Files.getLastModifiedTime(metsPath.getParent()).toMillis();
                }
            } catch (IOException e) {
                LOGGER.warn("Error while retrieving last modified information from {}", metsPath, e);
            }
            return -1L;
        } finally {
            MCRTransactionManager.commitTransactions();
            MCRSessionMgr.releaseCurrentSession();
            session.close(); // just created session for db transaction
        }
    }

}
