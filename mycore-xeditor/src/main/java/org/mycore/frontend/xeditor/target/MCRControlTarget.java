package org.mycore.frontend.xeditor.target;

import java.io.IOException;

import javax.servlet.ServletContext;

import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.MCREditorSessionStore;
import org.mycore.frontend.xeditor.MCRRepeat;

public abstract class MCRControlTarget extends MCREditorTargetBase {

    public void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session, String parameter) throws Exception {
        setSubmittedValues(job, session);
        session.forgetDisplayedFields();
        handleControlParameter(session, parameter);
        redirectToEditorPage(job, session);
    }

    protected void handleControlParameter(MCREditorSession session, String parameter) throws Exception {
        String[] tokens = parameter.split("_");
        String baseXPath = tokens[0];
        String repeatXPath = MCRRepeat.decode(tokens[1]);
        String pos = tokens[2];

        MCRBinding rootBinding = new MCRBinding(session.getEditedXML());
        MCRBinding baseBinding = new MCRBinding(baseXPath, rootBinding);

        handleControl(baseBinding, repeatXPath, pos);
    }

    protected abstract void handleControl(MCRBinding baseBinding, String repeatXPath, String pos) throws Exception;

    protected void redirectToEditorPage(MCRServletJob job, MCREditorSession session) throws IOException {
        String url = job.getRequest().getHeader("referer");
        int index = url.indexOf("?");
        if (index > 0)
            url = url.substring(0, index);
        url += "?" + MCREditorSessionStore.XEDITOR_SESSION_PARAM + "=" + session.getID();
        job.getResponse().sendRedirect(url);
    }
}
