package org.mycore.frontend.xeditor.target;

import javax.servlet.ServletContext;

import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.MCRRepeatBinding;

public abstract class MCRControlTarget extends MCREditorTarget {

    public void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session, String parameter) throws Exception {
        setSubmittedValues(job, session.getCurrentStep());
        session.getCurrentStep().forgetDisplayedFields();
        session.getValidator().forgetInvalidFields();
        handleControlParameter(session, parameter);
        redirectToEditorPage(job, session);
    }

    protected void handleControlParameter(MCREditorSession session, String parameter) throws Exception {
        String[] tokens = parameter.split("_");
        String baseXPath = tokens[0];
        String repeatXPath = MCRRepeatBinding.decode(tokens[1]);
        int pos = Integer.parseInt(tokens[2]);

        MCRBinding baseBinding = session.getCurrentStep().bind(baseXPath);
        MCRRepeatBinding repeatBinding = new MCRRepeatBinding(repeatXPath, baseBinding);

        handleControl(repeatBinding, pos);
    }

    protected abstract void handleControl(MCRRepeatBinding repeatBinding, int pos) throws Exception;
}
