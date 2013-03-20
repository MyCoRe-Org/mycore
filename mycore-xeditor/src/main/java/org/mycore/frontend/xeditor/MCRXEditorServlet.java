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

import java.io.IOException;
import java.text.ParseException;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXEditorServlet extends MCRServlet {

    protected final static Logger LOGGER = Logger.getLogger(MCRXEditorServlet.class);

    @Override
    public void doGetPost(MCRServletJob job) throws IOException, JDOMException, ParseException {
        String xEditorSessionID = job.getRequest().getParameter("XEditorSessionID");
        MCREditorSession session = MCREditorSessionStoreFactory.getSessionStore().getSession(xEditorSessionID);

        for (String xPath : (Set<String>) (job.getRequest().getParameterMap().keySet())) {
            if (xPath.startsWith("/")) {
                String[] values = job.getRequest().getParameterValues(xPath);
                session.setSubmittedValues(xPath, values);
            }
        }
        session.removeDeletedNodes();

        MCRContent editedXML = new MCRJDOMContent(session.getEditedXML());
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), editedXML);
    }
}
