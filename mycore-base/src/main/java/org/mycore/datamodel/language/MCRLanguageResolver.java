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

package org.mycore.datamodel.language;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.tools.MCRLanguageOrientationHelper;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import java.util.Map.Entry;

/**
 * Resolves languages by code. Syntax: language:{ISOCode}
 *
 * @author Frank Lützenkirchen
 */
public class MCRLanguageResolver implements URIResolver {

    public Source resolve(String href, String base) throws TransformerException, IllegalArgumentException {
        String[] hrefContent = href.split(":");
        if (hrefContent.length < 2) {
            throw new IllegalArgumentException("Empty language code found while resolving URI 'language:'.");
        }
        try {
            String code = hrefContent[1];
            MCRLanguage language = MCRLanguageFactory.instance().getLanguage(code);
            Document doc = new Document(buildXML(language));
            return new JDOMSource(doc);
        } catch (Exception ex) {
            throw new TransformerException(ex);
        }
    }

    private Element buildXML(MCRLanguage language) {
        Element xml = new Element("language");

        for (Entry<MCRLanguageCodeType, String> entry : language.getCodes().entrySet()) {
            xml.setAttribute(entry.getKey().name(), entry.getValue());
        }

        for (Entry<MCRLanguage, String> entry : language.getLabels().entrySet()) {
            Element label = new Element("label");
            label.setText(entry.getValue());
            MCRLanguageXML.setXMLLangAttribute(entry.getKey(), label);
            xml.addContent(label);
        }

        xml.setAttribute("rtl",
            MCRLanguageOrientationHelper.isRTL(language.getCode(MCRLanguageCodeType.xmlCode)) ? "true" : "false");

        return xml;
    }

}
