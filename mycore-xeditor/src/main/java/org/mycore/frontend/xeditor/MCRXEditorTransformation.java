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
import java.util.Map;

import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRXSL2XMLTransformer;
import org.mycore.common.xsl.MCRParameterCollector;

public class MCRXEditorTransformation {

    public static MCRContent transform(MCRContent xEditor, String xEditorSessionID, Map<String, String[]> requestParameters) throws IOException {
        MCREditorSession editorSession;
        if (xEditorSessionID == null) {
            editorSession = new MCREditorSession(requestParameters);
            MCREditorSessionStore.storeInSession(editorSession);
            xEditorSessionID = editorSession.getID();
        } else
            editorSession = MCREditorSessionStore.getFromSession(xEditorSessionID);

        MCRXSL2XMLTransformer transformer = MCRXSL2XMLTransformer.getInstance("xsl/xeditor.xsl");
        MCRParameterCollector xslParameters = new MCRParameterCollector();
        xslParameters.setParameter("XEditorSessionID", xEditorSessionID);
        return transformer.transform(xEditor, xslParameters);
    }
}
