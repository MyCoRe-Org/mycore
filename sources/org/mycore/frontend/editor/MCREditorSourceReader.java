/*
 * $RCSfile$
 * $Revision$ $Date$
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

package org.mycore.frontend.editor;

import java.util.Map;

import org.jdom.Element;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.frontend.servlets.MCRServlet;

/**
 * This class contains the functionality to read XML input that should be editor
 * in the editor.
 * 
 * @author Frank L�tzenkirchen
 */
public class MCREditorSourceReader {
    
    private final static int MAX_URI_PROTOCOL_LENGTH=15;
    
    /**
     * Reads XML input from an url and builds a list of source variable elements
     * that can be processed by editor.xsl
     */
    static MCREditorSubmission readSource(Element editor, Map parameters) {
        Element source = editor.getChild("source");
        String[] urlFromRequest = (String[]) (parameters.get("XSL.editor.source.url"));
        String[] tokenFromRequest = (String[]) (parameters.get("XSL.editor.source.id"));
        String[] newFromRequest = (String[]) (parameters.get("XSL.editor.source.new"));

        String url = null;

        if ((newFromRequest != null) && (newFromRequest[0].equals("true"))) {
            url = null;
        } else if ((urlFromRequest != null) && (urlFromRequest[0].trim().length() > 0)) {
            url = urlFromRequest[0].trim();
        } else if ((tokenFromRequest != null) && (tokenFromRequest[0].trim().length() > 0) && (source != null)) {
            String urlFromDef = source.getAttributeValue("url");
            String tokenFromDef = source.getAttributeValue("token");

            if ((urlFromDef != null) && (tokenFromDef != null)) {
                int pos = urlFromDef.indexOf(tokenFromDef);

                if (pos != -1) {
                    String before = urlFromDef.substring(0, pos);
                    String after = urlFromDef.substring(pos + tokenFromDef.length());
                    url = before + tokenFromRequest[0].trim() + after;
                }
            }
        } else if ((source != null) && (source.getAttributeValue("url", "").trim().length() > 0)) {
            //start new session if token attribute is present
            if (source.getAttributeValue("token")==null){
                url = source.getAttributeValue("url");
            } else {
                //as there is no sessionId yet we can not replace token in url
                url = null;
            }
        }

        if ((url == null) || (url.trim().length() == 0)) {
            MCREditorServlet.logger.info("Editor is started empty without XML input");

            return null;
        }
        //:NOTE: adjust MAX_URI_PROTOCOL_LENGTH according to the longest protocol name MCRURIResolver understands
        if (url.substring(0, (url.length() < MAX_URI_PROTOCOL_LENGTH) ? url.length() : MAX_URI_PROTOCOL_LENGTH).indexOf(":") == -1) {
            url = "request:" + url;
        }

        MCREditorServlet.logger.info("Editor reading XML input from " + url);
        Element input = MCRURIResolver.instance().resolve(url);
        input.detach();
        return new MCREditorSubmission(input,editor);
    }
}
