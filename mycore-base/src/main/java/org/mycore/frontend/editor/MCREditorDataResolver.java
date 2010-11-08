/*
 * 
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.xpath.XPath;
import org.mycore.common.xml.MCRURIResolver.MCRResolver;

/**
 * Returns input data entered in any recently used editor form, 
 * given a valid editor session ID.
 * 
 * @author Frank L\u00fctzenkirchen
 */
public class MCREditorDataResolver implements MCRResolver {
    protected final static Logger LOGGER = Logger.getLogger(MCREditorDataResolver.class);

    /**
     * Returns the current editor input from the form with the given editor session ID.
     * 
     * @param uri Syntax: editorData:[sessionID]:[xPath]
     * 
     * @see org.mycore.common.xml.MCRURIResolver.MCRResolver#resolveElement(java.lang.String)
     */
    public Element resolveElement(String uri) throws Exception {
        String[] tokens = uri.split(":",2);
        String sessionID = tokens[1];
        String xPath = tokens[2];
        xPath = fixAttributeConditionsInXPath(xPath);

        LOGGER.debug("MCREditorDataResolver editor session = " + sessionID);
        LOGGER.debug("MCREditorDataResolver xPath = " + xPath);

        Element editor = MCREditorSessionCache.instance().getEditorSession(sessionID).getXML();
        Document xml = new MCREditorSubmission(editor).getXML();
        Element resolved = (Element) (XPath.selectSingleNode(xml, xPath));
        return (resolved == null ? new Element("nothingFound") : resolved);
    }

    private String fixAttributeConditionsInXPath(String xPath) {
        String pattern = "__([a-zA-Z0-9]+)__([a-zA-Z0-9]+)";
        StringBuffer sb = new StringBuffer();
        Matcher m = Pattern.compile(pattern).matcher(xPath);
        while (m.find())
            m.appendReplacement(sb, "[@" + m.group(1) + "='" + m.group(2) + "']");
        m.appendTail(sb);
        xPath = sb.toString();
        return xPath;
    }
}
