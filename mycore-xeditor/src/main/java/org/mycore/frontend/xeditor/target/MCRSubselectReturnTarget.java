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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.tracker.MCRChangeData;

public class MCRSubselectReturnTarget implements MCREditorTarget {

    private static final Logger LOGGER = LogManager.getLogger(MCRSubselectReturnTarget.class);

    @Override
    public void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session, String parameter)
        throws Exception {
        String baseXPath = getBaseXPathForSubselect(session);
        LOGGER.info("Returning from subselect for {}", baseXPath);

        if ("cancel".equals(parameter)) {
            session.setBreakpoint("After canceling subselect for " + baseXPath);
        } else {
            Map<String, String[]> submittedValues = MCRTargetUtility.getSubmittedValues(job, baseXPath);
            session.getSubmission().setSubmittedValues(submittedValues);
            session.setBreakpoint("After returning from subselect for " + baseXPath);
        }

        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(session.getRedirectURL(null)));
    }

    private String getBaseXPathForSubselect(MCREditorSession session) throws JaxenException, JDOMException {
        Document doc = session.getEditedXML();
        MCRChangeData change = session.getChangeTracker().findLastChange(doc);
        String text = change.getText();
        String xPath = text.substring(text.lastIndexOf(" ") + 1).trim();
        return bindsFirstOrMoreThanOneElement(xPath, session) ? xPath + "[1]" : xPath;
    }

    private boolean bindsFirstOrMoreThanOneElement(String xPath, MCREditorSession session)
        throws JaxenException, JDOMException {
        MCRBinding binding = new MCRBinding(xPath, false, session.getRootBinding());
        boolean result = (binding.getBoundNode() instanceof Element) && !xPath.endsWith("]");
        binding.detach();
        return result;
    }
}
