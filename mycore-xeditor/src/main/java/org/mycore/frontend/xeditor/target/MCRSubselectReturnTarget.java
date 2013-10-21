package org.mycore.frontend.xeditor.target;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.tracker.MCRChangeData;

public class MCRSubselectReturnTarget implements MCREditorTarget {

    private final static Logger LOGGER = Logger.getLogger(MCRSubselectReturnTarget.class);

    @Override
    public void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session, String parameter) throws Exception {
        String baseXPath = getBaseXPathForSubselect(session);
        LOGGER.info("Returning from subselect for " + baseXPath);

        if ("cancel".equals(parameter)) {
            session.setBreakpoint("After canceling subselect for " + baseXPath);
        } else {
            setSubmittedValues(job, session, baseXPath);
            session.setBreakpoint("After returning from subselect for " + baseXPath);
        }

        job.getResponse().sendRedirect(session.getRedirectURL());
    }

    private String getBaseXPathForSubselect(MCREditorSession session) throws JaxenException, JDOMException {
        Document doc = session.getEditedXML();
        MCRChangeData change = session.getChangeTracker().findLastChange(doc);
        String text = change.getText();
        String xPath = text.substring(text.lastIndexOf(" ") + 1).trim();
        return bindsFirstOrMoreThanOneElement(xPath, session) ? xPath + "[1]" : xPath;
    }

    private boolean bindsFirstOrMoreThanOneElement(String xPath, MCREditorSession session) throws JaxenException, JDOMException {
        MCRBinding binding = new MCRBinding(xPath, false, session.getRootBinding());
        boolean result = (binding.getBoundNode() instanceof Element) && !xPath.endsWith("]");
        binding.detach();
        return result;
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
