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

package org.mycore.datamodel.language;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.tools.MCRLanguageOrientationHelper;

/**
 * {@link URIResolver} that returns language metadata for a given ISO language code as XML.
 */
public class MCRLanguageURIResolver implements URIResolver {

    private static final Map<MCRLanguageCodeType, String> XML_ATTRIBUTE_CODE_NAMES;

    static {
        XML_ATTRIBUTE_CODE_NAMES = new ConcurrentHashMap<>();
        XML_ATTRIBUTE_CODE_NAMES.put(MCRLanguageCodeType.XML_CODE, "xmlCode");
        XML_ATTRIBUTE_CODE_NAMES.put(MCRLanguageCodeType.BIBL_CODE, "biblCode");
        XML_ATTRIBUTE_CODE_NAMES.put(MCRLanguageCodeType.TERM_CODE, "termCode");
    }

    /**
     * Resolves the given ISO language code and returns its metadata as an XML source.
     * <p>The result includes all known code representations, translated labels in available
     * languages, and a right-to-left indicator.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{ISOCode}
     * </pre>
     * <p>Example request:
     * <pre>
     *   language:de
     *   language:eng
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <language xmlCode="de" biblCode="ger" termCode="deu" rtl="false">
     *     <label xml:lang="de">Deutsch</label>
     *     <label xml:lang="en">German</label>
     *   </language>
     * }</pre>
     *
     * @param href the URI to resolve; the part after the first {@code :} is the ISO language code
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping the {@code <language>} element
     * @throws TransformerException if the language code is unknown or an undefined code type is encountered
     * @throws IllegalArgumentException if no language code is provided
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException, IllegalArgumentException {
        String[] hrefContent = href.split(":");
        if (hrefContent.length < 2) {
            throw new IllegalArgumentException("Empty language code found while resolving URI 'language:'.");
        }
        try {
            String code = hrefContent[1];
            MCRLanguage language = MCRLanguageFactory.obtainInstance().getLanguage(code);
            Document doc = new Document(buildXML(language));
            return new JDOMSource(doc);
        } catch (TransformerException transformerException) {
            throw transformerException;
        } catch (Exception ex) {
            throw new TransformerException(ex);
        }
    }

    private Element buildXML(MCRLanguage language) throws TransformerException {
        Element xml = new Element("language");

        for (Entry<MCRLanguageCodeType, String> entry : language.getCodes().entrySet()) {
            String attributeName = XML_ATTRIBUTE_CODE_NAMES.get(entry.getKey());
            if (attributeName == null) {
                throw new TransformerException(
                    "Undefined language code '" + entry.getKey() + "' for language '" + language + "'.");
            }
            xml.setAttribute(attributeName, entry.getValue());
        }

        for (Entry<MCRLanguage, String> entry : language.getLabels().entrySet()) {
            Element label = new Element("label");
            label.setText(entry.getValue());
            MCRLanguageXML.setXMLLangAttribute(entry.getKey(), label);
            xml.addContent(label);
        }

        xml.setAttribute("rtl",
            MCRLanguageOrientationHelper.isRTL(language.getCode(MCRLanguageCodeType.XML_CODE)) ? "true" : "false");

        return xml;
    }

}
