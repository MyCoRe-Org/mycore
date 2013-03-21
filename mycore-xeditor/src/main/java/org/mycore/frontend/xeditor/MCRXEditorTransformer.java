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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.common.content.transformer.MCRXSL2XMLTransformer;
import org.mycore.common.xsl.MCRParameterCollector;
import org.xml.sax.SAXException;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXEditorTransformer {

    private final static Logger LOGGER = Logger.getLogger(MCRXEditorTransformer.class);

    private MCRParameterCollector parameters;

    private MCREditorSession editorSession;

    private MCRBinding currentBinding;

    public MCRXEditorTransformer(MCRParameterCollector parameters, MCREditorSession editorSession) {
        this.parameters = parameters;
        this.editorSession = editorSession;
    }

    public MCRContent transform(MCRContent editorSource) throws IOException {
        MCRXSL2XMLTransformer transformer = MCRXSL2XMLTransformer.getInstance("xsl/xeditor.xsl");
        String key = MCRXEditorTransformerStore.storeTransformer(this);
        parameters.setParameter("XEditorTransformerKey", key);
        return transformer.transform(editorSource, parameters);
    }

    public static MCRXEditorTransformer getTransformer(String key) {
        return MCRXEditorTransformerStore.getAndRemoveTransformer(key);
    }

    public String getEditorSessionID() {
        return editorSession.getID();
    }

    public void readSourceXML(String uri) throws JDOMException, IOException, SAXException, TransformerException {
        uri = replaceParameters(uri);
        if ((!uri.contains("{")) && (editorSession.getEditedXML() == null)) {
            LOGGER.info(editorSession.getID() + " reading edited XML from " + uri);
            Document xml = MCRSourceContent.getInstance(uri).asXML();
            editorSession.setEditedXML(xml);
            currentBinding = new MCRBinding(xml);
        }
    }

    public void bind(String xPath, String name) throws JDOMException, ParseException {
        currentBinding = new MCRBinding(xPath, name, currentBinding);
    }

    public void unbind() {
        currentBinding = currentBinding.getParent();
    }

    public String getAbsoluteXPath() {
        return currentBinding.getAbsoluteXPath();
    }

    public String getValue() {
        markAsUsed();
        return currentBinding.getValue();
    }

    public boolean hasValue(String value) {
        markAsUsed();
        return currentBinding.hasValue(value);
    }

    private void markAsUsed() {
        for (Object node : currentBinding.getBoundNodes()) {
            editorSession.markAsTransformedToInputField(node);
        }
    }

    public String bindingName() {
        return currentBinding.getName();
    }

    public String bindingXPath() {
        return currentBinding.getRelativeXPath();
    }

    public String numRepeats(int minRepeats) throws JDOMException {
        int numBoundNodes = currentBinding.getBoundNodes().size();
        int numRepeats = Math.max(numBoundNodes, Math.max(minRepeats, 1));
        return StringUtils.repeat("a ", numRepeats);
    }

    public String replaceParameters(String uri) {
        Matcher m = Pattern.compile("\\{(.+)\\}").matcher(uri);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String token = m.group(1);
            String value = parameters.getParameter(token, null);
            m.appendReplacement(sb, value == null ? m.group() : value);
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
