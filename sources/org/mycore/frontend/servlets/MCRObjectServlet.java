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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
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
    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRObjectServlet.class);

    private static MCRXMLTableManager TM = null;

    /**
     * The initalization of the servlet.
     * 
     * @see javax.servlet.GenericServlet#init()
     */
    public void init() throws ServletException {
        super.init();
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
        String id = getObjectID(job.getRequest());

        try {
            MCRObjectID mcrid = new MCRObjectID(id);

            if (!MCRAccessManager.checkPermission(mcrid, "read")) {
                StringBuffer msg = new StringBuffer(1024);
                msg.append("Access denied reading MCRObject with ID: ").append(mcrid.getId());
                msg.append(".\nCurrent User: ").append(MCRSessionMgr.getCurrentSession().getCurrentUserName());
                msg.append("\nRemote IP: ").append(MCRSessionMgr.getCurrentSession().getCurrentIP());
                generateErrorPage(job.getRequest(),job.getResponse(),HttpServletResponse.SC_FORBIDDEN,msg.toString(),null,false);
                return;
            }

            byte[] xml = TM.retrieve(mcrid);
            if (xml == null) {
                LOGGER.warn("Could not load MCRObject with ID: " + id);
                generateErrorPage(job.getRequest(), job.getResponse(), HttpServletResponse.SC_NOT_FOUND, "MCRObject with ID " + id + " is not known.", null,
                        false);
                return;
            }
            // call the LayoutServlet
            job.getRequest().setAttribute("MCRLayoutServlet.Input.BYTES", xml);
            RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
            rd.forward(job.getRequest(), job.getResponse());
        } catch (MCRException e) {
            LOGGER.warn(this.getClass() + " The ID " + id + " is not a MCRObjectID!");
            generateErrorPage(job.getRequest(), job.getResponse(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while retrieving MCRObject with ID: "
                    + id, e, false);
            return;
        }
    }

    /**
     * @param job
     * @return requested MCRObjectID
     */
    private static final String getObjectID(HttpServletRequest request) {
        // the urn with information about the MCRObjectID
        String uri = request.getPathInfo();

        if (uri != null) {
            int j = uri.length();
            LOGGER.debug("Path = " + uri + "-->" + uri.substring(1, j));
            return uri.substring(1, j);
        } else {
            return getProperty(request, "id");
        }
    }
}