/*
 * $Revision$ 
 * $Date$
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

package org.mycore.frontend.xeditor.target;

import java.io.IOException;
import org.jaxen.JaxenException;
import java.util.Set;

import javax.servlet.ServletContext;

import org.jdom2.JDOMException;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.MCREditorSessionStore;
import org.mycore.frontend.xeditor.MCREditorStep;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public abstract class MCREditorTarget {

    public abstract void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session, String parameter)
            throws Exception;

    protected void setSubmittedValues(MCRServletJob job, MCREditorStep step) throws JDOMException, JaxenException {
        for (String xPath : (Set<String>) (job.getRequest().getParameterMap().keySet())) {
            if (xPath.startsWith("/")) {
                String[] values = job.getRequest().getParameterValues(xPath);
                step.setSubmittedValues(xPath, values);
            }
        }
        step.emptyNotResubmittedNodes();
    }

    protected void redirectToEditorPage(MCRServletJob job, MCREditorSession session) throws IOException {
        String url = job.getRequest().getHeader("referer");
        int index = url.indexOf("?");
        if (index > 0)
            url = url.substring(0, index);
        url += "?" + MCREditorSessionStore.XEDITOR_SESSION_PARAM + "=" + session.getID();
        job.getResponse().sendRedirect(url);
    }
}
