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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRXMLContainer;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRXMLTableManager;

/**
 * This servlet response the MCRObject certain by the call path
 * <em>.../receive/MCRObjectID</em> or
 * <em>.../servlets/MCRObjectServlet/id=MCRObjectID[&XSL.Style=...]</em>.
 * 
 * @author Jens Kupferschmidt
 * @author Anja Schaar
 * 
 * @see org.mycore.frontend.servlets.MCRServlet
 */
public class MCRObjectServlet extends MCRServlet {
    private static Logger LOGGER = Logger.getLogger(MCRObjectServlet.class);

    private static MCRConfiguration CONFIG = null;

    private static MCRXMLTableManager TM = null;

    /**
     * The initalization of the servlet.
     * 
     * @see javax.servlet.GenericServlet#init()
     */
    public void init() throws ServletException {
        super.init();
        CONFIG = MCRConfiguration.instance();
        TM = MCRXMLTableManager.instance();
    }

    /**
     * The method replace the default form MCRServlet and redirect the
     * MCRLayoutServlet.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void doGetPost(MCRServletJob job) throws ServletException, Exception {
        // the urn with information about the MCRObjectID
        String uri = job.getRequest().getPathInfo();
        String id = "";

        if (uri != null) {
            LOGGER.debug(this.getClass() + " Path = " + uri);

            int j = uri.length();
            LOGGER.debug(this.getClass() + " " + uri.substring(1, j));
            id = uri.substring(1, j);
        } else {
            id = getProperty(job.getRequest(), "id");
        }

        // check the ID and retrive the data
        MCRObjectID mcrid = null;
        byte[] xml;

        try {
            mcrid = new MCRObjectID(id);

            xml = TM.retrieve(mcrid);
            if (xml == null){
                LOGGER.warn("Could not load MCRObject with ID: "+id);
                generateErrorPage(job.getRequest(),job.getResponse(),HttpServletResponse.SC_NOT_FOUND,"MCRObject with ID "+id+ " is not known.",null,false);
                return;
            }
        } catch (MCRException e) {
            LOGGER.warn(this.getClass() + " The ID " + id + " is not a MCRObjectID!");
            generateErrorPage(job.getRequest(),job.getResponse(),HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Error while retrieving MCRObject with ID: "+id,e,false);

            return;
        }

        // call the LayoutServlet
        job.getRequest().setAttribute("MCRLayoutServlet.Input.BYTES", xml);

        RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
        rd.forward(job.getRequest(), job.getResponse());
    }
}
