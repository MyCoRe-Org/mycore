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

import javax.servlet.ServletContext;

import org.jdom2.Document;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.tracker.MCRChangeTracker;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRLayoutServiceTarget implements MCREditorTarget {

    @Override
    public void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session, String style)
        throws Exception {
        session.getSubmission().setSubmittedValues(job.getRequest().getParameterMap());
        Document result = session.getEditedXML();

        if (session.getValidator().isValid()) {
            result = MCRChangeTracker.removeChangeTracking(result);
            result = session.getXMLCleaner().clean(result);
            result = session.getPostProcessor().process(result);

            if ((style != null) && (!style.isEmpty()))
                job.getRequest().setAttribute("XSL.Style", style);

            MCRContent editedXML = new MCRJDOMContent(result);
            MCRLayoutService.instance().doLayout(job.getRequest(), job.getResponse(), editedXML);
            session.setBreakpoint("After handling target layout " + style);
        } else {
            session.setBreakpoint("After validation failed, target layout " + style);
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(session.getRedirectURL(null)));
        }
    }
}
