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

package org.mycore.frontend.xeditor.transformer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRStringContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.services.i18n.MCRTranslation;

/**
 * Helps transforming xed:output elements. 
 * 
 * @author Frank Lützenkirchen
 */
public class MCROutputTransformerHelper extends MCRTransformerHelperBase {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ATTR_VALUE = "value";
    private static final String ATTR_I18N = "i18n";
    private static final String ATTR_ESCAPE_XML = "escape-xml";

    @Override
    List<String> getSupportedMethods() {
        return Arrays.asList("output");
    }

    @Override
    void handle(MCRTransformerHelperCall call) {
        String value = call.getAttributeValueOrDefault(ATTR_VALUE, null);
        String i18n = call.getAttributeValueOrDefault(ATTR_I18N, null);
        boolean escapeXML = "true".equalsIgnoreCase(call.getAttributeValueOrDefault(ATTR_ESCAPE_XML, "true"));
        String output = output(value, i18n);
        if (escapeXML) {
            call.getReturnElement().setText(output);
        } else {
            try {
                Element eText = MCRXMLParserFactory.getNonValidatingParser()
                    .parseXML(new MCRStringContent("<text>" + output + "</text>"))
                    .getRootElement();
                call.getReturnElement().setContent(eText.cloneContent());
            } catch (JDOMException | IOException e) {
                LOGGER.warn(
                    "Could not transform value='{}', i18n='{}'to XML (text representation will be used instead): {}",
                    value, i18n, output);
                call.getReturnElement().setText(output);
            }
        }
    }

    private String output(String attrValue, String attrI18N) {
        if (!StringUtils.isEmpty(attrI18N)) {
            String key = replaceXPaths(attrI18N);

            if (StringUtils.isEmpty(attrValue)) {
                return MCRTranslation.translate(key);
            } else {
                String value = transformationState.getXPathEvaluator().evaluateFirstAsString(attrValue);
                return MCRTranslation.translate(key, value);
            }

        } else if (!StringUtils.isEmpty(attrValue)) {
            return transformationState.getXPathEvaluator().replaceXPathOrI18n(attrValue);
        } else {
            return getCurrentBinding().getValue();
        }
    }
}
