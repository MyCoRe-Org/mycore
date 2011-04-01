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

import javax.servlet.http.HttpServletResponse;


import org.apache.log4j.Logger;
import org.jdom.Document;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.mets.model.Mets;
import org.mycore.mets.tools.MCRMetsProvider;
import org.mycore.mets.tools.MCRMetsSave;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author Silvio Hermann (shermann)
 */
public class MCRSaveMETSServlet extends MCRServlet {

    private static final Logger LOGGER = Logger.getLogger(MCRSaveMETSServlet.class);

    private static final long serialVersionUID = 1L;

    public void doGetPost(MCRServletJob job) throws Exception {
        String jsontree = job.getRequest().getParameter("jsontree");
        JsonObject json = new JsonParser().parse(jsontree).getAsJsonObject();
        // extract derivate id from json object (root id of the tree)
        String derivateId = job.getRequest().getParameter("derivate");

        // checking access right
        if (!MCRAccessManager.checkPermission(MCRObjectID.getInstance(derivateId), "writedb")) {
            LOGGER.warn("Creating Mets object for derivate with id " + derivateId + " failed. Unsufficient privileges.");
            job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        LOGGER.debug(jsontree);
        
        MCRMetsProvider mp = new MCRMetsProvider(derivateId);
        LOGGER.info("Creating Mets object for derivate with id " + derivateId);
        Mets mets = mp.toMets(json);
        LOGGER.info("Creating Mets object for derivate with id " + derivateId + " was succesful");

        LOGGER.info("Creating METS document from Mets object");
        Document metsDoc = mets.asDocument();
        LOGGER.info("Creating METS document from Mets object was succesful");

        MCRMetsSave.saveMets(metsDoc, derivateId);
        return;
    }
}
