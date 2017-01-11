/*
 * $Revision$ 
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.xeditor;

import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.target.MCREditorTarget;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXEditorServlet extends MCRServlet {

    protected final static Logger LOGGER = LogManager.getLogger(MCRXEditorServlet.class);

    @Override
    public void doGetPost(MCRServletJob job) throws Exception {
        String xEditorStepID = job.getRequest().getParameter(MCREditorSessionStore.XEDITOR_SESSION_PARAM);

        String sessionID = xEditorStepID.split("-")[0];
        MCREditorSession session = MCREditorSessionStoreFactory.getSessionStore().getSession(sessionID);

        if (session == null) {
            String msg = getErrorI18N("xeditor.error", "noSession", sessionID);
            job.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, msg);
            return;
        }

        int stepNr = Integer.parseInt(xEditorStepID.split("-")[1]);
        session.getChangeTracker().undoChanges(session.getEditedXML(), stepNr);

        sendToTarget(job, session);
    }

    private final static String TARGET_PATTERN = "_xed_submit_";

    private void sendToTarget(MCRServletJob job, MCREditorSession session) throws Exception {
        String targetID = "debug";
        String parameter = "";

        for (Enumeration<String> parameters = job.getRequest().getParameterNames(); parameters.hasMoreElements();) {
            String name = parameters.nextElement();
            if (name.startsWith(TARGET_PATTERN)) {
                if (name.endsWith(".x") || name.endsWith(".y")) // input type="image"
                    name = name.substring(0, name.length() - 2);

                targetID = name.split("[_\\:]")[3].toLowerCase(Locale.ROOT);
                parameter = name.substring(TARGET_PATTERN.length() + targetID.length());
                if (!parameter.isEmpty())
                    parameter = parameter.substring(1);

                break;
            }
        }
        LOGGER.info("sending submission to target " + targetID + " " + parameter);
        getTarget(targetID).handleSubmission(getServletContext(), job, session, parameter);
    }

    private MCREditorTarget getTarget(String targetID) {
        String property = "MCR.XEditor.Target." + targetID + ".Class";
        return MCRConfiguration.instance().getInstanceOf(property);
    }
}
