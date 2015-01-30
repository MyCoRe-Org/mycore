package org.mycore.frontend.xeditor.target;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCREditorSession;

public class MCRAjaxSubselectTarget implements MCREditorTarget {

    @Override
    public void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session, String parameter) throws Exception {
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
