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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;

import org.jdom2.Document;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.tracker.MCRChangeTracker;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRServletTarget implements MCREditorTarget {

    @Override
    public void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session,
        String servletNameOrPath)
        throws Exception {
        session.getSubmission().setSubmittedValues(job.getRequest().getParameterMap());
        Document result = session.getEditedXML();

        if (session.getValidator().isValid()) {
            result = MCRChangeTracker.removeChangeTracking(result);
            result = session.getXMLCleaner().clean(result);
            result = session.getPostProcessor().process(result);

            RequestDispatcher dispatcher = context.getNamedDispatcher(servletNameOrPath);
            if (dispatcher == null)
                dispatcher = context.getRequestDispatcher(servletNameOrPath);

            job.getRequest().setAttribute("MCRXEditorSubmission", result);

            session.setBreakpoint("After handling target servlet " + servletNameOrPath);

            dispatcher.forward(job.getRequest(), job.getResponse());
        } else {
            session.setBreakpoint("After validation failed, target servlet " + servletNameOrPath);
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(session.getRedirectURL(null)));
        }
    }
}
