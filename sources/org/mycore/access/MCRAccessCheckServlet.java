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

package org.mycore.access;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * Servlet for access check URL:
 * /servlets/MCRAccessCheckservlet?pool=XXX&objid=YYY if no parameter where
 * given, return value will always be true
 * 
 * @param pool:
 *            accesspool to check
 * @param objid:
 *            MyCoRe objectid as string to check
 * 
 * @return xml: <?xml version="1.0" encoding="UTF-8" ?> <mycoreaccesscheck>
 *         <accesscheck return="false" disabled="false" /> </mycoreaccesscheck>
 *  - attribute disabled contains the AccessManagerState (disabled
 * true/false) - attribute return contains the access check result
 * (false=locked)
 * 
 * @author Arne Seifert
 * 
 */
public class MCRAccessCheckServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    /**
     * access check for given pool and objectid
     */
    public void doGetPost(MCRServletJob job) throws IOException, ServletException {
        // get parameter
        String objid = getProperty(job.getRequest(), "objid");
        String pool = getProperty(job.getRequest(), "pool");

        boolean result = MCRAccessManager.checkAccess(pool, objid, MCRSessionMgr.getCurrentSession());

        Document jdom = new Document(new Element("mycoreaccesscheck"));
        jdom.getRootElement().addContent(new Element("accesscheck"));
        jdom.getRootElement().getChild("accesscheck").setAttribute(new Attribute("return", String.valueOf(result)));
        jdom.getRootElement().getChild("accesscheck").setAttribute(new Attribute("disabled", String.valueOf(new MCRAccessManager().isDisabled())));

        job.getRequest().setAttribute("MCRLayoutServlet.Input.JDOM", jdom);

        RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
        rd.forward(job.getRequest(), job.getResponse());
    }
}
