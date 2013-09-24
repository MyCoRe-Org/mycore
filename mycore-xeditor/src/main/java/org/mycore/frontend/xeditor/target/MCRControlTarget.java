package org.mycore.frontend.xeditor.target;

import javax.servlet.ServletContext;

import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.MCREncoder;
import org.mycore.frontend.xeditor.MCRRepeatBinding;

public abstract class MCRControlTarget extends MCREditorTarget {

    public void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session, String parameter) throws Exception {
        session.getSubmission().setSubmittedValues(job.getRequest().getParameterMap());
        session.getValidator().forgetInvalidFields();
        handleControlParameter(session, parameter);
        redirectToEditorPage(job, session);
    }

    protected void handleControlParameter(MCREditorSession session, String parameter) throws Exception {
        String[] tokens = parameter.split("_");
        String baseXPath = tokens[0];
        String repeatXPath = MCREncoder.decode(tokens[1]);
        int pos = Integer.parseInt(tokens[2]);

        MCRBinding baseBinding = new MCRBinding(baseXPath, session.getRootBinding());
        MCRRepeatBinding repeatBinding = new MCRRepeatBinding(repeatXPath, baseBinding);
        handleControl(repeatBinding, pos);
        repeatBinding.detach();
        baseBinding.detach();
    }

    protected abstract void handleControl(MCRRepeatBinding repeatBinding, int pos) throws Exception;
}
