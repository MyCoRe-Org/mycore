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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletException;

import org.jdom.Document;
import org.mycore.common.MCRSessionMgr;

/**
 * This servlet provides some common methods for the editors of the user
 * management of the mycore system.
 * 
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRUserAdminGUICommons extends MCRServlet {
    protected String pageDir = null;

    protected String noPrivsPage = null;

    protected String cancelPage = null;

    protected String okPage = null;

    /** Initialisation of the servlet */
    public void init() throws ServletException {
        super.init();
        pageDir = CONFIG.getString("MCR.useradmin_page_dir", "");
        noPrivsPage = pageDir + CONFIG.getString("MCR.useradmin_page_error_privileges", "useradmin_error_privileges.xml");
        cancelPage = pageDir + CONFIG.getString("MCR.useradmin_page_cancel", "useradmin_cancel.xml");
        okPage = pageDir + CONFIG.getString("MCR.useradmin_page_ok", "useradmin_ok.xml");
    }

    /**
     * This method builds a URL that can be used to redirect the client browser
     * to another page, thereby including http request parameters. The request
     * parameters will be encoded as http get request.
     * 
     * @param baseURL
     *            the base url of the target webpage
     * @param parameters
     *            the http request parameters
     */
    protected String buildRedirectURL(String baseURL, Properties parameters) {
        StringBuffer redirectURL = new StringBuffer(baseURL);
        boolean first = true;

        for (Enumeration e = parameters.keys(); e.hasMoreElements();) {
            if (first) {
                redirectURL.append("?");
                first = false;
            } else {
                redirectURL.append("&");
            }

            String name = (String) (e.nextElement());
            String value = null;

            try {
                value = URLEncoder.encode(parameters.getProperty(name), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                value = parameters.getProperty(name);
            }

            redirectURL.append(name).append("=").append(value);
        }

        return redirectURL.toString();
    }

    /**
     * This method simply redirects to a page providing information that the
     * privileges for a use case are not sufficient.
     * 
     * @param job
     *            The MCRServletJob instance
     */
    protected void showNoPrivsPage(MCRServletJob job) throws IOException {
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + noPrivsPage));

        return;
    }

    /**
     * This method simply redirects to a page providing information that the
     * current use case was fulfilled successfully.
     * 
     * @param job
     *            The MCRServletJob instance
     */
    protected void showOkPage(MCRServletJob job) throws IOException {
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + okPage));

        return;
    }

    /**
     * Gather information about the XML document to be shown and the
     * corresponding XSLT stylesheet and redirect the request to the
     * LayoutService
     * 
     * @param job
     *            The MCRServletJob instance
     * @param styleSheet
     *            String value to select the correct XSL stylesheet
     * @param jdomDoc
     *            The XML representation to be presented by the LayoutService
     * @param useStrict
     *            If true, the parameter styleSheet must be used directly as
     *            name of a stylesheet when forwarding to the MCRLayoutService.
     *            If false, styleSheet will be appended by the signature of the
     *            current language. useStrict=true is used when not using a
     *            stylesheet at all because one simply needs the raw XML output.
     * 
     * @throws ServletException
     *             for errors from the servlet engine.
     * @throws IOException
     *             for java I/O errors.
     */
    protected void doLayout(MCRServletJob job, String styleSheet, Document jdomDoc, boolean useStrict) throws IOException {
        String language = MCRSessionMgr.getCurrentSession().getCurrentLanguage();

        if (!useStrict) {
            styleSheet = styleSheet + "-" + language;
        }

        job.getRequest().getSession().setAttribute("mycore.language", language);
        job.getRequest().setAttribute("XSL.Style", styleSheet);
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), jdomDoc);
    }
}
