/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
import org.mycore.common.xsl.MCRXSLResourceHelper;

/**
 * PostProcessor for MyCoRe editor framework
 * that allows execution of XSLT stylesheets after an editor is closed
 * <p>
 * &lt;xed:post-processor class="org.mycore.frontend.xeditor.MCRPostProcessorXSL"
 *      xsl="editor/ir_xeditor2mods.xsl" /&gt;
 * <p>
 * You can specify with param xsl the stylesheet, which should be processed and
 * you can select a supported processor with the optional transformer parameter.
 * If no transformer is specified the default transformer will be used
 * (property: MCR.LayoutService.TransformerFactory).
 */
public class MCRPostProcessorXSL implements MCRXEditorPostProcessor {

    private String transformer;

    private String stylesheet;

    @Override
    public Document process(Document xml) throws IOException, JDOMException {
        if (stylesheet == null) {
            return xml.clone();
        }

        String factoryId = switch (transformer) {
            case "xalan" -> "SlowXalan";
            case "saxon" -> "Saxon";
            case null, default -> null;
        };

        final String xslFolder = MCRXSLResourceHelper.getXSLFolder();
        MCRContent source = new MCRJDOMContent(xml);
        MCRXSL2XMLTransformer transformer =
            factoryId == null ? MCRXSL2XMLTransformer.obtainInstance(xslFolder + "/" + stylesheet)
                : MCRXSL2XMLTransformer.obtainInstanceByFactory(factoryId, xslFolder + "/" + stylesheet);
        MCRContent transformed = transformer.transform(source);
        MCRContent normalized = new MCRNormalizeUnicodeTransformer().transform(transformed);
        return normalized.asXML();
    }

    @Override
    public void setAttributes(Map<String, String> attributeMap) {
        this.stylesheet = attributeMap.get("xsl");
        this.transformer = attributeMap.get("transformer");
    }

    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
    }
}

class MCRNormalizeUnicodeTransformer extends MCRContentTransformer {

    @Override
    public MCRJDOMContent transform(MCRContent source) throws IOException {
        try {
            Element root = source.asXML().getRootElement().clone();
            for (Text text : root.getDescendants(Filters.text())) {
                text.setText(MCRXMLFunctions.normalizeUnicode(text.getText()));
            }
            return new MCRJDOMContent(root);
        } catch (JDOMException ex) {
            throw new IOException(ex);
        }
    }
}
