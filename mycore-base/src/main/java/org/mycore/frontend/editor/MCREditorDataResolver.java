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

import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.transform.JDOMSource;
import org.jdom2.xpath.XPath;

/**
 * Returns input data entered in any recently used editor form, given a valid editor session ID.
 * 
 * @author Frank L\u00fctzenkirchen
 */
public class MCREditorDataResolver implements URIResolver {
    protected final static Logger LOGGER = Logger.getLogger(MCREditorDataResolver.class);

    /**
     * Returns the current editor input from the form with the given editor session ID.
     * 
     * @param href
     *            Syntax: editorData:[sessionID]:[xPath]
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String[] tokens = href.split(":", 3);
        String sessionID = tokens[1];
        String xPath = tokens[2];
        xPath = fixAttributeConditionsInXPath(xPath);

        LOGGER.debug("MCREditorDataResolver editor session = " + sessionID);
        LOGGER.debug("MCREditorDataResolver xPath = " + xPath);

        Element editor = MCREditorSessionCache.instance().getEditorSession(sessionID).getXML();
        MCREditorSubmission sub = new MCREditorSubmission(editor);
        Document xml = sub.getXML();
        try {
            XPath xp = XPath.newInstance(xPath);
            for (Entry<String, Namespace> entry : sub.getNamespaceMap().entrySet())
                xp.addNamespace(entry.getValue());

            Element resolved = (Element) (xp.selectSingleNode(xml));
            return new JDOMSource(resolved == null ? new Element("nothingFound") : resolved);
        } catch (JDOMException e) {
            throw new TransformerException("Could not get XPath instance for: " + xPath, e);
        }
    }

    private String fixAttributeConditionsInXPath(String xPath) {
        String pattern = "__([a-zA-Z0-9]+)__([a-zA-Z0-9_-]+)";
        StringBuffer sb = new StringBuffer();
        Matcher m = Pattern.compile(pattern).matcher(xPath);
        while (m.find()) {
            String attributeName = m.group(1);
            String attributeValue = m.group(2).replace(MCREditorSubmission.BLANK_ESCAPED, MCREditorSubmission.BLANK);
            String condition = "[@" + attributeName + "='" + attributeValue + "']";
            m.appendReplacement(sb, condition);
        }
        m.appendTail(sb);
        xPath = sb.toString();
        return xPath;
    }

}
