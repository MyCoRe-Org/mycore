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

import org.jdom2.Element;
import org.jdom2.Namespace;

/**
 * Helper class to map xml:lang and lang attributes in XML to MCRLanguage 
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRLanguageXML {

    /**
     * Sets the lang attribute to the ISO 639-2 bibliographic code of the given language 
     */
    public static void setLangAttribute(MCRLanguage lang, Element element) {
        String code = lang.getCode(MCRLanguageCodeType.biblCode);
        if (code != null)
            element.setAttribute("lang", code);
    }

    /**
     * Sets the xml:lang attribute to the ISO 639-1 code of the given language 
     */
    public static void setXMLLangAttribute(MCRLanguage lang, Element element) {
        element.setAttribute("lang", lang.getCode(MCRLanguageCodeType.xmlCode), Namespace.XML_NAMESPACE);
    }

    /**
     * Returns the language of the given XML element, by inspecting the xml:lang or lang attribute.
     * If neither exists, the default language is returned. 
     */
    public static MCRLanguage getLanguage(Element element) {
        String code = element.getAttributeValue("lang", Namespace.XML_NAMESPACE);
        if ((code == null) || code.isEmpty())
            code = element.getAttributeValue("lang");

        if ((code == null) || code.isEmpty())
            return MCRLanguageFactory.instance().getDefaultLanguage();
        else
            return MCRLanguageFactory.instance().getLanguage(code);

    }
}
