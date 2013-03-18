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

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRFileContent;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

public class MCRXEditorServlet extends MCRServlet {

    protected final static Logger LOGGER = Logger.getLogger(MCRXEditorServlet.class);

    @Override
    public void doGetPost(MCRServletJob job) throws IOException {
        new MCRXEditorServletRequest(job).handleRequest();
    }

    class MCRXEditorServletRequest {

        private HttpServletRequest req;

        private HttpServletResponse res;

        private MCREditorSession editorSession;

        public MCRXEditorServletRequest(MCRServletJob job) {
            this.req = job.getRequest();
            this.res = job.getResponse();
        }

        public void handleRequest() throws IOException {
            getOrBuildEditorSession();

            String path = getPath();
            File file = new File(path);
            if (!file.exists()) {
                String msg = "Could not find file " + path;
                res.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
                return;
            }
            
            MCRContent content = new MCRFileContent(file);
            getLayoutService().doLayout(req, res, content);
        }

        private void getOrBuildEditorSession() {
            String editorID = req.getParameter("XSL.XEditorSessionID");
            if (editorID == null) {
                editorSession = new MCREditorSession(req.getParameterMap());
                MCREditorSessionStore.storeInSession(editorSession);
                req.setAttribute("XSL.XEditorSessionID", editorSession.getID());
            } else {
                editorSession = MCREditorSessionStore.getFromSession(editorID);
            }
        }

        private String getPath() {
            String servletPath = req.getServletPath();
            LOGGER.info(servletPath);
            return getServletContext().getRealPath(servletPath);
        }
    }
}
