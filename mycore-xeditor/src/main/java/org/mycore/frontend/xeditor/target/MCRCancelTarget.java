/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.frontend.xeditor.target;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCREditorSession;

/**
 * @author Frank L\u00FCtzenkirchen
 * 
 * Implements the "cancel" target to redirect when cancel button is clicked.
 * The target URL is set by <code>&lt;xed:cancel url="..." /&gt;</code> 
 * 
 * When no URL is set, the web application base URL is used.
 * When a complete URL is given, it is used as is.
 * When a relative path is given, the URL is calculated relative to the page containing the editor form.
 * When an absolute path is given, the web application base URL is prepended. 
 */
public class MCRCancelTarget implements MCREditorTarget {

    @Override
    public void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session, String parameter)
        throws IOException,
        ServletException {
        String cancelURL = session.getCancelURL();

        if (cancelURL == null)
            cancelURL = MCRFrontendUtil.getBaseURL();
        else if (cancelURL.startsWith("/"))
            cancelURL = MCRFrontendUtil.getBaseURL() + cancelURL.substring(1);
        else if (!(cancelURL.startsWith("http:") || cancelURL.startsWith("https:"))) {
            String pageURL = session.getPageURL();
            cancelURL = pageURL.substring(0, pageURL.lastIndexOf('/') + 1) + cancelURL;
        }

        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(cancelURL));
    }
}
