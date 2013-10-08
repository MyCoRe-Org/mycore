package org.mycore.frontend.xeditor.target;

import javax.servlet.ServletContext;

import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.tracker.MCRChangeData;

public class MCRSubselectReturnTarget implements MCREditorTarget {

    @Override
    public void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session, String parameter) throws Exception {
        String baseXPath = getBaseXPathForSubselect(session);

        if ("cancel".equals(parameter)) {
            session.setBreakpoint("After canceling subselect for " + baseXPath);
        } else {
            setSubmittedValues(job, session, baseXPath);
            session.setBreakpoint("After returning from subselect for " + baseXPath);
        }
        
        job.getResponse().sendRedirect(session.getRedirectURL());
    }

    private String getBaseXPathForSubselect(MCREditorSession session) {
        Document doc = session.getEditedXML();
        MCRChangeData change = session.getChangeTracker().findLastChange(doc);
        String text = change.getText();
        return text.substring(text.lastIndexOf(" ") + 1).trim();
    }

    private void setSubmittedValues(MCRServletJob job, MCREditorSession session, String baseXPath) throws JDOMException, JaxenException {
        for (String paramName : job.getRequest().getParameterMap().keySet()) {
            if (paramName.startsWith("_xed_"))
                continue;

            String xPath = baseXPath + "/" + paramName;
            String[] values = job.getRequest().getParameterValues(paramName);
            session.getSubmission().setSubmittedValues(xPath, values);
        }
    }
}
