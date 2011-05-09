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

import java.util.HashSet;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
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
        String useExistingMetsParam = job.getRequest().getParameter("useExistingMets");
        boolean useExistingMets = true;
        useExistingMets = Boolean.valueOf(useExistingMetsParam);

        Document mets = getExistingMetsXML(derivate);
        long start = System.currentTimeMillis();
        String json = null;

        if (mets != null && useExistingMets) {
            LOGGER.info("Creating JSONObject from " + MCRJSONProvider.DEFAULT_METS_FILENAME + " for derivate with id \"" + derivate + "\"");
            MCRJSONProvider provider = new MCRJSONProvider(mets, derivate);
            json = provider.toJSON();
        } else {
            LOGGER.info("Creating initial JSONObject for derivate with id \"" + derivate + "\"");
            MCRJSONProvider provider = new MCRJSONProvider(getBaseMetsXML(derivate), derivate);
            json = provider.toJSON();
        }

        LOGGER.info("Generation of JSON (" + getClass().getSimpleName() + ") took " + (System.currentTimeMillis() - start) + " ms");
        HttpServletResponse response = job.getResponse();
        response.setContentType("application/x-json");
        response.setCharacterEncoding(MCRConfiguration.instance().getString("MCR.Request.CharEncoding", "UTF-8"));
        response.getWriter().print(json);
        return;
    }

    /**
     * @param derivate
     *            the derivate id for which the document should be returned
     * @return Document the mets document
     */
    private Document getExistingMetsXML(String derivate) throws Exception {
        MCRDirectory dir = MCRDirectory.getRootDirectory(derivate);
        MCRFilesystemNode file = dir.getChildByPath(MCRJSONProvider.DEFAULT_METS_FILENAME);

        if (file instanceof MCRFile) {
            MCRFile f = (MCRFile) file;
            try {
                Document mets = new SAXBuilder().build(f.getContentAsInputStream());
                return mets;
            } catch (Exception ex) {
                LOGGER.error("Error occured while loading mets from derivate \"" + derivate + "\"", ex);
            }
        }

        return null;
    }

    /**
     * @param derivate
     *            the derivate id for which the document should be returned
     * @return Document the mets document on base of the derivate
     */
    private Document getBaseMetsXML(String derivate) throws Exception {
        MCRDirectory dir = MCRDirectory.getRootDirectory(derivate);
        MCRFilesystemNode metsFile = dir.getChildByPath(MCRJSONProvider.DEFAULT_METS_FILENAME);

        HashSet<MCRFilesystemNode> ignoreNodes = new HashSet<MCRFilesystemNode>();
        if (metsFile != null) {
            ignoreNodes.add(metsFile);
        }
        Document mets = MCRMETSGenerator.getGenerator().getMETS(dir, ignoreNodes).asDocument();

        return mets;
    }
}