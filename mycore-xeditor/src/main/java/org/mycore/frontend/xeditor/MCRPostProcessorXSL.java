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
import java.util.Optional;

import javax.xml.transform.TransformerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.common.content.transformer.MCRXSL2XMLTransformer;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.common.xsl.MCRXSLResourceHelper;

/**
 * PostProcessor for MyCoRe editor framework
 * that allows execution of XSLT stylesheets after an editor is closed
 * <p>
 * &lt;xed:post-processor class="org.mycore.frontend.xeditor.MCRPostProcessorXSL"
 *      xsl="editor/ir_xeditor2mods.xsl" transformer="saxon" /&gt;
 * <p>
 * You can specify with param xsl the stylesheet, which should be processed and
 * you can specify with parm transformer the XSLStylesheetProcessor ('xalan' or 'saxon').
 * If no transformer is specified the default transformer will be used
 * (property: MCR.LayoutService.TransformerFactoryClass).
 * <p>
 * The params xsl and transformer are deprecated. Use a configured content transformer
 * (MCR.ContentTransformer.{id}) instead:
 * <p>
 * &lt;xed:post-processor contentTransformer="xeditor2mods" /&gt;
 * <p>
 * If contentTransformer is specified, the params xsl and transformer are ignored.
 */
public class MCRPostProcessorXSL implements MCRXEditorPostProcessor {

    private static final Logger LOGGER = LogManager.getLogger();

    private String transformer;

    private String stylesheet;

    private String contentTransformer;

    @Override
    public Document process(Document xml) throws IOException, JDOMException {
        if (contentTransformer != null) {
            return transformWithContentTransformer(xml);
        }
        if (stylesheet == null) {
            return xml.clone();
        }
        LOGGER.warn("xed:post-processor attributes 'xsl' and 'transformer' are deprecated, configure a content"
            + " transformer for {} and use attribute 'contentTransformer'", stylesheet);

        Optional<Class<? extends TransformerFactory>> oFactoryClass =
            switch (transformer) {
                case "xalan" -> MCRConfiguration2.<TransformerFactory>getClass("SLOWXALAN");
                case "saxon" -> MCRConfiguration2.<TransformerFactory>getClass("SAXON");
                case null, default -> Optional.empty();
            };

        Class<? extends TransformerFactory> factoryClass = oFactoryClass.orElse(null);

        final String xslFolder = MCRXSLResourceHelper.getXSLFolder();
        MCRContent source = new MCRJDOMContent(xml);
        MCRXSL2XMLTransformer transformer =
            factoryClass == null ? MCRXSL2XMLTransformer.obtainInstance(xslFolder + "/" + stylesheet)
                : MCRXSL2XMLTransformer.obtainInstance(factoryClass, xslFolder + "/" + stylesheet);
        MCRContent transformed = transformer.transform(source);
        MCRContent normalized = new MCRNormalizeUnicodeTransformer().transform(transformed);
        return normalized.asXML();
    }

    private Document transformWithContentTransformer(Document xml) throws IOException, JDOMException {
        MCRContentTransformer configuredTransformer = MCRContentTransformerFactory.getTransformer(contentTransformer);
        if (configuredTransformer == null) {
            throw new MCRConfigurationException("Content transformer " + contentTransformer
                + " is not configured, set MCR.ContentTransformer." + contentTransformer + ".Stylesheet or .Class");
        }
        MCRContent transformed = configuredTransformer.transform(new MCRJDOMContent(xml));
        MCRContent normalized = new MCRNormalizeUnicodeTransformer().transform(transformed);
        return normalized.asXML();
    }

    @Override
    public void setAttributes(Map<String, String> attributeMap) {
        this.stylesheet = attributeMap.get("xsl");
        this.transformer = attributeMap.get("transformer");
        this.contentTransformer = attributeMap.get("contentTransformer");
    }

    /**
     * @deprecated use {@link #setContentTransformer(String)} with a configured content transformer;
     *             will be removed in a future release
     */
    @Deprecated
    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
    }

    public void setContentTransformer(String contentTransformer) {
        this.contentTransformer = contentTransformer;
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
