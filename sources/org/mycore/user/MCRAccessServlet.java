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

package org.mycore.user;

import javax.servlet.RequestDispatcher;

import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet returns a XML Object that contains the access check result.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRAccessServlet extends MCRServlet {
	private static final long serialVersionUID = 1L;
	private static MCRAccessInterface AI = MCRAccessManager.getAccessImpl();

    /**
     * This method overrides doGetPost of MCRServlet. <br />
     * The method looks for the parameters permission and objid.  <br />
     * and checks, whether a user has the permission to do something general
     * something special with an object
     * <br />
     * &lt;mycoreaccess&gt; <br />
     * &lt;access return="true"&gt; <br />
     * &lt;/mycoreaccess&gt; <br />
     */
    public void doGetPost(MCRServletJob job) throws Exception {
    	boolean accessAllowed = false;
        // read the parameters
        // permission parameter 'read' or 'writedb' or 'deletedb'
        String permission = getProperty(job.getRequest(), "permission");

        if (permission == null && permission.equals("")) {
            permission = "read";
        }

        permission.trim().toLowerCase();

        String objid = getProperty(job.getRequest(), "objid");
        if (objid == null || objid.equals("")) {
        	accessAllowed = AI.checkPermission(permission);
        }else {
        	accessAllowed = AI.checkPermission(objid, permission);
        }

        // prepare the document
        org.jdom.Element root = new org.jdom.Element("mycoreaccess");
        org.jdom.Document jdom = new org.jdom.Document(root);
        org.jdom.Element access = new org.jdom.Element("access");
        access.setAttribute("return", String.valueOf(accessAllowed));
        root.addContent(access);
        job.getRequest().setAttribute("MCRLayoutServlet.Input.JDOM", jdom);
        job.getRequest().setAttribute("XSL.Style", "xml");

        RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
        rd.forward(job.getRequest(), job.getResponse());
    }
}
