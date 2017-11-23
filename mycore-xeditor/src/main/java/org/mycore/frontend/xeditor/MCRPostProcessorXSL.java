/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.frontend.xeditor;

import java.io.IOException;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRXSL2XMLTransformer;
import org.mycore.common.xml.MCRXMLFunctions;
import org.xml.sax.SAXException;

public class MCRPostProcessorXSL implements MCRXEditorPostProcessor {

    private String stylesheet;

    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
    }

    public Document process(Document xml) throws IOException, JDOMException, SAXException {
        if (stylesheet == null)
            return xml.clone();

        MCRContent source = new MCRJDOMContent(xml);
        MCRContent transformed = MCRXSL2XMLTransformer.getInstance("xsl/" + stylesheet).transform(source);
        MCRContent normalized = new MCRNormalizeUnicodeTransformer().transform(transformed);
        return normalized.asXML();
    }

    @Override
    public void setAttributes(Map<String, String> attributeMap) {
        this.stylesheet = attributeMap.get("xsl");
    }
}

class MCRNormalizeUnicodeTransformer extends MCRContentTransformer {

    public MCRJDOMContent transform(MCRContent source) throws IOException {
        try {
            Element root = source.asXML().getRootElement().clone();
            for (Text text : root.getDescendants(Filters.text())) {
                text.setText(MCRXMLFunctions.normalizeUnicode(text.getText()));
            }
            return new MCRJDOMContent(root);
        } catch (JDOMException | SAXException ex) {
            throw new IOException(ex);
        }
    }
}
