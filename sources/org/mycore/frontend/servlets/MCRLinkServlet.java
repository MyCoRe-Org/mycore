/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRLinkTableManager;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRResults;

/**
 * This servlet return the corresponding link value (from / to) for the
 * requested link value (from / to) of the MCRLinkTablemanager data. The return
 * is a simplified mcr:results JDOM tree.
 * 
 * @author Jens Kupferschmidt
 * 
 * @see org.mycore.frontend.servlets.MCRServlet
 */
public class MCRLinkServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRLinkServlet.class);

    private static MCRLinkTableManager LM = null;

    /**
     * The initalization of the servlet.
     * 
     * @see javax.servlet.GenericServlet#init()
     */
    public void init() throws ServletException {
        super.init();
        LM = MCRLinkTableManager.instance();
    }

    /**
     * The method replace the default form MCRServlet and redirect the
     * MCRLayoutService. Parameters are:
     * <ul>
     * <li>from or to</li>
     * <li>host</li>
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void doGetPost(MCRServletJob job) throws Exception {
        String from = "";
        String to = "";
        String type = "";
        String host = "";
        try {
            host = job.getRequest().getParameter("host");
            if ((host == null) || (host.length() == 0)) {
                host = MCRHit.LOCAL;
            }
            type = job.getRequest().getParameter("type");
            if ((type == null) || (type.length() == 0)) {
                type = MCRLinkTableManager.ENTRY_TYPE_REFERENCE;
            }
            from = job.getRequest().getParameter("from");
            if (from == null) from = "";
            to = job.getRequest().getParameter("to");
            if (to == null) to = "";
            if ((from.length() == 0) && (to.length() == 0)) {
                return; // request failed;
            }
            LOGGER.debug("Input parameter : type="+type+"   from="+from+"   to="+to+"   host="+host);

            if(host.equals(MCRHit.LOCAL)) {
                MCRResults results = new MCRResults();
                List links = new ArrayList();
                // Look for links
                if ((from != null) && (from.length() != 0)) {
                    links = LM.getDestinationOf(from, type);
                } else  {
                    links = LM.getSourceOf(to, type);
                }
                for (int i=0;i<links.size();i++) {
                    MCRHit hit = new MCRHit((String)links.get(i));
                    results.addHit(hit);
                }
                // build XML
                Element xml = results.buildXML();
                // Send output to LayoutServlet
                sendToLayout(job.getRequest(), job.getResponse(), new Document(xml));                    
            } else {
                LOGGER.warn("Remote host access is not supported, use the WebService access!");
            }
        } catch (MCRException e) {
            generateErrorPage(job.getRequest(), job.getResponse(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while retrieving link for ID: "
                    + from + " - "+ to + " - "+ type, e, false);
            return;
        }
    }
    
    /**
     * Forwards the document to the output
     * 
     * @author A.Schaar
     * @see its overwritten in jspdocportal
     */
    protected void sendToLayout(HttpServletRequest req, HttpServletResponse res, Document jdom) throws IOException {
        getLayoutService().doLayout(req, res, jdom);
    }

}
