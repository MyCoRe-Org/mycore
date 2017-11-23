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

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCREditorSession;

public class MCRAjaxSubselectTarget implements MCREditorTarget {

    @Override
    public void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session, String parameter)
        throws Exception {
        int pos = parameter.lastIndexOf(":");
        String xPath = parameter.substring(0, pos);
        Map<String, String[]> submittedValues = MCRTargetUtility.getSubmittedValues(job, xPath);
        session.getSubmission().setSubmittedValues(submittedValues);

        job.getResponse().setStatus(HttpServletResponse.SC_OK);
        job.getResponse().getOutputStream().print(session.getCombinedSessionStepID());
        job.getResponse().getOutputStream().flush();
        job.getResponse().getOutputStream().close();
    }
}
