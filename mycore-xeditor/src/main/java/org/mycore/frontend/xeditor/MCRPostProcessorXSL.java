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

import javax.xml.transform.TransformerFactory;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.mycore.common.MCRClassTools;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRXSL2XMLTransformer;
import org.mycore.common.xml.MCRXMLFunctions;
import org.xml.sax.SAXException;

/**
 * PostProcessor for MyCoRe editor framework
 * that allows execution of XSLT stylesheets after an editor is closed
 * 
 * <xed:post-processor class="org.mycore.frontend.xeditor.MCRPostProcessorXSL" 
 *      xsl="editor/ir_xeditor2mods.xsl" transformer="saxon" />
 * 
 * You can specify with param xsl the stylesheet, which should be processed and 
 * you can specify with parm transformer the XSLStylesheetProcessor ('xalan' or 'saxon').
 * If no transformer is specified the default transformer will be used
 * (property: MCR.LayoutService.TransformerFactoryClass).
 */
public class MCRPostProcessorXSL implements MCRXEditorPostProcessor {

    private Class<? extends TransformerFactory> factoryClass;

    private String transformer;

    private String stylesheet;

    public Document process(Document xml) throws IOException, JDOMException, SAXException {
        if (stylesheet == null) {
            return xml.clone();
        }

        try {
            if ("xalan".equals(transformer)) {
                this.factoryClass = MCRClassTools
                    .forName("org.apache.xalan.processor.TransformerFactoryImpl");
            }
            if ("saxon".equals(transformer)) {
                this.factoryClass = MCRClassTools
                    .forName("net.sf.saxon.TransformerFactoryImpl");
            }
        } catch (ClassNotFoundException e) {
            //do nothing, use default
        }

        if (this.factoryClass == null) {
            this.factoryClass = MCRConfiguration2
                .<TransformerFactory>getClass("MCR.LayoutService.TransformerFactoryClass").orElseThrow();
        }

        MCRContent source = new MCRJDOMContent(xml);
        MCRContent transformed = MCRXSL2XMLTransformer.getInstance(factoryClass, "xsl/" + stylesheet).transform(source);
        MCRContent normalized = new MCRNormalizeUnicodeTransformer().transform(transformed);
        return normalized.asXML();
    }

    @Override
    public void setAttributes(Map<String, String> attributeMap) {
        this.stylesheet = attributeMap.get("xsl");
        if (attributeMap.containsKey("transformer")) {
            this.transformer = attributeMap.get("transformer");
        }
    }

    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
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
