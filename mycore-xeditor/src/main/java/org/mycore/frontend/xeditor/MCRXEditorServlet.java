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
import java.util.Map;

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
        String servletPath = job.getRequest().getServletPath();
        LOGGER.debug(servletPath);
        String path = getServletContext().getRealPath(servletPath);

        File file = new File(path);
        if (!file.exists()) {
            String msg = "Could not find file " + path;
            job.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, msg);
            return;
        }

        MCRContent xEditor = new MCRFileContent(file);
        Map<String, String[]> requestParameters = job.getRequest().getParameterMap();
        String xEditorSessionID = job.getRequest().getParameter("XEditorSessionID");
        MCRContent transformedEditor = MCRXEditorTransformation.transform(xEditor, xEditorSessionID, requestParameters);

        getLayoutService().doLayout(job.getRequest(), job.getResponse(), transformedEditor);
    }
}
