/*
 * 
 * $Revision: 1.1 $ $Date: 2008/04/11 09:09:38 $
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

package org.mycore.user.servlets;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.ifs2.MCRContent;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.user.MCRUserMgr;

/**
 * This servlet is used to login a user to the mycore system.
 * 
 * @author Detlev Degenhardt
 * @version $Revision: 1.1 $ $Date: 2008/04/11 09:09:38 $
 */
public class MCRLoginServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    // The configuration
    private static Logger LOGGER = Logger.getLogger(MCRLoginServlet.class);

    /** This method overrides doGetPost of MCRServlet. */
    public void doGetPost(MCRServletJob job) throws Exception {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();

        String uid = getProperty(job.getRequest(), "uid");
        String pwd = getProperty(job.getRequest(), "pwd");
        String backto_url = getProperty(job.getRequest(), "url");

        if (backto_url == null) {
            String referer = job.getRequest().getHeader("Referer");
            backto_url = (referer != null) ? referer : MCRServlet.getBaseURL();
        }
        LOGGER.debug("SessionID: " + mcrSession.getID());
        LOGGER.debug("CurrentID: " + mcrSession.getUserInformation().getUserID());
        LOGGER.debug("UID :      " + uid);
        LOGGER.debug("URL :      " + backto_url);

        // Do not change login, just redirect to given url:
        if (mcrSession.getUserInformation().getUserID().equals(uid)) {
            job.getResponse().setHeader("Cache-Control", "no-cache");
            job.getResponse().setHeader("Pragma", "no-cache");
            job.getResponse().setHeader("Expires", "0");
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(backto_url));
            return;
        }

        org.jdom.Element root = new org.jdom.Element("mcr_user");
        org.jdom.Document jdomDoc = new org.jdom.Document(root);

        if (uid != null) {
            if (!MCRUserMgr.instance().existUser(uid)) {
                root.setAttribute("unknown_user", "true");
            } else if (MCRUserMgr.instance().login(uid, pwd)) {
                //user logged in
                LOGGER.info("MCRLoginServlet: user " + uid + " logged in successfully.");
                job.getRequest().removeAttribute("mode");
                job.getRequest().setAttribute("mode", "Select");
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(backto_url));
                return;
            } else {
                //password is wrong
                root.setAttribute("invalid_password", "true");
            }
        }
        root.addContent(new org.jdom.Element("backto_url").addContent(backto_url));
        doLayout(job, "Login", jdomDoc); // use the stylesheet
    }

    /**
     * Gather information about the XML document to be shown and the
     * corresponding XSLT stylesheet and redirect the request to the
     * LayoutServlet
     * 
     * @param job
     *            The MCRServletJob instance
     * @param style
     *            String value to select the correct XSL stylesheet
     * @param jdomDoc
     *            The XML representation to be presented by the LayoutServlet
     * @throws ServletException
     *             for errors from the servlet engine.
     * @throws IOException
     *             for java I/O errors.
     */
    protected void doLayout(MCRServletJob job, String style, Document jdomDoc) throws IOException {
        job.getRequest().setAttribute("XSL.Style", style);
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), MCRContent.readFrom(jdomDoc));
    }

}
