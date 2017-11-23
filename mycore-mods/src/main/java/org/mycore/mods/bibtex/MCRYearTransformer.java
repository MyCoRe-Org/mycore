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

package org.mycore.mods.bibtex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Element;

import bibtex.dom.BibtexAbstractValue;
import bibtex.dom.BibtexString;

/**
 * Transforms a BibTeX year field to a JDOM mods:dateIssued element.
 *
 * @author Frank L\u00FCtzenkirchen
 */
class MCRYearTransformer extends MCRField2XPathTransformer {

    private static final Pattern YEAR_PATTERN = Pattern.compile(".*(\\d{4}).*");

    MCRYearTransformer() {
        super("year", "mods:originInfo/mods:dateIssued[@encoding='w3cdtf']");
    }

    void buildField(BibtexAbstractValue value, Element parent) {
        String content = ((BibtexString) value).getContent();
        content = normalizeValue(content);
        String year = getFourDigitYear(content);
        if (year != null) {
            buildElement(xPath, year, parent);
        } else {
            MCRMessageLogger.logMessage("Field year: No 4-digit year found: " + content, parent);
        }
    }

    private String getFourDigitYear(String text) {
        Matcher m = YEAR_PATTERN.matcher(text);
        return m.matches() ? m.group(1) : null;
    }
}
