/* $Revision: 3033 $ 
 * $Date: 2010-10-22 13:41:12 +0200 (Fri, 22 Oct 2010) $ 
 * $LastChangedBy: thosch $
 * Copyright 2010 - Thüringer Universitäts- und Landesbibliothek Jena
 *  
 * Mets-Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mets-Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mets-Editor.  If not, see http://www.gnu.org/licenses/.
 */
package org.mycore.mets.servlets;

import java.util.Arrays;
import java.util.Comparator;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.mets.tools.JSONProvider;

/**
 * This servlet provides the data in json format for the dijit tree at the
 * client side. If there is no mets file the JSON created is basically a list of
 * files. If there is a mets file available, the mets xml data will be
 * transformed into JSON.
 * 
 * @author Silvio Hermann (shermann)
 */
public class MCRFilelistProviderServlet extends MCRServlet implements Comparator<MCRFilesystemNode> {

    private static final String DEFAULT_METS_FILENAME = MCRConfiguration.instance().getString("MCR.Mets.Filename");

    private static final Logger LOGGER = Logger.getLogger(MCRFilelistProviderServlet.class);

    private static final long serialVersionUID = 1L;

    public void doGetPost(MCRServletJob job) throws Exception {
        String derivate = job.getRequest().getParameter("derivate");
        String useExistingMetsParam = job.getRequest().getParameter("useExistingMets");
        boolean useExistingMets = true;
        useExistingMets = Boolean.valueOf(useExistingMetsParam);

        String json = null;
        Document mets = getMetsXML(derivate);
        if (mets != null && useExistingMets) {
            LOGGER.info("Creating JSONObject from " + MCRFilelistProviderServlet.DEFAULT_METS_FILENAME + " for derivate with id \"" + derivate + "\"");
            JSONProvider provider = new JSONProvider(mets, derivate);
            json = provider.toJSON();
        } else {
            LOGGER.info("Creating initial JSONObject for derivate with id \"" + derivate + "\"");
            json = createInitialJSON(derivate);
        }
        HttpServletResponse response = job.getResponse();
        response.setContentType("application/x-json");
        response.setCharacterEncoding(MCRConfiguration.instance().getString("MCR.Request.CharEncoding", "UTF-8"));
        response.getWriter().print(json);
    }

    /**
     * @param 
     *      derivate the derivate id for which the document should be returned
     * @return 
     *      Document the mets document
     */
    private Document getMetsXML(String derivate) {
        MCRFilesystemNode parent = MCRFilesystemNode.getRootNode(derivate);
        MCRFilesystemNode file = ((MCRDirectory) parent).getChild(MCRFilelistProviderServlet.DEFAULT_METS_FILENAME);

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

    /**Creates a JSON Object for the dojo tree at the client side*/
    private String createInitialJSON(String derivate) {
        MCRFilesystemNode node = MCRFilesystemNode.getRootNode(derivate);
        MCRFilesystemNode[] nodes = ((MCRDirectory) node).getChildren();
        StringBuilder builder = new StringBuilder();

        Arrays.sort(nodes, this);

        builder.append("{identifier: 'id',label: 'name',items: [\n");
        builder.append("{id: '" + derivate + "', name:'" + derivate + "', structureType:'monograph', children:[\n");
        String metsFName = null;

        try {
            metsFName = DEFAULT_METS_FILENAME;
        } catch (MCRConfigurationException ex) {
            LOGGER.warn(ex.getMessage());
            metsFName = "mets.xml";
            LOGGER.warn("Using default file  \"" + metsFName + "\" as mets file");
        }

        for (int i = 0; i < nodes.length; i++) {
            String name = nodes[i].getName();
            /* ignore the mets file that may be available */
            if (!name.endsWith(metsFName)) {
                builder.append("\t{ id: '");
                builder.append(name);
                builder.append("', name:'");
                builder.append(name);
                builder.append("', orderLabel:'");
                builder.append("',type:'item'}");
                if (i != nodes.length - 1) {
                    builder.append(",\n");
                }
            }
        }
        builder.append("]}\n]}");
        return builder.toString();
    }

    public int compare(MCRFilesystemNode o1, MCRFilesystemNode o2) {
        if (o1.getName().compareTo(o2.getName()) < 0) {
            return -1;
        }
        if (o1.getName().compareTo(o2.getName()) > 0) {
            return 1;
        }
        if (o1.getName().compareTo(o2.getName()) == 0) {
            return 0;
        }
        return 0;
    }
}