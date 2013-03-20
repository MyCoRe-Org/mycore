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
import java.net.MalformedURLException;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.frontend.servlets.MCRStaticXMLFileServlet;
import org.xml.sax.SAXException;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRStaticXEditorFileServlet extends MCRStaticXMLFileServlet {

    protected final static Logger LOGGER = Logger.getLogger(MCRStaticXEditorFileServlet.class);

    @Override
    protected MCRContent expandEditorElements(HttpServletRequest request, File file) throws IOException, JDOMException, SAXException,
            MalformedURLException {
        MCRContent content = super.expandEditorElements(request, file);
        if (mayContainEditorForm(content)) {
            String editorSessionID = request.getParameter("XEditorSessionID");
            MCREditorSession editorSession;
            if (editorSessionID != null)
                editorSession = MCREditorSessionStoreFactory.getSessionStore().getSession(editorSessionID);
            else {
                editorSession = new MCREditorSession();
                editorSessionID = MCREditorSessionStoreFactory.getSessionStore().storeSession(editorSession);
                editorSession.setID(editorSessionID);
            }

            MCRParameterCollector requestParameters = new MCRParameterCollector(request);
            content = new MCRXEditorTransformer(requestParameters, editorSession).transform(content);
        }
        return content;
    }
}
