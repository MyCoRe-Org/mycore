/*
 * $Revision$ $Date$
 * $LastChangedBy$ Copyright 2010 - Thüringer Universitäts- und
 * Landesbibliothek Jena
 * 
 * Mets-Editor is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Mets-Editor is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Mets-Editor. If not, see http://www.gnu.org/licenses/.
 */
package org.mycore.mets.servlets;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.HashSet;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.mets.model.MCRMETSGenerator;
import org.mycore.mets.tools.MCRJSONProvider;

/**
 * This servlet provides the data in json format for the dijit tree at the
 * client side. If there is no mets file the JSON created is basically a list of
 * files. If there is a mets file available, the mets xml data will be
 * transformed into JSON.
 * 
 * @author Silvio Hermann (shermann)
 */
public class MCRJSONProviderServlet extends MCRServlet {

    private static final Logger LOGGER = Logger.getLogger(MCRJSONProviderServlet.class);

    private static final long serialVersionUID = 1L;

    public void doGetPost(MCRServletJob job) throws Exception {
        String derivate = job.getRequest().getParameter("derivate");

        if (derivate == null || derivate.isEmpty()) {
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter \"derivate\" isn't set");
            return;
        }

        String useMets = job.getRequest().getParameter("useExistingMets");
        boolean useExistingMets = useMets != null ? useExistingMets = Boolean.valueOf(useMets) : true;

        MCRContent metsSource = MCRMETSServlet.getMetsSource(job, useExistingMets, derivate);
        Document metsDocument = metsSource.asXML();

        long start = System.currentTimeMillis();
        String json = null;

        if (metsDocument == null || !useExistingMets) {
            metsDocument = getBaseMetsXML(derivate);
        }

        LOGGER.info("Creating JSON for derivate with id \"" + derivate + "\"");

        MCRJSONProvider provider = new MCRJSONProvider(metsDocument, derivate);
        json = provider.getJson();

        LOGGER.debug("Generation of JSON (" + getClass().getSimpleName() + ") took "
            + (System.currentTimeMillis() - start) + " ms");
        HttpServletResponse response = job.getResponse();
        response.setContentType("application/x-json");
        response.setCharacterEncoding(MCRConfiguration.instance().getString("MCR.Request.CharEncoding", "UTF-8"));
        PrintWriter writer = response.getWriter();
        writer.print(json);
        writer.flush();
        writer.close();
        return;
    }

    /**
     * @param derivate
     *            the derivate id for which the document should be returned
     * @return Document the mets document on base of the derivate
     */
    private Document getBaseMetsXML(String derivate) throws Exception {
        MCRPath metsPath = MCRPath.getPath(derivate, (MCRJSONProvider.DEFAULT_METS_FILENAME));

        HashSet<MCRPath> ignoreNodes = new HashSet<>();
        if (Files.exists(metsPath)) {
            ignoreNodes.add(metsPath);
        }
        Document mets = MCRMETSGenerator.getGenerator().getMETS(MCRPath.getPath(derivate, "/"), ignoreNodes)
            .asDocument();

        return mets;
    }
}
