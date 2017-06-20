/*
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

package org.mycore.datamodel.language;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a language in the datamodel, independent of the type of code used to encode it.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRLanguage {

    /**
     * A map from codes used for this language, by code type
     */
    private Map<MCRLanguageCodeType, String> codesByType = new HashMap<MCRLanguageCodeType, String>();

    /**
     * A map of labels for this language, by language 
     */
    private Map<MCRLanguage, String> labelsByLanguage = new HashMap<MCRLanguage, String>();

    private Locale locale;

    /**
     * Language instances are created by the package itself, do not use on your own, use MCRLanguageFactory instead.
     * 
     * @see MCRLanguageFactory
     */
    MCRLanguage() {
    }

    /**
     * Sets the language code of the given type
     */
    void setCode(MCRLanguageCodeType type, String code) {
        codesByType.put(type, code);
    }

    /**
     * Returns the code of this language for the given type
     */
    public String getCode(MCRLanguageCodeType type) {
        return codesByType.get(type);
    }

    /**
     * Returns all language codes used for this language
     */
    public Map<MCRLanguageCodeType, String> getCodes() {
        return codesByType;
    }

    /**
     * Sets the label in the given language
     */
    void setLabel(MCRLanguage language, String label) {
        labelsByLanguage.put(language, label);
    }

    /**
     * Returns the label in the given language
     */
    public String getLabel(MCRLanguage language) {
        return labelsByLanguage.get(language);
    }

    /**
     * Returns the label in the given language
     */
    public String getLabel(String languageCode) {
        MCRLanguage language = MCRLanguageFactory.instance().getLanguage(languageCode);
        return labelsByLanguage.get(language);
    }

    /**
     * Returns all labels used for this language
     */
    public Map<MCRLanguage, String> getLabels() {
        return labelsByLanguage;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MCRLanguage)
            return this.toString().equals(obj.toString());
        else
            return false;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return getCode(MCRLanguageCodeType.xmlCode);
    }

    public Locale getLocale() {
        return locale;
    }

    void setLocale(Locale locale) {
        this.locale = locale;
    }
}
